package com.alexandre.dualsimmonitor

import android.telephony.CellInfo
import android.util.Log

fun CellInfo.debugDescription(): String = "class=${javaClass.name}, registered=$isRegistered, timestamp=${timestampMillis}, age=${System.currentTimeMillis() - timestampMillis}ms"
fun logCellInfo(cells: List<CellInfo>) { cells.forEach { Log.d("DualSimMonitor", it.debugDescription()) } }
