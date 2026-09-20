package com.alexandre.dualsimmonitor

import android.telephony.CellInfo
import android.util.Log

object CellInfoDebug {
    private const val TAG = "DualSimMonitor.CellInfo"
    fun request(subscriptionId: Int, slot: Int) = Log.d(TAG, "CELL_INFO_REQUEST subscriptionId=$subscriptionId slot=$slot")
    fun callback(subscriptionId: Int, cells: List<CellInfo>) {
        Log.d(TAG, "CELL_INFO_CALLBACK subscriptionId=$subscriptionId count=${cells.size}")
        cells.forEach { Log.d(TAG, "${CellInfoParser.diagnostic(it)}") }
    }
}
