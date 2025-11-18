package com.viniss.todo.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class NoHtmlValidator : ConstraintValidator<NoHtml, String?> {

    private val htmlPatterns = listOf(
        Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE),
        Regex("<[^>]+on\\w+\\s*=", RegexOption.IGNORE_CASE), // Event handlers like onclick, onerror
        Regex("<iframe[^>]*>", RegexOption.IGNORE_CASE),
        Regex("<object[^>]*>", RegexOption.IGNORE_CASE),
        Regex("<embed[^>]*>", RegexOption.IGNORE_CASE),
        Regex("javascript:", RegexOption.IGNORE_CASE),
        Regex("<img[^>]*>", RegexOption.IGNORE_CASE),
        Regex("<style[^>]*>.*?</style>", RegexOption.IGNORE_CASE)
    )

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) return true

        return htmlPatterns.none { pattern ->
            pattern.containsMatchIn(value)
        }
    }
}
