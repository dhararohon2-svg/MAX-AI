package com.maxai.gamingsuite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCapabilityEngineTest {
    @Test
    fun unavailableFrequencyIsExplicit() {
        assertEquals("Not exposed", DeviceCapabilityEngine.formatMHz(null))
    }

    @Test
    fun formatsFrequencyInMhz() {
        assertEquals("1234 MHz", DeviceCapabilityEngine.formatMHz(1234.0))
    }

    @Test
    fun gamingProfileDescriptionsAreGuidanceNotClockClaims() {
        assertTrue(profileDescription("Extreme Performance").contains("native performance/game mode"))
        assertTrue(profileDescription("Ultra Battery Saver").contains("lower graphics"))
    }
}
