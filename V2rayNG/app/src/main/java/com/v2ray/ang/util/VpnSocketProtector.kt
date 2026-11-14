package com.v2ray.ang.util

import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreServiceManager
import java.io.FileDescriptor
import java.lang.reflect.Field
import java.net.Socket

/**
 * Marks a Java [Socket] so Android VpnService does not capture it (avoids routing loops).
 */
object VpnSocketProtector {
    fun protect(socket: Socket): Boolean {
        return try {
            val fd = fileDescriptor(socket) ?: return true
            val sockFd = descriptorInt(fd)
            if (sockFd < 0) return true
            CoreServiceManager.serviceControl?.get()?.vpnProtect(sockFd) ?: true
        } catch (e: Exception) {
            LogUtil.w(AppConfig.TAG, "VpnSocketProtector: protect failed: ${e.message}")
            false
        }
    }

    private fun fileDescriptor(socket: Socket): FileDescriptor? {
        val implField = accessibleField(socket.javaClass, "impl") ?: return null
        val impl = implField.get(socket) ?: return null
        val fdField = accessibleField(impl.javaClass, "fd") ?: return null
        return fdField.get(impl) as? FileDescriptor
    }

    private fun descriptorInt(fd: FileDescriptor): Int {
        val field = accessibleField(fd.javaClass, "descriptor")
            ?: accessibleField(fd.javaClass, "fd")
            ?: return -1
        return field.getInt(fd)
    }

    private fun accessibleField(clazz: Class<*>, name: String): Field? {
        var c: Class<*>? = clazz
        while (c != null) {
            try {
                return c.getDeclaredField(name).also { it.isAccessible = true }
            } catch (_: NoSuchFieldException) {
                c = c.superclass
            }
        }
        return null
    }
}
