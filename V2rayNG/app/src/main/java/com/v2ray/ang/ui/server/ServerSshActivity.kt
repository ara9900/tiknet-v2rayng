package com.v2ray.ang.ui.server

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.v2ray.ang.R
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.ui.compose.FormTextField

/** Host / user / password editor for ssh:// profiles (tunnel, not shell). */
class ServerSshActivity : BaseServerActivity() {

    override val serverConfigType: EConfigType = EConfigType.SSH

    @Composable
    override fun ScreenContent() {
        val uiState = rememberSaveable(saver = ServerUiState.Saver) {
            ServerUiState.from(initialConfig = initialConfig)
        }.apply {
            configType = serverConfigType
        }

        ServerEditorScaffold(
            title = "SSH",
            onSaveClick = { saveServer(uiState) }
        ) {
            CommonBasicFields(uiState)
            FormTextField(
                stringResource(R.string.server_lab_security4),
                uiState.username,
                { uiState.username = it },
            )
            FormTextField(
                stringResource(R.string.server_lab_id4),
                uiState.password,
                { uiState.password = it },
            )
        }
    }
}
