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
    private val executor = ContextCompat.getMainExecutor(context)
    private val worker = Executors.newSingleThreadExecutor()

    fun refresh(callback: (MonitorState) -> Unit) {
        worker.execute {
            val location = has(Manifest.permission.ACCESS_FINE_LOCATION) || has(Manifest.permission.ACCESS_COARSE_LOCATION)
            val phone = has(Manifest.permission.READ_PHONE_STATE)
            val manager = context.getSystemService(SubscriptionManager::class.java)
            val subscriptions = if (phone) runCatching { manager.activeSubscriptionInfoList.orEmpty() }.getOrDefault(emptyList()) else emptyList()
            collect(subscriptions, location, 0, mutableListOf()) { sims ->
                val dataId = runCatching { SubscriptionManager.getDefaultDataSubscriptionId() }.getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                executor.execute { callback(MonitorState(sims, sims.firstOrNull { it.subscriptionId == dataId }, ConnectivityMonitor.read(context), location, phone)) }
            }
        }
    }

    private fun collect(list: List<SubscriptionInfo>, location: Boolean, index: Int, result: MutableList<SimInfo>, done: (List<SimInfo>) -> Unit) {
        if (index >= list.size) { done(result); return }
        val info = list[index]
        val tm = context.getSystemService(TelephonyManager::class.java).createForSubscriptionId(info.subscriptionId)
        requestFresh(tm, info.subscriptionId, info.simSlotIndex, location) { cells ->
            val parsed = cells.map(CellInfoParser::parse)
            val sim = SimInfo(info.carrierName?.toString().orEmpty().ifBlank { "Indisponível" }, info.simSlotIndex, info.subscriptionId, info.simSlotIndex >= 0, parsed.firstOrNull { it.registered } ?: parsed.firstOrNull(), parsed, System.currentTimeMillis())
            result += sim
            collect(list, location, index + 1, result, done)
        }
    }

    private fun requestFresh(tm: TelephonyManager, subscriptionId: Int, slot: Int, location: Boolean, done: (List<CellInfo>) -> Unit) {
        if (!location || android.os.Build.VERSION.SDK_INT < 29) {
            val cells = runCatching { tm.allCellInfo.orEmpty() }.getOrDefault(emptyList())
            CellInfoDebug.callback(subscriptionId, slot, "allCellInfo", cells)
            done(cells)
            return
        }
        var completed = false
        fun finish(value: List<CellInfo>, source: String) {
            if (!completed) {
                completed = true
                CellInfoDebug.callback(subscriptionId, slot, source, value)
                done(value)
            }
        }
        CellInfoDebug.request(subscriptionId, slot)
        runCatching {
            tm.requestCellInfoUpdate(executor, object : TelephonyManager.CellInfoCallback() {
                override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                    finish(cellInfo, "requestCellInfoUpdate")
                }
            })
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                finish(runCatching { tm.allCellInfo.orEmpty() }.getOrDefault(emptyList()), "allCellInfo")
            }, 3500L)
        }.onFailure {
            android.util.Log.e("DualSimMonitor/CellInfo", "CELL_INFO_ERROR subscriptionId=$subscriptionId slot=$slot source=requestCellInfoUpdate", it)
            finish(runCatching { tm.allCellInfo.orEmpty() }.getOrDefault(emptyList()), "allCellInfo")
        }
    }

    fun loadHistory(): List<Measurement> = HistoryStore(context).read()
    fun save(measurements: List<Measurement>) = HistoryStore(context).append(measurements)
    fun clearHistory() = HistoryStore(context).clear()
    fun exportDiagnostic(state: MonitorState): String = buildString {
        appendLine("timestamp,sim,subscriptionId,operadora,technology,registered,cellId,pci,tac,earfcn,nrarfcn,dbm,asu,rsrp,rsrq,rssnr,sinr,timestampAge")
        state.sims.forEachIndexed { index, sim -> sim.cells.forEach { c -> appendLine("${sim.timestamp},${index + 1},${sim.subscriptionId},${sim.carrier},${c.technology},${c.registered},${c.cell},${c.pci},${c.tac},${c.earfcn},${c.nrarfcn},${c.dbm},${c.asu},${c.rsrp},${c.rsrq},${c.sinr},0") } }
    }
    private fun has(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

data class MonitorState(val sims: List<SimInfo> = emptyList(), val dataSim: SimInfo? = null, val connectivity: ConnectivityInfo = ConnectivityInfo(), val locationGranted: Boolean = false, val phoneGranted: Boolean = false)
data class SimInfo(val carrier: String, val slot: Int, val subscriptionId: Int, val active: Boolean, val cell: CellSnapshot?, val cells: List<CellSnapshot>, val timestamp: Long)
data class ConnectivityInfo(val connected: Boolean = false, val transport: String = "Indisponível", val validated: String = "Indisponível")

private class HistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE)
    fun read(): List<Measurement> = prefs.getStringSet("rows", emptySet()).orEmpty().mapNotNull { row -> row.split("|").takeIf { it.size >= 14 }?.let { p -> Measurement(p[0].toLongOrNull() ?: 0, p[1].toIntOrNull() ?: 0, p[2].toIntOrNull() ?: 0, p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10].toLongOrNull(), p[11].toFloatOrNull(), p[12].toIntOrNull() ?: -1) } }
    fun append(rows: List<Measurement>) { val all = read().toMutableList(); all.addAll(rows); prefs.edit().putStringSet("rows", all.take(500).map { listOf(it.timestampMillis,it.subscriptionId,it.slot,it.carrier,it.dbm,it.rsrp,it.rsrq,it.sinr,it.technology,it.cell,it.latencyMillis ?: "",it.packetLossPercent ?: "",it.dataSubscriptionId).joinToString("|") }.toSet()).apply() }
    fun clear() { prefs.edit().remove("rows").apply() }
}
