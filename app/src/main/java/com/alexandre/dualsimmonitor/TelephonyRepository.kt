package com.alexandre.dualsimmonitor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.CellInfo
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

data class MonitorState(val sims: List<SimInfo> = emptyList(), val dataSim: SimInfo? = null, val connectivity: ConnectivityInfo = ConnectivityInfo(), val locationGranted: Boolean = false, val phoneGranted: Boolean = false)
data class SimInfo(val number: String, val carrier: String, val slot: Int, val subscriptionId: Int, val active: Boolean, val cell: CellSnapshot?, val cells: List<CellSnapshot>)
data class ConnectivityInfo(val connected: Boolean = false, val transport: String = "Indisponível", val validated: String = "Indisponível")

class TelephonyRepository(private val context: Context) {
    fun refresh(): MonitorState {
        val location = has(Manifest.permission.ACCESS_FINE_LOCATION) || has(Manifest.permission.ACCESS_COARSE_LOCATION)
        val phone = has(Manifest.permission.READ_PHONE_STATE)
        val sm = context.getSystemService(SubscriptionManager::class.java)
        val sims = if (phone) runCatching { sm.activeSubscriptionInfoList.orEmpty().map { read(it, location) } }.getOrDefault(emptyList()) else emptyList()
        val dataId = runCatching { SubscriptionManager.getDefaultDataSubscriptionId() }.getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        return MonitorState(sims, sims.firstOrNull { it.subscriptionId == dataId }, ConnectivityMonitor.read(context), location, phone)
    }
    private fun read(info: SubscriptionInfo, location: Boolean): SimInfo {
        val tm = context.getSystemService(TelephonyManager::class.java).createForSubscriptionId(info.subscriptionId)
        val cells = if (location) runCatching { tm.allCellInfo.orEmpty() }.getOrDefault(emptyList()) else emptyList()
        val parsed = cells.map(CellInfoParser::parse)
        val n = runCatching { info.number }.getOrNull()?.takeIf { it.isNotBlank() }?.let { if (it.length > 4) "••••${it.takeLast(4)}" else "••••" } ?: "Indisponível"
        return SimInfo(n, info.carrierName?.toString().orEmpty().ifBlank { "Indisponível" }, info.simSlotIndex, info.subscriptionId, info.simSlotIndex >= 0, parsed.firstOrNull { it.registered } ?: parsed.firstOrNull(), parsed)
    }
    private fun has(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
