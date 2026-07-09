package com.smithware.tidypilot.data

data class PremiumPlan(
    val id: String,
    val name: String,
    val priceLabel: String,
    val valueLabel: String
)

data class PremiumFeature(
    val name: String,
    val freeIncluded: Boolean,
    val premiumIncluded: Boolean,
    val helpText: String
)

val premiumPlans = listOf(
    PremiumPlan("monthly", "Monthly", "$3.99/mo", "Best for trying the advanced planner."),
    PremiumPlan("yearly", "Yearly", "$29.99/yr", "Lower yearly cost for ongoing home routines."),
    PremiumPlan("lifetime", "Lifetime", "$69.99 once", "One-time support for long-term local-first use.")
)

val premiumFeatures = listOf(
    PremiumFeature("Add rooms", true, true, "Core home setup stays free."),
    PremiumFeature("Add custom tasks", true, true, "Basic cleaning should never be paywalled."),
    PremiumFeature("Basic recurring schedules", true, true, "Daily and weekly chores remain free."),
    PremiumFeature("Basic reminders", true, true, "Gentle local nudges stay available."),
    PremiumFeature("Today dashboard", true, true, "The main answer to what needs cleaning today."),
    PremiumFeature("One Thing mode", true, true, "Overwhelm support stays free."),
    PremiumFeature("Starter chore library", true, true, "Users should not invent every chore."),
    PremiumFeature("Basic dirtiness scoring", true, true, "Need-based recommendations are the core promise."),
    PremiumFeature("Unlimited advanced plans", false, true, "Premium can expand guided routines without crowding the free app."),
    PremiumFeature("AI home setup wizard", false, true, "Future smart setup can save time for complex homes."),
    PremiumFeature("Household sharing", false, true, "Premium can support family assignment and coordination."),
    PremiumFeature("Advanced chore rotation", false, true, "Fair turns and rotations are household power tools."),
    PremiumFeature("Widgets", false, true, "Home screen widgets keep the plan visible without opening the app."),
    PremiumFeature("Supplies and budget tracking", false, true, "Premium can add deeper shopping, cost, and budget insights."),
    PremiumFeature("Seasonal and deep-clean plans", false, true, "Bigger plans can be premium while daily chores stay free."),
    PremiumFeature("Backup and export", false, true, "Advanced data control can be premium later."),
    PremiumFeature("Themes", false, true, "Visual customization is nice-to-have, not core cleaning."),
    PremiumFeature("Advanced stats", false, true, "Deeper trends help power users tune routines."),
    PremiumFeature("Smart recommendation tuning", false, true, "Premium can fine-tune scoring without blocking basic scoring.")
)

fun hasMockPremium(settings: AppSettingsEntity): Boolean =
    settings.premiumEntitlement == "premium" || settings.premiumPlan == "lifetime"

