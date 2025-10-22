package com.viniss.todo.api.dto

enum class DueFilter { ALL, TODAY, WEEK, OVERDUE;
    companion object {
        fun fromParam(raw: String?): DueFilter =
            when (raw?.lowercase()) {
                null, "", "all" -> ALL
                "today" -> TODAY
                "week", "thisweek" -> WEEK
                "overdue", "late" -> OVERDUE
                else -> ALL
            }
    }
}
