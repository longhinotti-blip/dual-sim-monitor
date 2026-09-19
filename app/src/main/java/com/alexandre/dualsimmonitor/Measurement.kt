package com.alexandre.dualsimmonitor

/** Future local measurement record. No database or network storage is used in v0.1. */
data class Measurement(
    val timestampMillis: Long,
    val subscriptionId: Int,
    val slot: Int,
    val carrier: String,
    val dbm: String,
    val rsrp: String,
    val rsrq: String,
    val sinr: String,
    val technology: String,
    val cell: String,
    val latencyMillis: Long? = null,
    val packetLossPercent: Float? = null,
    val dataSubscriptionId: Int
)
