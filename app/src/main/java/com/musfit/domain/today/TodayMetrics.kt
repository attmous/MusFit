package com.musfit.domain.today

/** The pool of pinnable Today-carousel metrics. [id] is the persisted identity. */
enum class TodayMetric(val id: String, val label: String) {
    Calories("calories", "Calories"),
    Protein("protein", "Protein"),
    Carbs("carbs", "Carbs"),
    Fat("fat", "Fat"),
    Water("water", "Water"),
    Steps("steps", "Steps"),
    Weight("weight", "Weight"),
    BodyFat("body_fat", "Body fat"),
    Sessions("sessions", "Sessions"),
    ActiveCalories("active_calories", "Active kcal"),
    RestingHeartRate("resting_hr", "Resting HR"),
    CalorieBalance("calorie_balance", "Balance"),
    LoggingStreak("streak", "Streak"),
    ;

    companion object {
        fun fromId(id: String): TodayMetric? = entries.firstOrNull { it.id == id }

        /** Fresh-install / defensive-fallback pins; also seeded by migration 25→26. */
        val DEFAULT_PINS = listOf(Calories, Steps, Protein)
    }
}
