package com.v2ray.ang.service

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.VpnSocketProtector
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.DirectConnection
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.SocketFactory

/**
 * SSH password login → local SOCKS5 on 127.0.0.1.
 * Xray then uses that SOCKS as outbound (Xray has no native ssh://).
 */
object SshTunnelManager {
    @Volatile
    var localSocksPort: Int = 0
        private set

    private var ssh: SSHClient? = null
    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null
    private var relayPool: ExecutorService? = null
    private val running = AtomicBoolean(false)

    @Synchronized
    fun start(profile: ProfileItem) {
        stop()

        val host = profile.server?.trim().orEmpty()
        if (host.isEmpty()) error("SSH host missing")
        val port = profile.serverPort?.toIntOrNull()?.takeIf { it > 0 } ?: 22
        val user = profile.username?.trim().orEmpty()
        if (user.isEmpty()) error("SSH username missing")
        val password = profile.password.orEmpty()

        val client = SSHClient()
        client.addHostKeyVerifier(PromiscuousVerifier())
        client.socketFactory = ProtectedSocketFactory
        client.connectTimeout = 15_000
        client.timeout = 30_000
        client.connect(host, port)
        try {
            client.authPassword(user, password)
        } catch (e: Exception) {
            try {
                client.disconnect()
            } catch (_: Exception) {
            }
            throw IOException("SSH auth failed: ${e.message}", e)
        }
        try {
            client.connection.keepAlive.keepAliveInterval = 30
        } catch (_: Exception) {
        }

        val listen = ServerSocket(0, 50, InetAddress.getByName(AppConfig.LOOPBACK))
        val localPort = listen.localPort
        val pool = Executors.newCachedThreadPool { r ->
            Thread(r, "ssh-socks-relay").also { it.isDaemon = true }
        }

        ssh = client
        serverSocket = listen
        relayPool = pool
        localSocksPort = localPort
        running.set(true)

        acceptThread = Thread({
            while (running.get()) {
                val clientSock = try {
                    listen.accept()
                } catch (_: Exception) {
                    break
                }
                pool.execute {
                    try {
                        handleSocksClient(client, clientSock)
                    } catch (e: Exception) {
                        LogUtil.d(AppConfig.TAG, "SSH SOCKS client ended: ${e.message}")
                        try {
                            clientSock.close()
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }, "ssh-socks-accept").also {
            it.isDaemon = true
            it.start()
        }

        LogUtil.i(AppConfig.TAG, "SSH tunnel up → socks://${AppConfig.LOOPBACK}:$localPort ($host:$port)")
    }

    @Synchronized
    fun stop() {
        running.set(false)
        localSocksPort = 0

        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null

        try {
            acceptThread?.interrupt()
        } catch (_: Exception) {
        }
        acceptThread = null

        try {
            relayPool?.shutdownNow()
        } catch (_: Exception) {
        }
        relayPool = null

        try {
            ssh?.disconnect()
        } catch (_: Exception) {
        }
        try {
            ssh?.close()
        } catch (_: Exception) {
        }
        ssh = null
    }

    private fun handleSocksClient(sshClient: SSHClient, socket: Socket) {
        socket.soTimeout = 0
        val input = socket.getInputStream()
        val output = socket.getOutputStream()

        if (input.read() != 0x05) {
            socket.close()
            return
        }
        val nMethods = input.read()
        if (nMethods <= 0) {
            socket.close()
            return
        }
        repeat(nMethods) { input.read() }
        output.write(byteArrayOf(0x05, 0x00))
        output.flush()

        if (input.read() != 0x05) {
            socket.close()
            return
        }
        val cmd = input.read()
        input.read() // rsv
        val atyp = input.read()
        val destHost: String
        when (atyp) {
            0x01 -> {
                val addr = ByteArray(4)
                readFully(input, addr)
                destHost = InetAddress.getByAddress(addr).hostAddress ?: run {
                    replySocks(output, 0x01)
                    socket.close()
                    return
                }
            }

            0x03 -> {
                val len = input.read()
                if (len <= 0) {
                    replySocks(output, 0x01)
                    socket.close()
                    return
                }
                val name = ByteArray(len)
                readFully(input, name)
                destHost = String(name, Charsets.UTF_8)
            }

            0x04 -> {
                val addr = ByteArray(16)
                readFully(input, addr)
                destHost = InetAddress.getByAddress(addr).hostAddress ?: run {
                    replySocks(output, 0x01)
                    socket.close()
                    return
                }
            }

            else -> {
                replySocks(output, 0x08)
                socket.close()
                return
            }
        }
        val portHi = input.read()
        val portLo = input.read()
        if (portHi < 0 || portLo < 0) {
            socket.close()
            return
        }
        val destPort = (portHi shl 8) or portLo

        if (cmd != 0x01) {
            replySocks(output, 0x07)
            socket.close()
            return
        }

        val channel: DirectConnection = try {
            sshClient.newDirectConnection(destHost, destPort)
        } catch (e: Exception) {
            LogUtil.w(AppConfig.TAG, "SSH direct-tcpip $destHost:$destPort failed: ${e.message}")
            replySocks(output, 0x05)
            socket.close()
            return
        }

        val remoteIn = channel.inputStream
        val remoteOut = channel.outputStream
        replySocks(output, 0x00)

        val closed = AtomicBoolean(false)
        fun pump(from: InputStream, to: OutputStream) {
            val buf = ByteArray(16 * 1024)
            try {
                while (!closed.get()) {
                    val n = from.read(buf)
                    if (n < 0) break
                    to.write(buf, 0, n)
                    to.flush()
                }
            } catch (_: Exception) {
            } finally {
                closed.set(true)
                try {
                    socket.close()
                } catch (_: Exception) {
                }
                try {
                    channel.close()
                } catch (_: Exception) {
                }
            }
        }

        val down = Thread({ pump(remoteIn, output) }, "ssh-socks-down")
        down.isDaemon = true
        down.start()
        pump(input, remoteOut)
        try {
            down.join(2000)
        } catch (_: Exception) {
        }
    }

    private fun replySocks(output: OutputStream, rep: Int) {
        output.write(
            byteArrayOf(
                0x05, rep.toByte(), 0x00, 0x01,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00,
            )
        )
        output.flush()
    }

    private fun readFully(input: InputStream, buf: ByteArray) {
        var off = 0
        while (off < buf.size) {
            val n = input.read(buf, off, buf.size - off)
            if (n < 0) throw IOException("EOF")
            off += n
        }
    }

    private object ProtectedSocketFactory : SocketFactory() {
        override fun createSocket(): Socket = ProtectedSocket()

        override fun createSocket(host: String, port: Int): Socket =
            ProtectedSocket().also { it.connect(InetSocketAddress(host, port)) }

        override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket =
            ProtectedSocket().also {
                it.bind(InetSocketAddress(localHost, localPort))
                it.connect(InetSocketAddress(host, port))
            }

        override fun createSocket(host: InetAddress, port: Int): Socket =
            ProtectedSocket().also { it.connect(InetSocketAddress(host, port)) }

        override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket =
            ProtectedSocket().also {
                it.bind(InetSocketAddress(localAddress, localPort))
                it.connect(InetSocketAddress(address, port))
            }
    }

    private class ProtectedSocket : Socket() {
        override fun connect(endpoint: SocketAddress?, timeout: Int) {
            if (!isBound) {
                bind(InetSocketAddress(0))
            }
            VpnSocketProtector.protect(this)
            super.connect(endpoint, timeout)
        }
    }
}
