package com.alexandre.dualsimmonitor

import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.util.Log

data class CellDiagnostic(
    val subscriptionId: Int,
    val slot: Int,
    val cellClass: String,
    val registered: Boolean,
    val cellCount: Int,
    val timestampMillis: Long,
    val ageMillis: Long,
    val dbmRaw: String = "—",
    val rsrpRaw: String = "—",
    val rsrqRaw: String = "—",
    val rssnrRaw: String = "—",
    val rssnrFormatted: String = "—",
    val pci: String = "—",
    val tac: String = "—",
    val earfcn: String = "—",
    val ciOrNci: String = "—"
)

/** Temporary runtime diagnostics only. It does not modify parsed or persisted values. */
object CellInfoDebug {
    private const val TAG = "DualSimMonitor/CellInfo"

    fun request(subscriptionId: Int, slot: Int) = Log.d(TAG, "CELL_INFO_REQUEST subscriptionId=$subscriptionId slot=$slot")

    fun callback(subscriptionId: Int, slot: Int, source: String, cells: List<CellInfo>): List<CellDiagnostic> {
        Log.d(TAG, "CELL_INFO_CALLBACK subscriptionId=$subscriptionId slot=$slot SOURCE=$source count=${cells.size}")
        return cells.mapIndexed { index, cell ->
            val diagnostic = describe(subscriptionId, slot, cells.size, cell)
            Log.d(TAG, "CELL index=$index source=$source $diagnostic")
            diagnostic
        }
    }

    private fun describe(subscriptionId: Int, slot: Int, count: Int, cell: CellInfo): CellDiagnostic {
        val timestamp = cell.timestampMillis
        val age = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
        return when (cell) {
            is CellInfoLte -> {
                val s = cell.cellSignalStrength
                val i = cell.cellIdentity
                val raw = s.rssnr
                val formatted = when {
                    raw == CellInfo.UNAVAILABLE -> "Indisponível"
                    raw == Int.MIN_VALUE -> "Indisponível"
                    else -> raw.toString()
                }
                CellDiagnostic(subscriptionId, slot, cell.javaClass.name, cell.isRegistered, count, timestamp, age,
                    s.dbm.toString(), s.rsrp.toString(), s.rsrq.toString(), rawState(raw), formatted,
                    i.pci.toString(), i.tac.toString(), i.earfcn.toString(), i.ci.toString())
            }
            is CellInfoNr -> CellDiagnostic(subscriptionId, slot, cell.javaClass.name, cell.isRegistered, count, timestamp, age,
                cell.cellSignalStrength.dbm.toString(), "NR", "NR", "NR", "NR",
                read(cell.cellIdentity, "getPci"), read(cell.cellIdentity, "getTac"), read(cell.cellIdentity, "getNrarfcn"), read(cell.cellIdentity, "getNci"))
            else -> CellDiagnostic(subscriptionId, slot, cell.javaClass.name, cell.isRegistered, count, timestamp, age)
        }
    }

    private fun rawState(value: Int): String = when (value) {
        CellInfo.UNAVAILABLE -> "UNAVAILABLE"
        Int.MIN_VALUE -> "INT_MIN_VALUE"
        else -> value.toString()
    }

    private fun read(target: Any?, methodName: String): String {
        val method = target?.javaClass?.methods?.firstOrNull { it.name == methodName } ?: return "null"
        return runCatching { method.invoke(target)?.toString() ?: "null" }.getOrDefault("null")
    }
}
