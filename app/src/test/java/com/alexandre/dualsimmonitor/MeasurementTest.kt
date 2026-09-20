package com.alexandre.dualsimmonitor

import org.junit.Assert.assertEquals
import org.junit.Test

class MeasurementTest {
    @Test fun unavailableValuesRemainText() { assertEquals("Indisponível", CellSnapshot("LTE", true, "1").rsrp) }
    @Test fun measurementKeepsBothSubscriptions() { assertEquals(3, Measurement(1, 3, 1, "VIVO", "-80", "-100", "-10", "5", "LTE", "1", dataSubscriptionId = 3).subscriptionId) }
}
