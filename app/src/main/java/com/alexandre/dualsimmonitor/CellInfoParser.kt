package com.alexandre.dualsimmonitor

import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma

/**
 * Converts platform CellInfo objects into values safe for display.
 *
 * This implementation intentionally avoids direct access to optional radio members that are
 * not consistently exposed by OEM/Android builds (for example on Xiaomi / Android 16).
 * Where a property is missing, we fall back to "Indisponível" instead of crashing the build.
 */
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
        val identity = cell.cellIdentity
        val signal = cell.cellSignalStrength
        return CellSnapshot(
            technology = "5G / NR",
            registered = cell.isRegistered,
            cell = readValue(identity, listOf("nci"), "Indisponível"),
            dbm = signal.dbm.readableSigned(),
            asu = signal.asuLevel.readableUnsigned(),
            rsrp = readValue(signal, listOf("ssRsrp", "ssRsrpDbm", "csiRsrp"), "Indisponível"),
            rsrq = readValue(signal, listOf("ssRsrq", "csiRsrq"), "Indisponível"),
            sinr = readValue(signal, listOf("ssSinr", "csiSinr"), "Indisponível"),
            pci = readValue(identity, listOf("pci"), "Indisponível"),
            tac = readValue(identity, listOf("tac"), "Indisponível"),
            nrarfcn = readValue(identity, listOf("nrarfcn"), "Indisponível"),
            csiRsrp = readValue(signal, listOf("csiRsrp"), "Indisponível"),
            csiRsrq = readValue(signal, listOf("csiRsrq"), "Indisponível"),
            csiSinr = readValue(signal, listOf("csiSinr"), "Indisponível")
        )
    }

    private fun parseWcdma(cell: CellInfoWcdma): CellSnapshot {
        val identity = cell.cellIdentity
        val signal = cell.cellSignalStrength
        return CellSnapshot(
            technology = "3G / WCDMA",
            registered = cell.isRegistered,
            cell = identity.cid.readableUnsigned(),
            dbm = signal.dbm.readableSigned(),
            asu = signal.asuLevel.readableUnsigned(),
            rscp = readValue(signal, listOf("rscp"), "Indisponível"),
            ecNo = readValue(signal, listOf("ecNo"), "Indisponível"),
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

    private fun readValue(target: Any?, candidates: List<String>, fallback: String): String {
        if (target == null) return fallback

        val methods = target.javaClass.methods
        for (candidate in candidates) {
            val getter = methods.firstOrNull { it.name == candidate || it.name == candidate.toGetterName() }
            if (getter != null) {
                return try {
                    val value = getter.invoke(target)
                    when (value) {
                        null -> fallback
                        is Int -> value.toReadableString()
                        is Number -> value.toString()
                        else -> value.toString()
                    }
                } catch (_: Throwable) {
                    fallback
                }
            }
        }

        return fallback
    }

    private fun String.toGetterName(): String {
        if (isEmpty()) return ""
        return "get${this[0].uppercaseChar()}${substring(1)}"
    }

    private fun Int?.readableSigned(): String =
        if (this == null || this == Int.MAX_VALUE || this == Int.MIN_VALUE) "Indisponível" else toString()

    private fun Int?.readableUnsigned(): String =
        if (this == null || this == Int.MAX_VALUE || this < 0) "Indisponível" else toString()

    private fun Int.toReadableString(): String =
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
