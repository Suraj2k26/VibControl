package com.vibcontrol

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Data models ─────────────────────────────────────────────
data class VibPattern(
    val id: String,
    val name: String,
    val desc: String,
    val cmdByte: Byte,
    val vizHeights: List<Int>   // 0–8, for the waveform preview bars
)

data class LogEntry(
    val time: String,
    val message: String,
    val level: BleManager.LogLevel
)

data class UiState(
    val bleState: BleManager.State       = BleManager.State.Disconnected,
    val selectedPatternId: String        = "single",
    val intensity: Int                   = 80,   // 1–100
    val customSeq: List<Boolean>         = listOf(true, false, true, false),
    val logs: List<LogEntry>             = emptyList()
)

// ── ViewModel ────────────────────────────────────────────────
class MainViewModel(app: Application) : AndroidViewModel(app) {

    val bleManager = BleManager(app)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val patterns = listOf(
        VibPattern("single",     "Single pulse",  "One short buzz",        0x01, listOf(0,8,0,0,0,0,0,0)),
        VibPattern("double",     "Double tap",    "Two quick buzzes",      0x02, listOf(0,7,0,4,0,0,0,0)),
        VibPattern("sos",        "SOS",           "··· — — — ···",         0x03, listOf(2,2,2,6,6,6,2,2)),
        VibPattern("heartbeat",  "Heartbeat",     "Lub-dub rhythm",        0x04, listOf(0,8,5,0,0,8,5,0)),
        VibPattern("escalate",   "Escalate",      "Ramps up intensity",    0x05, listOf(1,2,3,4,5,6,7,8)),
        VibPattern("continuous", "Continuous",    "1 second hold",         0x06, listOf(7,7,7,7,7,7,7,7)),
    )

    init {
        bleManager.onState = { state ->
            _uiState.value = _uiState.value.copy(bleState = state)
        }
        bleManager.onLog = { msg, level ->
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val entry = LogEntry(time, msg, level)
            val updated = listOf(entry) + _uiState.value.logs.take(29)
            _uiState.value = _uiState.value.copy(logs = updated)
        }
    }

    fun selectPattern(id: String) {
        _uiState.value = _uiState.value.copy(selectedPatternId = id)
    }

    fun setIntensity(value: Int) {
        _uiState.value = _uiState.value.copy(intensity = value)
    }

    fun toggleCustomStep(index: Int) {
        val seq = _uiState.value.customSeq.toMutableList()
        seq[index] = !seq[index]
        _uiState.value = _uiState.value.copy(customSeq = seq)
    }

    fun addCustomStep(on: Boolean) {
        val seq = _uiState.value.customSeq
        if (seq.size < 16) {
            _uiState.value = _uiState.value.copy(customSeq = seq + on)
        }
    }

    fun clearCustomSeq() {
        _uiState.value = _uiState.value.copy(customSeq = listOf(true, false, true, false))
    }

    fun sendSelectedPattern() {
        val pattern = patterns.find { it.id == _uiState.value.selectedPatternId } ?: return
        val intensity = _uiState.value.intensity.toByte()
        bleManager.sendBytes(byteArrayOf(pattern.cmdByte, intensity))
    }

    fun sendCustomPattern() {
        val seq = _uiState.value.customSeq
        val maskBytes = ByteArray((seq.size + 7) / 8)
        seq.forEachIndexed { i, on ->
            if (on) maskBytes[i / 8] = (maskBytes[i / 8].toInt() or (1 shl (i % 8))).toByte()
        }
        val intensity = _uiState.value.intensity.toByte()
        val payload = byteArrayOf(0xF0.toByte(), seq.size.toByte()) + maskBytes + byteArrayOf(intensity)
        bleManager.sendBytes(payload)
    }
}
