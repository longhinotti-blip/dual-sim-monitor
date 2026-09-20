package com.alexandre.dualsimmonitor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.CellInfo
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class TelephonyRepository(private val context: Context) {
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private val worker = Executors.newSingleThreadExecutor()

    fun refresh(callback: (MonitorState) -> Unit) { worker.execute { collectState(callback) } }

    fun monitorAndSave(callback: (MonitorState) -> Unit) {
        worker.execute {
            collectState { rawState -> worker.execute {
                val probe = ConnectivityMonitor.measure(context)
                val measuredState = rawState.copy(connectivity = rawState.connectivity.copy(latencyMs = probe.latencyMs, packetLossPercent = probe.packetLossPercent, probeMethod = probe.method))
                val dataId = measuredState.dataSim?.subscriptionId ?: SubscriptionManager.INVALID_SUBSCRIPTION_ID
                val rows = measuredState.sims.flatMap { sim ->
                    sim.cells.filter { it.registered }.ifEmpty { sim.cell?.let(::listOf).orEmpty() }.map { cell ->
                        val isData = sim.subscriptionId == dataId
                        Measurement(measuredState.timestamp, sim.subscriptionId, sim.slot + 1, sim.carrier, cell.dbm, cell.rsrp, cell.rsrq, cell.sinr, cell.technology, cell.cell, probe.latencyMs.takeIf { isData }, probe.packetLossPercent.takeIf { isData }, dataId, isData)
                    }
                }
                if (rows.isNotEmpty()) HistoryStore(context).append(rows)
                mainExecutor.execute { callback(measuredState) }
            } }
        }
    }

    private fun collectState(callback: (MonitorState) -> Unit) {
        val location = has(Manifest.permission.ACCESS_FINE_LOCATION) || has(Manifest.permission.ACCESS_COARSE_LOCATION)
        val phone = has(Manifest.permission.READ_PHONE_STATE)
        val manager = context.getSystemService(SubscriptionManager::class.java)
        val subscriptions = if (phone) runCatching { manager.activeSubscriptionInfoList.orEmpty() }.getOrDefault(emptyList()) else emptyList()
        collectSubscriptions(subscriptions, location, 0, mutableListOf(), mutableListOf()) { sims, diagnostics ->
            val dataId = runCatching { SubscriptionManager.getDefaultDataSubscriptionId() }.getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
            val timestamp = System.currentTimeMillis()
            mainExecutor.execute { callback(MonitorState(sims, sims.firstOrNull { it.subscriptionId == dataId }, ConnectivityMonitor.read(context), location, phone, timestamp, diagnostics)) }
        }
    }

    private fun collectSubscriptions(list: List<SubscriptionInfo>, location: Boolean, index: Int, result: MutableList<SimInfo>, diagnostics: MutableList<CellDiagnostic>, done: (List<SimInfo>, List<CellDiagnostic>) -> Unit) {
        if (index >= list.size) { done(result, diagnostics); return }
        val info = list[index]
        val tm = context.getSystemService(TelephonyManager::class.java).createForSubscriptionId(info.subscriptionId)
        requestFresh(tm, info.subscriptionId, info.simSlotIndex, location) { cells, cellDiagnostics ->
            val parsed = cells.map(CellInfoParser::parse)
            result += SimInfo(info.carrierName?.toString().orEmpty().ifBlank { "Indisponível" }, info.simSlotIndex, info.subscriptionId, info.simSlotIndex >= 0, parsed.firstOrNull { it.registered } ?: parsed.firstOrNull(), parsed, System.currentTimeMillis())
            diagnostics += cellDiagnostics
            collectSubscriptions(list, location, index + 1, result, diagnostics, done)
        }
    }

    private fun requestFresh(tm: TelephonyManager, subscriptionId: Int, slot: Int, location: Boolean, done: (List<CellInfo>, List<CellDiagnostic>) -> Unit) {
        if (!location) {
            val cells = runCatching { tm.allCellInfo.orEmpty() }.getOrDefault(emptyList())
            done(cells, CellInfoDebug.callback(subscriptionId, slot, "allCellInfo", cells)); return
        }
        var completed = false
        fun finish(value: List<CellInfo>, source: String) { if (!completed) { completed = true; done(value, CellInfoDebug.callback(subscriptionId, slot, source, value)) } }
        CellInfoDebug.request(subscriptionId, slot)
        runCatching {
            tm.requestCellInfoUpdate(mainExecutor, object : TelephonyManager.CellInfoCallback() { override fun onCellInfo(cellInfo: MutableList<CellInfo>) = finish(cellInfo, "requestCellInfoUpdate") })
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ finish(runCatching { tm.allCellInfo.orEmpty() }.getOrDefault(emptyList()), "allCellInfo") }, 3500L)
        }.onFailure { android.util.Log.e("DualSimMonitor/CellInfo", "CELL_INFO_ERROR subscriptionId=$subscriptionId slot=$slot source=requestCellInfoUpdate", it); finish(runCatching { tm.allCellInfo.orEmpty() }.getOrDefault(emptyList()), "allCellInfo") }
    }

    fun loadHistory(): List<Measurement> = HistoryStore(context).read()
    fun clearHistory() = HistoryStore(context).clear()
    fun exportDiagnostic(state: MonitorState): String = buildString { appendLine("timestamp,sim,subscriptionId,operadora,technology,registered,cellId,pci,tac,earfcn,nrarfcn,dbm,asu,rsrp,rsrq,sinr,timestampAge"); state.sims.forEach { sim -> sim.cells.forEach { c -> appendLine("${sim.timestamp},${sim.slot + 1},${sim.subscriptionId},${sim.carrier},${c.technology},${c.registered},${c.cell},${c.pci},${c.tac},${c.earfcn},${c.nrarfcn},${c.dbm},${c.asu},${c.rsrp},${c.rsrq},${c.sinr},0") } } }
    private fun has(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

data class MonitorState(val sims: List<SimInfo> = emptyList(), val dataSim: SimInfo? = null, val connectivity: ConnectivityInfo = ConnectivityInfo(), val locationGranted: Boolean = false, val phoneGranted: Boolean = false, val timestamp: Long = 0L, val diagnostics: List<CellDiagnostic> = emptyList())
data class SimInfo(val carrier: String, val slot: Int, val subscriptionId: Int, val active: Boolean, val cell: CellSnapshot?, val cells: List<CellSnapshot>, val timestamp: Long)
data class ConnectivityInfo(val connected: Boolean = false, val transport: String = "Indisponível", val validated: String = "Indisponível", val latencyMs: Long? = null, val packetLossPercent: Float? = null, val probeMethod: String = "HTTPS")
data class ConnectivityProbe(val latencyMs: Long?, val packetLossPercent: Float?, val method: String = "HTTPS")

private class HistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE)
    fun read(): List<Measurement> = prefs.getStringSet("rows", emptySet()).orEmpty().mapNotNull { row -> row.split("|").takeIf { it.size >= 13 }?.let { p -> Measurement(p[0].toLongOrNull() ?: return@let null, p[1].toIntOrNull() ?: return@let null, p[2].toIntOrNull() ?: return@let null, p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10].toLongOrNull(), p[11].toFloatOrNull(), p[12].toIntOrNull() ?: -1) } }.sortedByDescending { it.timestampMillis }
    fun append(rows: List<Measurement>) { val all = (read() + rows).sortedByDescending { it.timestampMillis }.take(500); prefs.edit().putStringSet("rows", all.map { listOf(it.timestampMillis,it.subscriptionId,it.slot,it.carrier,it.dbm,it.rsrp,it.rsrq,it.sinr,it.technology,it.cell,it.latencyMillis ?: "",it.packetLossPercent ?: "",it.dataSubscriptionId,it.isDefaultDataSim).joinToString("|") }.toSet()).apply() }
    fun clear() { prefs.edit().remove("rows").apply() }
}
