package com.green.hwinfo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.green.hwinfo.ui.theme.NeonBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    initialIp: String,
    onConnect: (String) -> Unit
) {
    var ipText by remember { mutableStateOf(initialIp) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "HWiNFO MONITOR",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonBlue
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = ipText,
            onValueChange = { ipText = it },
            label = { Text("Server IP Address") },
            placeholder = { Text("192.168.1.X") },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = NeonBlue,
                unfocusedIndicatorColor = Color.Gray,
                focusedLabelColor = NeonBlue,
                cursorColor = NeonBlue
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onConnect(ipText) },
            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("CONNECT", color = Color.Black)
        }
    }
}
