package com.viniss.todo.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * Comprehensive unit tests for PasswordValidator.
 *
 * Uses @ParameterizedTest to efficiently test multiple password scenarios
 * with minimal test code while achieving high coverage.
 *
 * Covers:
 * - Password length validation
 * - Uppercase/lowercase requirements
 * - Digit requirements
 * - Special character requirements
 * - Weak pattern detection
 * - Sequential character detection
 */
class PasswordValidatorTest {

    private lateinit var validator: PasswordValidator
    private lateinit var props: PasswordProps

    @BeforeEach
    fun setUp() {
        // Default configuration (matching application.yml defaults)
        props = PasswordProps(
            minLength = 12,
            requireUppercase = true,
            requireLowercase = true,
            requireDigit = true,
            requireSpecialChar = true,
            specialChars = "!@#\$%^&*()_+-=[]{}|;:,.<>?"
        )
        validator = PasswordValidator(props)
    }

    // ========================================
    // VALID PASSWORD TESTS
    // ========================================

    @ParameterizedTest
    @ValueSource(strings = [
        "SecureP@ss2024",
        "MyP@ssw0rd123",
        "C0mpl3x!Pass",
        "Str0ng#Passw0rd",
        "V@lid1234Pass",
        "Test!ng2024Secure",
        "Qu!ck#Fox123",
        "P@ssw0rd$ecure"
    ])
    fun `should accept valid passwords`(password: String) {
        val result = validator.validate(password)

        assertThat(result.isValid()).isTrue()
        assertThat(result.errorList()).isEmpty()
    }

    // ========================================
    // LENGTH VALIDATION TESTS
    // ========================================

    @ParameterizedTest
    @CsvSource(
        "short, false",
        "Short1!, false",
        "A!1, false",
        "1234567890, false",
        "Ab!12345678, false",     // 11 chars (1 short)
        "Ab!123456789, true",     // 12 chars (minimum)
        "Ab!1234567890, true"     // 13 chars
    )
    fun `should validate minimum length`(password: String, shouldBeValid: Boolean) {
        val result = validator.validate(password)

        if (shouldBeValid) {
            assertThat(result.errorList())
                .noneMatch { it.contains("at least 12 characters") }
        } else {
            assertThat(result.errorList())
                .anyMatch { it.contains("at least 12 characters") }
        }
    }

    @Test
    fun `should reject password shorter than minimum length`() {
        val password = "Short1!"
        val result = validator.validate(password)

        assertThat(result.isValid()).isFalse()
        assertThat(result.errorList())
            .anyMatch { it.contains("Password must be at least 12 characters long") }
    }

    // ========================================
    // UPPERCASE VALIDATION TESTS
    // ========================================

    @ParameterizedTest
    @ValueSource(strings = [
        "nouppercasepassword123!",
        "lowercase@only2024",
        "test!ng123secure",
        "p@ssw0rd$ecure123"
    ])
    fun `should reject passwords without uppercase letter`(password: String) {
        val result = validator.validate(password)

        assertThat(result.isValid()).isFalse()
        assertThat(result.errorList())
            .anyMatch { it.contains("must contain at least one uppercase letter") }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "Atleast1Upper!",
        "P@ssw0rdSecure",
        "MULTIPLE123!Upper"
    ])
    fun `should accept passwords with uppercase letters`(password: String) {
        val result = validator.validate(password)

        assertThat(result.errorList())
            .noneMatch { it.contains("uppercase") }
    }

    // ========================================
    // LOWERCASE VALIDATION TESTS
    // ========================================

    @ParameterizedTest
    @ValueSource(strings = [
        "NOLOWERCASE123!",
        "UPPERCASE@ONLY2024",
        "TEST!NG123SECURE",
        "P@SSW0RD$ECURE123"
    ])
    fun `should reject passwords without lowercase letter`(password: String) {
        val result = validator.validate(password)

        assertThat(result.isValid()).isFalse()
        assertThat(result.errorList())
            .anyMatch { it.contains("must contain at least one lowercase letter") }
    }

    // ========================================
    // DIGIT VALIDATION TESTS
    // ========================================

    @ParameterizedTest
    @ValueSource(strings = [
        "NoDigitsPassword!",
        "Secure@Password",
        "Test!ngSecure",
        "P@sswordSecure"
    ])
    fun `should reject passwords without digits`(password: String) {
        val result = validator.validate(password)

        assertThat(result.isValid()).isFalse()
        assertThat(result.errorList())
            .anyMatch { it.contains("must contain at least one digit") }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "Atleast1Digit!",
        "P@ssw0rdSecure",
        "Multiple123!Digits"
    ])
    fun `should accept passwords with digits`(password: String) {
        val result = validator.validate(password)

        assertThat(result.errorList())
            .noneMatch { it.contains("digit") }
    }

    // ========================================
    // SPECIAL CHARACTER VALIDATION TESTS
    // ========================================

    @ParameterizedTest
    @ValueSource(strings = [
        "NoSpecialChar123",
        "SecurePassword2024",
        "Testing123Secure",
        "Passw0rdSecure1234"
    ])
    fun `should reject passwords without special characters`(password: String) {
        val result = validator.validate(password)

        assertThat(result.isValid()).isFalse()
        assertThat(result.errorList())
            .anyMatch { it.contains("must contain at least one special character") }
    }

    @ParameterizedTest
    @CsvSource(
        "Atleast1Special!, !",
        "P@ssw0rdSecure, @",
        "Multiple#123$Chars, #,$",
        "Test[Brackets]1, [,]",
        "Semi;Colon:1Pass, ;,:",
        "Question?Mark1Pass, ?",
        "Percent%Sign1Pass, %"
    )
    fun `should accept passwords with various special characters`(password: String, specialChars: String) {
        val result = validator.validate(password)

        assertThat(result.errorList())
            .noneMatch { it.contains("special character") }
    }

    // ========================================
    // WEAK PATTERN DETECTION TESTS
    // ========================================

    @ParameterizedTest
    @ValueSource(strings = [
        "Password123!",
        "MyPassword1!",
        "12345SecureP@ss",
        "Qwerty123!Secure",
        "Abc123Secure!Pass",
        "Letmein123!Secure",
        "Welcome123!Secure",
        "Monkey123!Secure",
        "Dragon123!Secure",
        "Master123!Secure",
        "Sunshine1!Secure"
    ])
    fun `should detect common weak patterns`(password: String) {
        val result = validator.validate(password)

        assertThat(result.isValid()).isFalse()
        assertThat(result.errorList())
            .anyMatch { it.contains("contains common weak patterns") }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "SecureP@ss2024",
        "MyStr0ng!Pass",
        "C0mpl3x#Secure",
        "V@lid1234Test"
    ])
    fun `should accept passwords without weak patterns`(password: String) {
        val result = validator.validate(password)

        assertThat(result.errorList())
            .noneMatch { it.contains("weak patterns") }
    }

    // ========================================
    // SEQUENTIAL CHARACTER DETECTION TESTS
    // ========================================

    @ParameterizedTest
    @ValueSource(strings = [
        "Abc123456789!",      // "abc" sequential
        "Test123456!Pass",    // "123" sequential
        "SecureAbc!Pass1",    // "abc" sequential
        "Xyz987654!Pass",     // "xyz" sequential
        "Pass321!Secure1",    // "321" descending
        "TestCba!123Pass",    // "cba" descending
        "Secure!987Pass1"     // "987" descending
    ])
    fun `should detect sequential characters`(password: String) {
        val result = validator.validate(password)

        assertThat(result.isValid()).isFalse()
        assertThat(result.errorList())
            .anyMatch { it.contains("contains sequential characters") }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "SecureP@ss2024",    // No sequences
        "My!Str0ng#Pass",    // No sequences
        "C0mpl3x$Secure",    // No sequences
        "V@lid#Test1492"     // Non-sequential numbers
    ])
    fun `should accept passwords without sequential characters`(password: String) {
        val result = validator.validate(password)

        assertThat(result.errorList())
            .noneMatch { it.contains("sequential") }
    }

    @Test
    fun `should detect ascending sequential characters`() {
        val password = "TestAbc123!Pass"
        val result = validator.validate(password)

        assertThat(result.isValid()).isFalse()
        assertThat(result.errorList())
            .anyMatch { it.contains("sequential characters") }
    }

    @Test
    fun `should detect descending sequential characters`() {
        val password = "TestCba321!Pass"
        val result = validator.validate(password)

        assertThat(result.isValid()).isFalse()
        assertThat(result.errorList())
            .anyMatch { it.contains("sequential characters") }
    }

    // ========================================
    // MULTIPLE ERROR SCENARIOS
    // ========================================

    @Test
    fun `should return multiple errors for severely weak password`() {
        val password = "short"
        val result = validator.validate(password)

        assertThat(result.isValid()).isFalse()
        assertThat(result.errorList().size).isGreaterThanOrEqualTo(3)
    }

    @ParameterizedTest
    @CsvSource(
        "weak, 4",           // Very weak - missing everything
        "WeakPass, 2",       // Missing digit and special char
        "weak123, 2",        // Missing uppercase and special char
        "WEAK123!, 1"        // Missing only lowercase
    )
    fun `should return correct number of errors`(password: String, expectedMinErrors: Int) {
        val result = validator.validate(password)

        assertThat(result.isValid()).isFalse()
        assertThat(result.errorList().size).isGreaterThanOrEqualTo(expectedMinErrors)
    }

    // ========================================
    // CONFIGURATION TESTS
    // ========================================

    @Test
    fun `should respect custom minimum length`() {
        val customProps = PasswordProps(
            minLength = 8,
            requireUppercase = true,
            requireLowercase = true,
            requireDigit = true,
            requireSpecialChar = true
        )
        val customValidator = PasswordValidator(customProps)

        val password = "Short1!" // 7 chars
        val result = customValidator.validate(password)

        assertThat(result.isValid()).isFalse()
        assertThat(result.errorList())
            .anyMatch { it.contains("at least 8 characters") }
    }

    @Test
    fun `should allow disabling uppercase requirement`() {
        val customProps = PasswordProps(
            minLength = 12,
            requireUppercase = false,
            requireLowercase = true,
            requireDigit = true,
            requireSpecialChar = true
        )
        val customValidator = PasswordValidator(customProps)

        val password = "lowercase123!pass"
        val result = customValidator.validate(password)

        assertThat(result.errorList())
            .noneMatch { it.contains("uppercase") }
    }

    @Test
    fun `should allow disabling special character requirement`() {
        val customProps = PasswordProps(
            minLength = 12,
            requireUppercase = true,
            requireLowercase = true,
            requireDigit = true,
            requireSpecialChar = false
        )
        val customValidator = PasswordValidator(customProps)

        val password = "NoSpecialChar123"
        val result = customValidator.validate(password)

        assertThat(result.errorList())
            .noneMatch { it.contains("special character") }
    }

    // ========================================
    // REQUIREMENTS DESCRIPTION TEST
    // ========================================

    @Test
    fun `should provide human-readable requirements description`() {
        val description = validator.getRequirementsDescription()

        assertThat(description).contains("12 characters")
        assertThat(description).contains("uppercase")
        assertThat(description).contains("lowercase")
        assertThat(description).contains("digit")
        assertThat(description).contains("special character")
    }

    @Test
    fun `requirements description should reflect custom configuration`() {
        val customProps = PasswordProps(
            minLength = 8,
            requireUppercase = false,
            requireLowercase = true,
            requireDigit = false,
            requireSpecialChar = true
        )
        val customValidator = PasswordValidator(customProps)

        val description = customValidator.getRequirementsDescription()

        assertThat(description).contains("8 characters")
        assertThat(description).doesNotContain("uppercase")
        assertThat(description).contains("lowercase")
        assertThat(description).doesNotContain("digit")
        assertThat(description).contains("special character")
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    fun `should handle empty password`() {
        val result = validator.validate("")

        assertThat(result.isValid()).isFalse()
        assertThat(result.errorList()).isNotEmpty()
    }

    @Test
    fun `should handle very long password`() {
        val longPassword = "A".repeat(50) + "b1!"
        val result = validator.validate(longPassword)

        // Should be valid if meets other requirements
        assertThat(result.errorList())
            .noneMatch { it.contains("length") }
    }

    @Test
    fun `should handle password with only special characters`() {
        val password = "!!!!!!!!!!!!!"
        val result = validator.validate(password)

        assertThat(result.isValid()).isFalse()
        assertThat(result.errorList().size).isGreaterThan(0)
    }

    @Test
    fun `should handle password with unicode characters`() {
        val password = "Passw0rd!§©®™"
        val result = validator.validate(password)

        // Should validate based on standard requirements
        assertThat(result).isNotNull()
    }
}
