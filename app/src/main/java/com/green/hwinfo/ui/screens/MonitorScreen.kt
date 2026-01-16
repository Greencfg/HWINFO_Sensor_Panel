package com.green.hwinfo.ui.screens

import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.green.hwinfo.data.AppConfig
import com.green.hwinfo.data.SensorConfigManager
import com.green.hwinfo.data.TileConfig
import com.green.hwinfo.data.TileShape
import com.green.hwinfo.model.SensorData
import com.green.hwinfo.network.ApiService
import com.green.hwinfo.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt

// Shapes
val TriangleShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
fun MonitorScreen(
    serverIp: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val configManager = remember { SensorConfigManager(context) }
    val scope = rememberCoroutineScope()

    var appConfig by remember { mutableStateOf(configManager.getAppConfig()) }
    var tileConfigs by remember { mutableStateOf(configManager.getTileConfigs()) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var isEditMode by remember { mutableStateOf(false) }

    val apiService = remember(serverIp) {
        try {
            val baseUrl = if (serverIp.contains(":")) {
                if (serverIp.startsWith("http")) serverIp else "http://$serverIp/"
            } else { "http://$serverIp:8085/" }
            Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
        } catch (e: Exception) { null }
    }

    var rawSensorData by remember { mutableStateOf<List<SensorData>>(emptyList()) }
    var activeConfigs by remember { mutableStateOf<List<Pair<SensorData, TileConfig>>>(emptyList()) }     
    var hiddenConfigs by remember { mutableStateOf<List<Pair<SensorData, TileConfig>>>(emptyList()) }     
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(rawSensorData, tileConfigs) {
        if (rawSensorData.isEmpty()) return@LaunchedEffect
        var configsChanged = false
        val currentConfigs = tileConfigs.toMutableList()
        rawSensorData.forEach { sensor ->
            if (currentConfigs.none { it.originalLabel == sensor.label }) {
                val maxRow = currentConfigs.maxOfOrNull { it.gridY } ?: -1
                currentConfigs.add(TileConfig(originalLabel = sensor.label ?: "", gridX = 0, gridY = maxRow + 1))
                configsChanged = true
            }
        }
        if (configsChanged) { tileConfigs = currentConfigs; configManager.saveTileConfigs(currentConfigs) }
        val allMapped = currentConfigs.mapNotNull { config ->
            val sensor = rawSensorData.find { it.label == config.originalLabel }
            if (sensor != null) sensor to config else null
        }
        activeConfigs = allMapped.filter { !it.second.isHidden }
        hiddenConfigs = allMapped.filter { it.second.isHidden }
    }

    LaunchedEffect(serverIp, apiService) {
        while(true) {
            if (apiService != null) {
                try {
                    val baseUrl = if (serverIp.contains(":")) {
                        val base = if (serverIp.startsWith("http")) serverIp else "http://$serverIp"      
                         if (base.endsWith("/")) base else "$base/"
                    } else { "http://$serverIp:8085/" }
                    rawSensorData = apiService.getSensorData("${baseUrl}api/data")
                    errorMsg = null
                } catch (e: Exception) { errorMsg = "Connecting..." }
            } else { errorMsg = "Invalid IP Format" }
            delay(2000)
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> 
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            val newConfig = appConfig.copy(backgroundImageUri = it.toString())
            appConfig = newConfig
            configManager.saveAppConfig(newConfig)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF1E1E1E), drawerContentColor = Color.White) {
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {        
                    Text("Settings", style = MaterialTheme.typography.headlineMedium, color = NeonBlue)   
                    Spacer(Modifier.height(24.dp))
                    Text("Status", color = Color.Gray); Text(errorMsg ?: "Connected", color = if (errorMsg != null) NeonRed else NeonGreen)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text("Disconnect") }
                    Divider(Modifier.padding(vertical = 16.dp), color = Color.Gray)
                    Text("Background", color = Color.Gray)
                    Row {
                        Button(onClick = { launcher.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("Select") }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { appConfig = appConfig.copy(backgroundImageUri = null); configManager.saveAppConfig(appConfig) }) { Icon(Icons.Default.Delete, null, tint = NeonRed) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = appConfig.useGlassEffect, onCheckedChange = { 
                            appConfig = appConfig.copy(useGlassEffect = it); configManager.saveAppConfig(appConfig) 
                        }); Text("Blur Background (Glass)")
                    }
                    Text("Grid Size: ${appConfig.gridSizeDp}", color = Color.Gray)
                    Slider(value = appConfig.gridSizeDp.toFloat(), onValueChange = { 
                        appConfig = appConfig.copy(gridSizeDp = it.toInt()); configManager.saveAppConfig(appConfig) 
                    }, valueRange = 50f..250f)
                    Divider(Modifier.padding(vertical = 16.dp), color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Edit Mode", modifier = Modifier.weight(1f)); Switch(checked = isEditMode, onCheckedChange = { isEditMode = it })
                    }
                    if (hiddenConfigs.isNotEmpty()) {
                        Divider(Modifier.padding(vertical = 16.dp), color = Color.Gray)
                        Text("Hidden Items (${hiddenConfigs.size})", color = Color.Gray)
                        hiddenConfigs.forEach { (sensor, config) ->
                            Row(modifier = Modifier.fillMaxWidth().clickable {
                                val list = tileConfigs.toMutableList()
                                val idx = list.indexOfFirst { it.originalLabel == config.originalLabel }
                                if (idx != -1) { list[idx] = list[idx].copy(isHidden = false); tileConfigs = list; configManager.saveTileConfigs(list) }
                            }.padding(8.dp)) {
                                Icon(Icons.Default.Add, null, tint = NeonGreen); Spacer(Modifier.width(8.dp)); Text(config.customLabel ?: sensor.label ?: "")
                            }
                        }
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            if (!appConfig.backgroundImageUri.isNullOrEmpty()) {
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(appConfig.backgroundImageUri).crossfade(true).build(),
                    contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(if (appConfig.useGlassEffect) 20.dp else 0.dp))
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
            } else if (isEditMode) { GridBackground(appConfig.gridSizeDp) }

            IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.padding(16.dp).statusBarsPadding()) {
                Icon(Icons.Default.Menu, null, tint = Color.White)
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val cellSizePx = with(density) { appConfig.gridSizeDp.dp.toPx() }
                val maxRow = activeConfigs.maxOfOrNull { it.second.gridY + it.second.spanY } ?: 0
                val contentHeight = ((maxRow + 2) * appConfig.gridSizeDp).dp
                Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).heightIn(min = contentHeight)) {
                    activeConfigs.forEach { (sensor, config) ->
                        DraggableTile(sensor = sensor, config = config, cellSizeDp = appConfig.gridSizeDp, isEditMode = isEditMode,
                            onUpdateConfig = { newConfig ->
                                val list = tileConfigs.toMutableList()
                                val idx = list.indexOfFirst { it.originalLabel == newConfig.originalLabel }
                                if (idx != -1) { list[idx] = newConfig; tileConfigs = list; configManager.saveTileConfigs(list) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GridBackground(cellSize: Int) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = cellSize.dp.toPx()
        for (x in 0..size.width.toInt() step step.toInt()) { drawLine(Color.White.copy(0.1f), start = Offset(x.toFloat(), 0f), end = Offset(x.toFloat(), size.height)) }
        for (y in 0..size.height.toInt() step step.toInt()) { drawLine(Color.White.copy(0.1f), start = Offset(0f, y.toFloat()), end = Offset(size.width, y.toFloat())) }
    }
}

@Composable
fun DraggableTile(sensor: SensorData, config: TileConfig, cellSizeDp: Int, isEditMode: Boolean, onUpdateConfig: (TileConfig) -> Unit) {
    val density = LocalDensity.current
    val cellSizePx = with(density) { cellSizeDp.dp.toPx() }
    val offset by animateIntOffsetAsState(targetValue = IntOffset((config.gridX * cellSizePx).roundToInt(), (config.gridY * cellSizePx).roundToInt()))
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    if (showEditDialog) { EditTileDialog(config, onDismiss = { showEditDialog = false }, onSave = { onUpdateConfig(it); showEditDialog = false }) }
    val visualOffset = if (isDragging) { IntOffset((offset.x + dragOffset.x).roundToInt(), (offset.y + dragOffset.y).roundToInt()) } else { offset }
    val widthDp = (config.spanX.coerceAtLeast(1) * cellSizeDp).dp
    val heightDp = (config.spanY.coerceAtLeast(1) * cellSizeDp).dp
    Box(modifier = Modifier.offset { visualOffset }.size(widthDp, heightDp).padding(4.dp)
        .pointerInput(isEditMode) {
            if (isEditMode) {
                detectDragGesturesAfterLongPress(onDragStart = { isDragging = true }, 
                    onDragEnd = {
                        isDragging = false
                        val newGridX = ((offset.x + dragOffset.x) / cellSizePx).roundToInt().coerceAtLeast(0)
                        val newGridY = ((offset.y + dragOffset.y) / cellSizePx).roundToInt().coerceAtLeast(0)
                        dragOffset = Offset.Zero
                        if (newGridX != config.gridX || newGridY != config.gridY) onUpdateConfig(config.copy(gridX = newGridX, gridY = newGridY))
                    },
                    onDragCancel = { isDragging = false; dragOffset = Offset.Zero },
                    onDrag = { change, dragAmount -> change.consume(); dragOffset += dragAmount }
                )
            }
        }.clickable(enabled = isEditMode && !isDragging) { showEditDialog = true }
    ) { SensorShapeCard(sensor, config, isEditMode, onHide = { onUpdateConfig(config.copy(isHidden = true)) }) }
}

@Composable
fun SensorShapeCard(data: SensorData, config: TileConfig, isEditMode: Boolean, onHide: () -> Unit) {
    val shape: Shape = when(config.shape ?: TileShape.SQUARE) {
        TileShape.CIRCLE -> CircleShape; TileShape.TRIANGLE -> TriangleShape; else -> RoundedCornerShape(16.dp)
    }
    val cardBgColor = if (config.customColor != null) Color(config.customColor!!) else Color.DarkGray.copy(alpha = 0.5f)
    val titleColor = if (config.customTitleColor != null) Color(config.customTitleColor!!) else TextSecondary
    val valueColor = if (config.customValueColor != null) Color(config.customValueColor!!) else Color.White
    Box(modifier = Modifier.fillMaxSize().shadow(4.dp, shape).clip(shape)
        .background(Brush.verticalGradient(listOf(cardBgColor.copy(alpha=0.8f), cardBgColor.copy(alpha=0.5f))))
        .border(1.dp, Color.White.copy(0.2f), shape).padding(8.dp), contentAlignment = Alignment.Center) {
        if (isEditMode) {
            Icon(Icons.Default.Close, null, modifier = Modifier.align(Alignment.TopEnd).size(20.dp).clickable { onHide() }, tint = NeonRed)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = config.customLabel ?: data.label ?: "", style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * config.titleSizeScale).sp), color = titleColor, maxLines = 1)
            Text(text = data.value ?: "", style = MaterialTheme.typography.headlineMedium.copy(fontSize = (24 * config.valueSizeScale).sp), fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTileDialog(config: TileConfig, onDismiss: () -> Unit, onSave: (TileConfig) -> Unit) {
    var name by remember { mutableStateOf(config.customLabel ?: config.originalLabel) }
    var bgColor by remember { mutableStateOf(config.customColor) }
    var titleColor by remember { mutableStateOf(config.customTitleColor) }
    var valueColor by remember { mutableStateOf(config.customValueColor) }
    var shape by remember { mutableStateOf(config.shape ?: TileShape.SQUARE) }
    var titleScale by remember { mutableStateOf(config.titleSizeScale) }
    var valueScale by remember { mutableStateOf(config.valueSizeScale) }
    var spanX by remember { mutableStateOf(config.spanX.toFloat()) }
    var spanY by remember { mutableStateOf(config.spanY.toFloat()) }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).verticalScroll(rememberScrollState())) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Edit Tile", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp)); Text("Size: W:${spanX.toInt()} H:${spanY.toInt()}", color = Color.Gray)
                Row {
                    Slider(value = spanX, onValueChange = { spanX = it }, valueRange = 1f..4f, steps = 2, modifier = Modifier.weight(1f))
                    Slider(value = spanY, onValueChange = { spanY = it }, valueRange = 1f..4f, steps = 2, modifier = Modifier.weight(1f))
                }
                Text("Shape", color = Color.Gray)
                Row { TileShape.values().forEach { s -> FilterChip(selected = shape == s, onClick = { shape = s }, label = { Text(s.name) }); Spacer(Modifier.width(4.dp)) } }
                Text("Text Scales", color = Color.Gray)
                Slider(value = titleScale, onValueChange = { titleScale = it }, valueRange = 0.5f..2.0f)
                Slider(value = valueScale, onValueChange = { valueScale = it }, valueRange = 0.5f..2.5f)
                Text("Colors", color = Color.Gray)
                ColorRow(bgColor) { bgColor = it }; ColorRow(titleColor) { titleColor = it }; ColorRow(valueColor) { valueColor = it }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onSave(config.copy(customLabel = name, customColor = bgColor, customTitleColor = titleColor, 
                        customValueColor = valueColor, titleSizeScale = titleScale, valueSizeScale = valueScale, shape = shape, 
                        spanX = spanX.toInt(), spanY = spanY.toInt())) }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun ColorRow(selected: Long?, onSelect: (Long?) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        val colors = listOf(null, Color.White.toArgb().toLong(), NeonRed.toArgb().toLong(), NeonBlue.toArgb().toLong(), NeonGreen.toArgb().toLong(), NeonYellow.toArgb().toLong())
        colors.forEach { c -> Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(if (c == null) Color.Gray else Color(c))
                .border(2.dp, if (selected == c) Color.White else Color.Transparent, CircleShape).clickable { onSelect(c) }) }
    }
}
