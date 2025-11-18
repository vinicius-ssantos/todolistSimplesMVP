package com.viniss.todo.domain

/**
 * Enum representing the frequency of task recurrence.
 */
enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

/**
 * Enum representing days of the week for weekly recurrence.
 */
enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}

/**
 * Value object representing a recurrence pattern for tasks.
 * Stored as JSON in the database.
 */
data class RecurrencePattern(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1, // Repeat every N days/weeks/months/years
    val daysOfWeek: List<DayOfWeek>? = null, // For weekly recurrence
    val dayOfMonth: Int? = null, // For monthly recurrence (1-31)
    val monthOfYear: Int? = null, // For yearly recurrence (1-12)
    val endDate: java.time.LocalDate? = null, // When to stop recurring
    val occurrences: Int? = null // Alternative to endDate: stop after N occurrences
) {
    init {
        require(interval > 0) { "Interval must be positive" }
        dayOfMonth?.let { require(it in 1..31) { "Day of month must be between 1 and 31" } }
        monthOfYear?.let { require(it in 1..12) { "Month of year must be between 1 and 12" } }
        occurrences?.let { require(it > 0) { "Occurrences must be positive" } }
    }

    companion object {
        /**
         * Creates a daily recurrence pattern.
         */
        fun daily(interval: Int = 1, endDate: java.time.LocalDate? = null): RecurrencePattern =
            RecurrencePattern(RecurrenceFrequency.DAILY, interval, endDate = endDate)

        /**
         * Creates a weekly recurrence pattern.
         */
        fun weekly(
            interval: Int = 1,
            daysOfWeek: List<DayOfWeek>,
            endDate: java.time.LocalDate? = null
        ): RecurrencePattern =
            RecurrencePattern(RecurrenceFrequency.WEEKLY, interval, daysOfWeek, endDate = endDate)

        /**
         * Creates a monthly recurrence pattern.
         */
        fun monthly(
            interval: Int = 1,
            dayOfMonth: Int,
            endDate: java.time.LocalDate? = null
        ): RecurrencePattern =
            RecurrencePattern(RecurrenceFrequency.MONTHLY, interval, dayOfMonth = dayOfMonth, endDate = endDate)

        /**
         * Creates a yearly recurrence pattern.
         */
        fun yearly(
            interval: Int = 1,
            dayOfMonth: Int,
            monthOfYear: Int,
            endDate: java.time.LocalDate? = null
        ): RecurrencePattern =
            RecurrencePattern(
                RecurrenceFrequency.YEARLY,
                interval,
                dayOfMonth = dayOfMonth,
                monthOfYear = monthOfYear,
                endDate = endDate
            )
    }
}
