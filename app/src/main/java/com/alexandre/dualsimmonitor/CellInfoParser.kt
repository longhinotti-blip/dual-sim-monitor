package com.alexandre.dualsimmonitor

import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthNr
import android.telephony.CellSignalStrengthWcdma

/** Converts platform CellInfo objects into values safe for display. */
object CellInfoParser {
    fun parse(cell: CellInfo): CellSnapshot = when (cell) {
        is CellInfoLte -> parseLte(cell)
        is CellInfoNr -> parseNr(cell)
        is CellInfoWcdma -> parseWcdma(cell)
        is CellInfoGsm -> parseGsm(cell)
        is CellInfoCdma -> parseCdma(cell)
        else -> CellSnapshot(
            technology = "Desconhecida",
            registered = cell.isRegistered,
            cell = cell.toString()
        )
    }

    private fun parseLte(cell: CellInfoLte): CellSnapshot {
        val identity = cell.cellIdentity
        val signal = cell.cellSignalStrength
        return CellSnapshot(
            technology = "4G / LTE",
            registered = cell.isRegistered,
            cell = identity.ci.readableUnsigned(),
            dbm = signal.dbm.readableSigned(),
            asu = signal.asuLevel.readableUnsigned(),
            rsrp = signal.rsrp.readableSigned(),
            rsrq = signal.rsrq.readableSigned(),
            sinr = signal.rssnr.readableSigned(),
            cqi = signal.cqi.readableUnsigned(),
            timingAdvance = signal.timingAdvance.readableUnsigned(),
            pci = identity.pci.readableUnsigned(),
            tac = identity.tac.readableUnsigned(),
            earfcn = identity.earfcn.readableUnsigned()
        )
    }

    private fun parseNr(cell: CellInfoNr): CellSnapshot {
        val identity = cell.cellIdentity as? CellIdentityNr
        val signal = cell.cellSignalStrength as? CellSignalStrengthNr
        return CellSnapshot(
            technology = "5G / NR",
            registered = cell.isRegistered,
            cell = identity?.nci?.readableUnsigned() ?: "Indisponível",
            dbm = signal?.dbm.readableSigned() ?: "Indisponível",
            asu = signal?.asuLevel.readableUnsigned() ?: "Indisponível",
            rsrp = signal?.ssRsrp.readableSigned() ?: "Indisponível",
            rsrq = signal?.ssRsrq.readableSigned() ?: "Indisponível",
            sinr = signal?.ssSinr.readableSigned() ?: "Indisponível",
            csiRsrp = signal?.csiRsrp.readableSigned() ?: "Indisponível",
            csiRsrq = signal?.csiRsrq.readableSigned() ?: "Indisponível",
            csiSinr = signal?.csiSinr.readableSigned() ?: "Indisponível",
            pci = identity?.pci.readableUnsigned() ?: "Indisponível",
            tac = identity?.tac.readableUnsigned() ?: "Indisponível",
            nrarfcn = identity?.nrarfcn.readableUnsigned() ?: "Indisponível"
        )
    }

    private fun parseWcdma(cell: CellInfoWcdma): CellSnapshot {
        val identity = cell.cellIdentity
        val signal = cell.cellSignalStrength as? CellSignalStrengthWcdma
        return CellSnapshot(
            technology = "3G / WCDMA",
            registered = cell.isRegistered,
            cell = identity.cid.readableUnsigned(),
            dbm = signal?.dbm.readableSigned() ?: "Indisponível",
            asu = signal?.asuLevel.readableUnsigned() ?: "Indisponível",
            rscp = signal?.rscp.readableSigned() ?: "Indisponível",
            ecNo = signal?.ecNo.readableSigned() ?: "Indisponível",
            lac = identity.lac.readableUnsigned(),
            psc = identity.psc.readableUnsigned()
        )
    }

    private fun parseGsm(cell: CellInfoGsm): CellSnapshot {
        val identity = cell.cellIdentity
        val signal = cell.cellSignalStrength
        return CellSnapshot(
            technology = "2G / GSM",
            registered = cell.isRegistered,
            cell = identity.cid.readableUnsigned(),
            dbm = signal.dbm.readableSigned(),
            asu = signal.asuLevel.readableUnsigned(),
            lac = identity.lac.readableUnsigned(),
            arfcn = identity.arfcn.readableUnsigned(),
            bsic = identity.bsic.readableUnsigned()
        )
    }

    private fun parseCdma(cell: CellInfoCdma): CellSnapshot {
        val signal = cell.cellSignalStrength
        return CellSnapshot(
            technology = "CDMA",
            registered = cell.isRegistered,
            cell = cell.cellIdentity.basestationId.readableUnsigned(),
            dbm = signal.dbm.readableSigned(),
            asu = signal.asuLevel.readableUnsigned()
        )
    }

    private fun Int?.readableSigned(): String =
        if (this == null || this == Int.MAX_VALUE || this == Int.MIN_VALUE) "Indisponível" else toString()

    private fun Int?.readableUnsigned(): String =
        if (this == null || this == Int.MAX_VALUE || this < 0) "Indisponível" else toString()
}

data class CellSnapshot(
    val technology: String,
    val registered: Boolean,
    val cell: String,
    val dbm: String = "Indisponível",
    val asu: String = "Indisponível",
    val rsrp: String = "Indisponível",
    val rsrq: String = "Indisponível",
    val sinr: String = "Indisponível",
    val cqi: String = "Indisponível",
    val timingAdvance: String = "Indisponível",
    val pci: String = "Indisponível",
    val tac: String = "Indisponível",
    val earfcn: String = "Indisponível",
    val rscp: String = "Indisponível",
    val ecNo: String = "Indisponível",
    val lac: String = "Indisponível",
    val psc: String = "Indisponível",
    val arfcn: String = "Indisponível",
    val bsic: String = "Indisponível",
    val nrarfcn: String = "Indisponível",
    val csiRsrp: String = "Indisponível",
    val csiRsrq: String = "Indisponível",
    val csiSinr: String = "Indisponível"
)
