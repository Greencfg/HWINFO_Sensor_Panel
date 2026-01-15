package com.green.hwinfo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.green.hwinfo.data.PreferencesManager
import com.green.hwinfo.ui.screens.ConnectScreen
import com.green.hwinfo.ui.screens.MonitorScreen
import com.green.hwinfo.ui.theme.HWiNFOMonitorTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = PreferencesManager(this)

        setContent {
            HWiNFOMonitorTheme {
                val scope = rememberCoroutineScope()
                val savedIp by prefs.ipAddress.collectAsState(initial = null)

                var currentIp by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(savedIp) {
                    if (!savedIp.isNullOrEmpty() && currentIp == null) {
                        currentIp = savedIp
                    }
                }

                if (currentIp == null) {
                    ConnectScreen(
                        initialIp = savedIp ?: "",
                        onConnect = { ip ->
                            scope.launch {
                                prefs.saveIpAddress(ip)
                                currentIp = ip
                            }
                        }
                    )
                } else {
                    MonitorScreen(
                        serverIp = currentIp!!,
                        onBack = {
                            currentIp = null
                        }
                    )
                }
            }
        }
    }
}
