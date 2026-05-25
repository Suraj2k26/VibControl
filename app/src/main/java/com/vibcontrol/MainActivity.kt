package com.vibcontrol

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ── Theme ────────────────────────────────────────────────────
private val Teal600   = Color(0xFF0F6E56)
private val Teal500   = Color(0xFF1D9E75)
private val Teal100   = Color(0xFF9FE1CB)
private val Teal50    = Color(0xFFE1F5EE)
private val Red400    = Color(0xFFE24B4A)
private val Amber400  = Color(0xFFBA7517)
private val Gray50    = Color(0xFFF8F9FA)
private val Gray200   = Color(0xFFE9ECEF)
private val Gray500   = Color(0xFF6C757D)

private val VibColorScheme = lightColorScheme(
    primary            = Teal500,
    onPrimary          = Color.White,
    primaryContainer   = Teal50,
    onPrimaryContainer = Teal600,
    surface            = Color.White,
    onSurface          = Color(0xFF1C1B1F),
    surfaceVariant     = Gray50,
    onSurfaceVariant   = Gray500,
    outline            = Gray200,
    background         = Color(0xFFF5F7F6),
    onBackground       = Color(0xFF1C1B1F),
)

// ── Activity ─────────────────────────────────────────────────
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* handled in connectFlow */ }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            startScanSafe()
        } else {
            viewModel.bleManager.onLog?.invoke(
                "Bluetooth permissions denied — grant them in Settings",
                BleManager.LogLevel.ERROR
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = VibColorScheme) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                VibControlScreen(
                    uiState    = uiState,
                    patterns   = viewModel.patterns,
                    onConnect  = { connectFlow() },
                    onDisconnect = { disconnectSafe() },
                    onSelectPattern   = viewModel::selectPattern,
                    onSetIntensity    = viewModel::setIntensity,
                    onSendPattern     = { sendSafe { viewModel.sendSelectedPattern() } },
                    onToggleStep      = viewModel::toggleCustomStep,
                    onAddStep         = viewModel::addCustomStep,
                    onClearSeq        = viewModel::clearCustomSeq,
                    onSendCustom      = { sendSafe { viewModel.sendCustomPattern() } }
                )
            }
        }
    }

    private fun connectFlow() {
        val btAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (btAdapter == null || !btAdapter.isEnabled) {
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        requestPermissionsThenScan()
    }

    private fun requestPermissionsThenScan() {
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        permissionLauncher.launch(needed)
    }

    @androidx.annotation.RequiresPermission(allOf = [
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    ])
    private fun startScanSafe() {
        viewModel.bleManager.startScan()
    }

    private fun disconnectSafe() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            viewModel.bleManager.disconnect()
        } else {
            viewModel.bleManager.disconnect()
        }
    }

    private fun sendSafe(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            action()
        } else {
            action()
        }
    }
}

// ── Screen ────────────────────────────────────────────────────
@Composable
fun VibControlScreen(
    uiState: UiState,
    patterns: List<VibPattern>,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSelectPattern: (String) -> Unit,
    onSetIntensity: (Int) -> Unit,
    onSendPattern: () -> Unit,
    onToggleStep: (Int) -> Unit,
    onAddStep: (Boolean) -> Unit,
    onClearSeq: () -> Unit,
    onSendCustom: () -> Unit,
) {
    val connected = uiState.bleState is BleManager.State.Connected

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp)
        ) {
            // Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Teal50),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Wifi, contentDescription = null, tint = Teal500, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text("VibControl", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text("ESP32-C3 Motor Controller", fontSize = 13.sp, color = Gray500)
                    }
                }
            }

            // Status bar
            item {
                StatusCard(
                    state = uiState.bleState,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect
                )
            }

            // Pattern grid
            item {
                SectionLabel("Preset patterns")
                Spacer(Modifier.height(8.dp))
                PatternGrid(
                    patterns = patterns,
                    selectedId = uiState.selectedPatternId,
                    onSelect = onSelectPattern
                )
            }

            // Intensity
            item {
                SectionLabel("Intensity")
                Spacer(Modifier.height(6.dp))
                IntensityRow(intensity = uiState.intensity, onChanged = onSetIntensity)
            }

            // Custom sequence
            item {
                SectionLabel("Custom sequence")
                Spacer(Modifier.height(8.dp))
                CustomSequenceCard(
                    seq = uiState.customSeq,
                    onToggle = onToggleStep,
                    onAddOn = { onAddStep(true) },
                    onAddOff = { onAddStep(false) },
                    onClear = onClearSeq,
                    onSend = onSendCustom
                )
            }

            // Send button
            item {
                Button(
                    onClick = onSendPattern,
                    enabled = connected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Teal500,
                        disabledContainerColor = Gray200
                    )
                ) {
                    Icon(Icons.Outlined.Vibration, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send pattern", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Log
            item {
                SectionLabel("Log")
                Spacer(Modifier.height(6.dp))
                LogCard(logs = uiState.logs)
            }
        }
    }
}

// ── Status card ───────────────────────────────────────────────
@Composable
fun StatusCard(
    state: BleManager.State,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val (dotColor, statusText, btnText, btnAction) = when (state) {
        is BleManager.State.Connected   -> Quadruple(Teal500, "Connected to ${state.deviceName}", "Disconnect", onDisconnect)
        is BleManager.State.Scanning    -> Quadruple(Amber400, "Scanning…", "Cancel", onDisconnect)
        is BleManager.State.Disconnected -> Quadruple(Gray500, "Not connected", "Connect", onConnect)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Gray200),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Animated dot
            val animAlpha by animateFloatAsState(if (state is BleManager.State.Scanning) 0.4f else 1f)
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = animAlpha))
            )
            Text(statusText, modifier = Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            OutlinedButton(
                onClick = btnAction,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, if (state is BleManager.State.Connected) Red400.copy(alpha = 0.6f) else Gray200),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (state is BleManager.State.Connected) Red400 else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(btnText, fontSize = 13.sp)
            }
        }
    }
}

// ── Pattern grid ──────────────────────────────────────────────
@Composable
fun PatternGrid(patterns: List<VibPattern>, selectedId: String, onSelect: (String) -> Unit) {
    val icons = mapOf(
        "single"     to Icons.Outlined.RadioButtonUnchecked,
        "double"     to Icons.Outlined.DensitySmall,
        "sos"        to Icons.Outlined.Campaign,
        "heartbeat"  to Icons.Outlined.MonitorHeart,
        "escalate"   to Icons.Outlined.TrendingUp,
        "continuous" to Icons.Outlined.LinearScale
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        patterns.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { pattern ->
                    PatternCard(
                        pattern  = pattern,
                        selected = pattern.id == selectedId,
                        icon     = icons[pattern.id] ?: Icons.Outlined.Circle,
                        onSelect = { onSelect(pattern.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PatternCard(
    pattern: VibPattern,
    selected: Boolean,
    icon: ImageVector,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg     = if (selected) Teal50    else Color.White
    val border = if (selected) Teal500   else Gray200
    val tint   = if (selected) Teal500   else Gray500
    val textClr = if (selected) Teal600  else MaterialTheme.colorScheme.onSurface
    val subClr = if (selected) Teal500   else Gray500

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected) 1.5.dp else 0.5.dp, border),
        color = bg
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(pattern.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textClr)
            Text(pattern.desc, fontSize = 11.sp, color = subClr, lineHeight = 15.sp)
            Spacer(Modifier.height(8.dp))
            WaveformViz(heights = pattern.vizHeights, active = selected)
        }
    }
}

@Composable
fun WaveformViz(heights: List<Int>, active: Boolean) {
    val barColor = if (active) Teal500 else Gray200
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(18.dp)
    ) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height((3 + h * 1.875f).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

// ── Intensity row ─────────────────────────────────────────────
@Composable
fun IntensityRow(intensity: Int, onChanged: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Slider(
            value         = intensity.toFloat(),
            onValueChange = { onChanged(it.toInt()) },
            valueRange    = 1f..100f,
            modifier      = Modifier.weight(1f),
            colors        = SliderDefaults.colors(thumbColor = Teal500, activeTrackColor = Teal500, inactiveTrackColor = Gray200)
        )
        Text("$intensity%", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(40.dp))
    }
}

// ── Custom sequence ────────────────────────────────────────────
@Composable
fun CustomSequenceCard(
    seq: List<Boolean>,
    onToggle: (Int) -> Unit,
    onAddOn: () -> Unit,
    onAddOff: () -> Unit,
    onClear: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, Gray200),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Pulse steps
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                seq.forEachIndexed { i, on ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (on) Teal50 else Gray50)
                            .border(0.5.dp, if (on) Teal100 else Gray200, RoundedCornerShape(6.dp))
                            .clickable { onToggle(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (on) "●" else "○",
                            fontSize = 14.sp,
                            color = if (on) Teal500 else Gray500
                        )
                    }
                }
            }
            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SeqChip("Clear", Icons.Outlined.Delete, onClick = onClear)
                SeqChip("+ On",  Icons.Outlined.Add,    onClick = onAddOn)
                SeqChip("+ Off", Icons.Outlined.Remove, onClick = onAddOff)
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onSend,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal500)
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Send", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun SeqChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, Gray200)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp)
    }
}

// ── Log ───────────────────────────────────────────────────────
@Composable
fun LogCard(logs: List<LogEntry>) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Gray50,
        border = BorderStroke(0.5.dp, Gray200)
    ) {
        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text("No activity yet", fontSize = 12.sp, color = Gray500, fontFamily = FontFamily.Monospace)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                logs.forEach { entry ->
                    val color = when (entry.level) {
                        BleManager.LogLevel.OK    -> Teal600
                        BleManager.LogLevel.ERROR -> Red400
                        BleManager.LogLevel.INFO  -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        "${entry.time}  ${entry.message}",
                        fontSize = 11.sp,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────
@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.08.sp,
        color = Gray500
    )
}

data class Quadruple<A,B,C,D>(val a: A, val b: B, val c: C, val d: D)

@Composable
private fun animateFloatAsState(target: Float): State<Float> {
    return androidx.compose.animation.core.animateFloatAsState(
        targetValue = target,
        animationSpec = androidx.compose.animation.core.tween(600),
        label = "alpha"
    )
}
