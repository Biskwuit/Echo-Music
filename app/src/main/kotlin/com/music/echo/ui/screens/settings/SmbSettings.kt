package iad1tya.echo.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import iad1tya.echo.music.R
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.constants.SmbHostKey
import iad1tya.echo.music.constants.SmbPortKey
import iad1tya.echo.music.constants.SmbShareKey
import iad1tya.echo.music.constants.SmbBasePathKey
import iad1tya.echo.music.constants.SmbUsernameKey
import iad1tya.echo.music.constants.SmbPasswordKey
import iad1tya.echo.music.constants.SmbEnabledKey
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.Material3SettingsGroup
import iad1tya.echo.music.ui.component.Material3SettingsItem
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.rememberPreference
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.onFocusChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmbSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    highlightKey: String? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val (smbEnabled, onSmbEnabledChange) = rememberPreference(SmbEnabledKey, defaultValue = false)
    val (host, onHostChange) = rememberPreference(SmbHostKey, defaultValue = "")
    val (port, onPortChange) = rememberPreference(SmbPortKey, defaultValue = 445)
    val (share, onShareChange) = rememberPreference(SmbShareKey, defaultValue = "")
    val (basePath, onBasePathChange) = rememberPreference(SmbBasePathKey, defaultValue = "")
    val (username, onUsernameChange) = rememberPreference(SmbUsernameKey, defaultValue = "")
    val (password, onPasswordChange) = rememberPreference(SmbPasswordKey, defaultValue = "")

    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    fun applyConfig() {
        com.music.smb.SmbService.configure(
            com.music.smb.SmbConfig(
                host = host,
                port = port,
                shareName = share,
                basePath = basePath,
                username = username,
                password = password
            )
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        Material3SettingsGroup(
            scrollState = scrollState,
            title = "Local SMB Share",
            items = buildList {
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.graphic_eq),
                    title = { Text("Enable SMB lossless source") },
                    description = { Text("Use a local SMB/CIFS share for Lossless audio, matched by YouTube video ID filename") },
                    trailingContent = {
                        Switch(
                            checked = smbEnabled,
                            onCheckedChange = {
                                onSmbEnabledChange(it)
                                if (it) applyConfig() else com.music.smb.SmbService.disconnect()
                            },
                            colors = SwitchDefaults.colors()
                        )
                    },
                    onClick = { onSmbEnabledChange(!smbEnabled) }
                ))

                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.settings),
                    title = { Text("Host / IP") },
                    description = {
                        var localHost by remember(host) { mutableStateOf(host) }
                        OutlinedTextField(
                            value = localHost,
                            onValueChange = {
                                localHost = it
                                testStatus = null
                            },
                            singleLine = true,
                            placeholder = { Text("192.168.1.*") },
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused && localHost != host) {
                                        onHostChange(localHost)
                                    }
                                }
                        )
                    }
                ))

                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.settings),
                    title = { Text("Port") },
                    description = {
                        var localPort by remember(port) { mutableStateOf(port.toString()) }
                        OutlinedTextField(
                            value = localPort,
                            onValueChange = { localPort = it },
                            singleLine = true,
                            placeholder = { Text("445") },
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused) {
                                        localPort.toIntOrNull()?.let {
                                            if (it != port) onPortChange(it)
                                        }
                                    }
                                }
                        )
                    }
                ))

                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.settings),
                    title = { Text("Share name") },
                    description = {
                        var localShare by remember(share) { mutableStateOf(share) }
                        OutlinedTextField(
                            value = localShare,
                            onValueChange = {
                                localShare = it
                                testStatus = null
                            },
                            singleLine = true,
                            placeholder = { Text("Xylo") },
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused && localShare != share) {
                                        onShareChange(localShare)
                                    }
                                }
                        )
                    }
                ))

                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.settings),
                    title = { Text("Subfolder") },
                    description = {
                        var localBasePath by remember(basePath) { mutableStateOf(basePath) }
                        OutlinedTextField(
                            value = localBasePath,
                            onValueChange = {
                                localBasePath = it
                                testStatus = null
                            },
                            singleLine = true,
                            placeholder = { Text("swingmusic/music (optional)") },
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused && localBasePath != basePath) {
                                        onBasePathChange(localBasePath)
                                    }
                                }
                        )
                    }
                ))

                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.settings),
                    title = { Text("Username") },
                    description = {
                        var localUsername by remember(username) { mutableStateOf(username) }
                        OutlinedTextField(
                            value = localUsername,
                            onValueChange = {
                                localUsername = it
                                testStatus = null
                            },
                            singleLine = true,
                            placeholder = { Text("leave blank for guest") },
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused && localUsername != username) {
                                        onUsernameChange(localUsername)
                                    }
                                }
                        )
                    }
                ))

                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.settings),
                    title = { Text("Password") },
                    description = {
                        var localPassword by remember(password) { mutableStateOf(password) }
                        OutlinedTextField(
                            value = localPassword,
                            onValueChange = {
                                localPassword = it
                                testStatus = null
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused && localPassword != password) {
                                        onPasswordChange(localPassword)
                                    }
                                }
                        )
                    }
                ))

                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.check),
                    title = { Text("Test connection") },
                    description = { Text(testStatus ?: "Save and verify the share is reachable") },
                    trailingContent = {
                        TextButton(
                            enabled = !isTesting,
                            onClick = {
                                applyConfig()
                                isTesting = true
                                testStatus = "Testing..."
                                coroutineScope.launch {
                                    val result = com.music.smb.SmbService.testConnection()
                                    testStatus = result.fold(
                                        onSuccess = { "Connected successfully" },
                                        onFailure = { "Failed: ${it.message ?: it::class.simpleName}" }
                                    )
                                    isTesting = false
                                }
                            }
                        ) {
                            Text("Test")
                        }
                    },
                    onClick = {}
                ))
            }
        )
    }

    TopAppBar(
        title = { Text("SMB Music Share") },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        }
    )
}