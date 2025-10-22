package com.viniss.todo.api.param

import com.viniss.todo.domain.Status

object StatusParamMapper {
  /**
   * Aceita:
   *  - "all"               -> null (sem filtro)
   *  - "completed"         -> { DONE }
   *  - "pending"           -> { OPEN, IN_PROGRESS, BLOCKED }
   *  - "OPEN,IN_PROGRESS"  -> CSV de enums
   */
  fun from(raw: String?): Set<Status>? {
    if (raw.isNullOrBlank() || raw.equals("all", true)) return null
    return when (raw.lowercase()) {
      "completed" -> setOf(Status.DONE)
      "pending"   -> setOf(Status.OPEN, Status.IN_PROGRESS, Status.BLOCKED)
      else -> raw.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull {
          try { Status.valueOf(it) } catch (_: Exception) { null }
        }
        .toSet()
        .ifEmpty { null }
    }
  }
}
