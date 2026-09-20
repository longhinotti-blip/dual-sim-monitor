package com.alexandre.dualsimmonitor

import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.util.Log

/** Temporary runtime instrumentation; it does not modify parsed or persisted values. */
object CellInfoDebug {
    private const val TAG = "DualSimMonitor/CellInfo"

    fun request(subscriptionId: Int, slot: Int) {
        Log.d(TAG, "CELL_INFO_REQUEST subscriptionId=$subscriptionId slot=$slot")
    }

    fun callback(subscriptionId: Int, slot: Int, source: String, cells: List<CellInfo>) {
        Log.d(TAG, "CELL_INFO_CALLBACK subscriptionId=$subscriptionId slot=$slot SOURCE=$source count=${cells.size}")
        cells.forEachIndexed { index, cell -> logCell(subscriptionId, slot, source, index, cell) }
    }

    private fun logCell(subscriptionId: Int, slot: Int, source: String, index: Int, cell: CellInfo) {
        val ageMs = (System.currentTimeMillis() - cell.timestampMillis).coerceAtLeast(0L)
        Log.d(TAG, "CELL subscriptionId=$subscriptionId slot=$slot index=$index SOURCE=$source class=${cell.javaClass.name} registered=${cell.isRegistered} timestamp=${cell.timestampMillis} ageMs=$ageMs")
        when (cell) {
            is CellInfoLte -> logLte(subscriptionId, cell)
            is CellInfoNr -> logNr(subscriptionId, cell)
        }
    }

    private fun logLte(subscriptionId: Int, cell: CellInfoLte) {
        val signal = cell.cellSignalStrength
        val identity = cell.cellIdentity
        val rssnr = signal.rssnr
        val rssnrState = when (rssnr) {
            CellInfo.UNAVAILABLE -> "UNAVAILABLE"
            Int.MIN_VALUE -> "INT_MIN_VALUE"
            else -> "VALOR_VALIDO"
        }
        Log.d(TAG, "LTE subscriptionId=$subscriptionId dbm=${signal.dbm} rsrp=${signal.rsrp} rsrq=${signal.rsrq} rssnr=$rssnr rssnrState=$rssnrState rssi=${signal.rssi} cqi=${signal.cqi} timingAdvance=${signal.timingAdvance} pci=${identity.pci} tac=${identity.tac} earfcn=${identity.earfcn} ci=${identity.ci}")
    }

    private fun logNr(subscriptionId: Int, cell: CellInfoNr) {
        val signal = cell.cellSignalStrength
        val identity = cell.cellIdentity
        Log.d(TAG, "NR subscriptionId=$subscriptionId ssRsrp=${signal.ssRsrp} ssRsrq=${signal.ssRsrq} ssSinr=${signal.ssSinr} csiRsrp=${signal.csiRsrp} csiRsrq=${signal.csiRsrq} csiSinr=${signal.csiSinr} pci=${identity.pci} tac=${identity.tac} nrarfcn=${identity.nrarfcn} nci=${identity.nci}")
    }
}
