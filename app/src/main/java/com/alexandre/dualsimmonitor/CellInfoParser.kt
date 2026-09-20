package com.alexandre.dualsimmonitor

import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma

object CellInfoParser {
    fun parse(cell: CellInfo): CellSnapshot = when (cell) {
        is CellInfoLte -> {
            val signal = cell.cellSignalStrength
            CellSnapshot(
                technology = "4G / LTE",
                registered = cell.isRegistered,
                cell = cell.cellIdentity.ci.readable(),
                dbm = signal.dbm.readable(),
                asu = signal.asuLevel.readable(),
                rsrp = signal.rsrp.readable(),
                rsrq = signal.rsrq.readable(),
                sinr = signal.rssnr.readable()
            )
        }
        is CellInfoNr -> {
            val signal = cell.cellSignalStrength
            CellSnapshot(
                technology = "5G / NR",
                registered = cell.isRegistered,
                cell = cell.cellIdentity.toString(),
                dbm = signal.dbm.readable(),
                asu = signal.asuLevel.readable()
            )
        }
        is CellInfoWcdma -> CellSnapshot(
            technology = "3G / WCDMA",
            registered = cell.isRegistered,
            cell = cell.cellIdentity.cid.readable(),
            dbm = cell.cellSignalStrength.dbm.readable(),
            asu = cell.cellSignalStrength.asuLevel.readable()
        )
        is CellInfoGsm -> CellSnapshot(
            technology = "2G / GSM",
            registered = cell.isRegistered,
            cell = cell.cellIdentity.cid.readable(),
            dbm = cell.cellSignalStrength.dbm.readable(),
            asu = cell.cellSignalStrength.asuLevel.readable()
        )
        is CellInfoCdma -> CellSnapshot(
            technology = "CDMA",
            registered = cell.isRegistered,
            cell = cell.cellIdentity.basestationId.readable(),
            dbm = cell.cellSignalStrength.dbm.readable(),
            asu = cell.cellSignalStrength.asuLevel.readable()
        )
        else -> CellSnapshot(
            technology = "Desconhecida",
            registered = cell.isRegistered,
            cell = cell.toString()
        )
    }

    private fun Int.readable(): String =
        if (this == Int.MAX_VALUE || this < 0) "Indisponível" else toString()
}

data class CellSnapshot(
    val technology: String,
    val registered: Boolean,
    val cell: String,
    val dbm: String = "Indisponível",
    val asu: String = "Indisponível",
    val rsrp: String = "Indisponível",
    val rsrq: String = "Indisponível",
    val sinr: String = "Indisponível"
)
