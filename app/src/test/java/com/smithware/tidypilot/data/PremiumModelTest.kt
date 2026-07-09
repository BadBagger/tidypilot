package com.smithware.tidypilot.data

import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumModelTest {
    @Test
    fun freeVersionKeepsCoreCleaningUseful() {
        val freeEssentials = setOf(
            "Add rooms",
            "Add custom tasks",
            "Basic recurring schedules",
            "Basic reminders",
            "Today dashboard",
            "One Thing mode",
            "Starter chore library",
            "Basic dirtiness scoring"
        )

        val freeIncluded = premiumFeatures.filter { it.freeIncluded }.map { it.name }.toSet()

        assertTrue(freeIncluded.containsAll(freeEssentials))
    }

    @Test
    fun mockLifetimeCountsAsPremium() {
        val settings = AppSettingsEntity(premiumPlan = "lifetime")

        assertTrue(hasMockPremium(settings))
    }
}
