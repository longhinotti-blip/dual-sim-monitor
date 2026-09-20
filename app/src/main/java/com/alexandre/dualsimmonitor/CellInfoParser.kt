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
            val i = cell.cellIdentity
            val s = cell.cellSignalStrength
            CellSnapshot("4G / LTE", cell.isRegistered, i.ci.readableUnsigned(), s.dbm.readableSigned(), s.asuLevel.readableUnsigned(), s.rsrp.readableSigned(), s.rsrq.readableSigned(), s.rssnr.readableSigned(), s.cqi.readableUnsigned(), s.timingAdvance.readableUnsigned(), i.pci.readableUnsigned(), i.tac.readableUnsigned(), i.earfcn.readableUnsigned())
        }
        is CellInfoNr -> {
            val i = cell.cellIdentity
            val s = cell.cellSignalStrength
            CellSnapshot("5G / NR", cell.isRegistered, read(i,"nci"), s.dbm.readableSigned(), s.asuLevel.readableUnsigned(), read(s,"ssRsrp"), read(s,"ssRsrq"), read(s,"ssSinr"), pci = read(i,"pci"), tac = read(i,"tac"), nrarfcn = read(i,"nrarfcn"), csiRsrp = read(s,"csiRsrp"), csiRsrq = read(s,"csiRsrq"), csiSinr = read(s,"csiSinr"))
        }
        is CellInfoWcdma -> CellSnapshot("3G / WCDMA", cell.isRegistered, cell.cellIdentity.cid.readableUnsigned(), cell.cellSignalStrength.dbm.readableSigned(), cell.cellSignalStrength.asuLevel.readableUnsigned(), rscp = read(cell.cellSignalStrength,"rscp"), ecNo = read(cell.cellSignalStrength,"ecNo"), lac = cell.cellIdentity.lac.readableUnsigned(), psc = cell.cellIdentity.psc.readableUnsigned())
        is CellInfoGsm -> CellSnapshot("2G / GSM", cell.isRegistered, cell.cellIdentity.cid.readableUnsigned(), cell.cellSignalStrength.dbm.readableSigned(), cell.cellSignalStrength.asuLevel.readableUnsigned(), lac = cell.cellIdentity.lac.readableUnsigned(), arfcn = cell.cellIdentity.arfcn.readableUnsigned(), bsic = cell.cellIdentity.bsic.readableUnsigned())
        is CellInfoCdma -> CellSnapshot("CDMA", cell.isRegistered, cell.cellIdentity.basestationId.readableUnsigned(), cell.cellSignalStrength.dbm.readableSigned(), cell.cellSignalStrength.asuLevel.readableUnsigned())
        else -> CellSnapshot("Desconhecida", cell.isRegistered, cell.toString())
    }
    fun diagnostic(cell: CellInfo): String = buildString {
        append("class=${cell.javaClass.name}; registered=${cell.isRegistered}; timestamp=${cell.timestampMillis}; ageMs=${System.currentTimeMillis()-cell.timestampMillis}; ")
        append(parse(cell))
    }
    private fun read(target: Any?, name: String): String {
        if (target == null) return "Indisponível"
        val method = target.javaClass.methods.firstOrNull { it.name == name || it.name == "get${name.replaceFirstChar { c -> c.uppercase() }}" } ?: return "Indisponível"
        return runCatching { method.invoke(target) }.getOrNull().let { value ->
            when (value) { null -> "Indisponível"; is Number -> if (value.toLong() == Int.MAX_VALUE.toLong() || value.toLong() == Int.MIN_VALUE.toLong() || value.toLong() < 0 && name in setOf("pci","tac","nrarfcn","nci")) "Indisponível" else value.toString(); else -> value.toString() }
        }
    }
    private fun Int?.readableSigned() = if (this == null || this == CellInfo.UNAVAILABLE || this == Int.MIN_VALUE) "Indisponível" else toString()
    private fun Int?.readableUnsigned() = if (this == null || this == CellInfo.UNAVAILABLE || this < 0) "Indisponível" else toString()
}

data class CellSnapshot(
    val technology: String, val registered: Boolean, val cell: String,
    val dbm: String = "Indisponível", val asu: String = "Indisponível", val rsrp: String = "Indisponível", val rsrq: String = "Indisponível", val sinr: String = "Indisponível", val cqi: String = "Indisponível", val timingAdvance: String = "Indisponível", val pci: String = "Indisponível", val tac: String = "Indisponível", val earfcn: String = "Indisponível", val rscp: String = "Indisponível", val ecNo: String = "Indisponível", val lac: String = "Indisponível", val psc: String = "Indisponível", val arfcn: String = "Indisponível", val bsic: String = "Indisponível", val nrarfcn: String = "Indisponível", val csiRsrp: String = "Indisponível", val csiRsrq: String = "Indisponível", val csiSinr: String = "Indisponível"
)
