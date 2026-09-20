package com.alexandre.dualsimmonitor

/** One local radio/network observation. Internet metrics are nullable for secondary SIMs. */
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
    val dataSubscriptionId: Int,
    val isDefaultDataSim: Boolean = subscriptionId == dataSubscriptionId
)
