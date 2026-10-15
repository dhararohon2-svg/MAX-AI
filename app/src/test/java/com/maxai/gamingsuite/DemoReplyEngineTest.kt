package com.maxai.gamingsuite

import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleProfileTest {
    @Test
    fun performanceProfileWarnsAboutHeat() {
        assertTrue(profileDescription("Extreme Performance").contains("heat"))
    }

    @Test
    fun batteryProfileRecommendsLowerSettings() {
        assertTrue(profileDescription("Ultra Battery Saver").contains("brightness"))
    }

    @Test
    fun balancedProfileHasSafeGuidance() {
        assertTrue(profileDescription("Smart Balanced").contains("monitor heat"))
    }
}
