package com.alexandre.dualsimmonitor

import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma

object CellInfoParser {
    fun parse(cell: CellInfo): CellSnapshot = when (cell) {
        is CellInfoLte -> CellSnapshot("4G / LTE", cell.isRegistered, cell.cellIdentity.ci.readable(), cell.cellSignalStrength.dbm.readable(), cell.cellSignalStrength.asuLevel.readable(), cell.cellSignalStrength.rsrp.readable(), cell.cellSignalStrength.rsrq.readable(), cell.cellSignalStrength.rssnr.readable())
        is CellInfoNr -> { val s = cell.cellSignalStrength; CellSnapshot("5G / NR", cell.isRegistered, cell.cellIdentity.toString(), s.dbm.readable(), s.asuLevel.readable(), s.ssRsrp.readable(), s.ssRsrq.readable(), s.ssSinr.readable()) }
        is CellInfoWcdma -> CellSnapshot("3G / WCDMA", cell.isRegistered, cell.cellIdentity.cid.readable(), cell.cellSignalStrength.dbm.readable(), cell.cellSignalStrength.asuLevel.readable())
        is CellInfoGsm -> CellSnapshot("2G / GSM", cell.isRegistered, cell.cellIdentity.cid.readable(), cell.cellSignalStrength.dbm.readable(), cell.cellSignalStrength.asuLevel.readable())
        is CellInfoCdma -> CellSnapshot("CDMA", cell.isRegistered, cell.cellIdentity.basestationId.readable(), cell.cellSignalStrength.dbm.readable(), cell.cellSignalStrength.asuLevel.readable())
        else -> CellSnapshot("Desconhecida", cell.isRegistered, cell.toString())
    }
    private fun Int.readable() = if (this == Int.MAX_VALUE || this < 0) "Indisponível" else toString()
}

data class CellSnapshot(val technology: String, val registered: Boolean, val cell: String, val dbm: String = "Indisponível", val asu: String = "Indisponível", val rsrp: String = "Indisponível", val rsrq: String = "Indisponível", val sinr: String = "Indisponível")
