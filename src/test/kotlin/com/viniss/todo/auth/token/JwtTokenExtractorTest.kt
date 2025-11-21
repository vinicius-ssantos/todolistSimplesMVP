package com.viniss.todo.auth.token

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant
import java.util.*

/**
 * Comprehensive unit tests for JwtTokenExtractor.
 *
 * Tests JWT parsing and extraction without full validation,
 * covering valid tokens, malformed tokens, and edge cases.
 *
 * Uses @ParameterizedTest for efficient coverage of multiple scenarios.
 */
class JwtTokenExtractorTest {

    private lateinit var extractor: JwtTokenExtractor

    @BeforeEach
    fun setUp() {
        extractor = JwtTokenExtractor()
    }

    // ========================================
    // TOKEN STRUCTURE VALIDATION TESTS
    // ========================================

    @ParameterizedTest
    @CsvSource(
        "header.payload.signature, true",
        "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.sig, true",
        "part1.part2.part3, true",
        "a.b.c, true"
    )
    fun `should validate tokens with 3 parts as valid structure`(token: String, expectedValid: Boolean) {
        val result = extractor.hasValidStructure(token)

        assertThat(result).isEqualTo(expectedValid)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "invalid",
        "only.two",
        "too.many.parts.here",
        "",
        ".",
        "..",
        "...",
        "one.two.three.four.five"
    ])
    fun `should reject tokens with invalid structure`(token: String) {
        val result = extractor.hasValidStructure(token)

        assertThat(result).isFalse()
    }

    // ========================================
    // JTI EXTRACTION TESTS
    // ========================================

    @Test
    fun `should extract JTI from valid token`() {
        // Token with JTI in payload: {"jti":"test-jti-123"}
        val payload = """{"jti":"test-jti-123"}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val jti = extractor.extractJti(token)

        assertThat(jti).isEqualTo("test-jti-123")
    }

    @Test
    fun `should extract JTI from token with multiple claims`() {
        // Token with multiple claims including JTI
        val payload = """{"sub":"user@example.com","jti":"abc-123-xyz","exp":1234567890,"iat":1234567000}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val jti = extractor.extractJti(token)

        assertThat(jti).isEqualTo("abc-123-xyz")
    }

    @Test
    fun `should extract JTI with UUID format`() {
        val expectedJti = UUID.randomUUID().toString()
        val payload = """{"jti":"$expectedJti"}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val jti = extractor.extractJti(token)

        assertThat(jti).isEqualTo(expectedJti)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "invalid-token",
        "only.two",
        "",
        "not-a-jwt"
    ])
    fun `should return null for invalid token structure when extracting JTI`(token: String) {
        val jti = extractor.extractJti(token)

        assertThat(jti).isNull()
    }

    @Test
    fun `should return null when JTI is missing from payload`() {
        val payload = """{"sub":"user@example.com","exp":1234567890}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val jti = extractor.extractJti(token)

        assertThat(jti).isNull()
    }

    @Test
    fun `should return null for malformed base64 payload when extracting JTI`() {
        val token = "header.not-valid-base64!@#$.signature"

        val jti = extractor.extractJti(token)

        assertThat(jti).isNull()
    }

    // ========================================
    // EXPIRATION EXTRACTION TESTS
    // ========================================

    @Test
    fun `should extract expiration from valid token`() {
        val expectedExp = 1234567890L
        val payload = """{"exp":$expectedExp}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val expiration = extractor.extractExpiration(token)

        assertThat(expiration).isEqualTo(Instant.ofEpochSecond(expectedExp))
    }

    @Test
    fun `should extract expiration from token with multiple claims`() {
        val expectedExp = 9999999999L
        val payload = """{"sub":"user@example.com","jti":"abc-123","exp":$expectedExp,"iat":1234567000}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val expiration = extractor.extractExpiration(token)

        assertThat(expiration).isEqualTo(Instant.ofEpochSecond(expectedExp))
    }

    @Test
    fun `should return default expiration when exp claim is missing`() {
        val payload = """{"sub":"user@example.com","jti":"abc-123"}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val expiration = extractor.extractExpiration(token)

        // Should return default (15 minutes from now)
        assertThat(expiration).isAfter(Instant.now())
        assertThat(expiration).isBefore(Instant.now().plusSeconds(1000))
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "invalid-token",
        "only.two",
        ""
    ])
    fun `should return default expiration for invalid token structure`(token: String) {
        val expiration = extractor.extractExpiration(token)

        // Should return default expiration (15 minutes from now)
        assertThat(expiration).isAfter(Instant.now())
        assertThat(expiration).isBefore(Instant.now().plusSeconds(1000))
    }

    @Test
    fun `should return default expiration for malformed base64 payload`() {
        val token = "header.not-valid-base64!@#$.signature"

        val expiration = extractor.extractExpiration(token)

        // Should return default expiration
        assertThat(expiration).isAfter(Instant.now())
        assertThat(expiration).isBefore(Instant.now().plusSeconds(1000))
    }

    @Test
    fun `should handle exp claim with invalid format`() {
        val payload = """{"exp":"not-a-number"}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val expiration = extractor.extractExpiration(token)

        // Should return default expiration
        assertThat(expiration).isAfter(Instant.now())
        assertThat(expiration).isBefore(Instant.now().plusSeconds(1000))
    }

    // ========================================
    // USER ID EXTRACTION TESTS
    // ========================================

    @Test
    fun `should extract user ID from valid token`() {
        val expectedUserId = UUID.randomUUID()
        val payload = """{"sub":"$expectedUserId"}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val userId = extractor.extractUserId(token)

        assertThat(userId).isEqualTo(expectedUserId)
    }

    @Test
    fun `should extract user ID from token with multiple claims`() {
        val expectedUserId = UUID.randomUUID()
        val payload = """{"sub":"$expectedUserId","jti":"abc-123","exp":1234567890}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val userId = extractor.extractUserId(token)

        assertThat(userId).isEqualTo(expectedUserId)
    }

    @Test
    fun `should return null when sub claim is missing`() {
        val payload = """{"jti":"abc-123","exp":1234567890}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val userId = extractor.extractUserId(token)

        assertThat(userId).isNull()
    }

    @Test
    fun `should return null when sub claim is not a valid UUID`() {
        val payload = """{"sub":"not-a-uuid"}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val userId = extractor.extractUserId(token)

        assertThat(userId).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "invalid-token",
        "only.two",
        "",
        "not-a-jwt"
    ])
    fun `should return null for invalid token structure when extracting user ID`(token: String) {
        val userId = extractor.extractUserId(token)

        assertThat(userId).isNull()
    }

    @Test
    fun `should return null for malformed base64 payload when extracting user ID`() {
        val token = "header.not-valid-base64!@#$.signature"

        val userId = extractor.extractUserId(token)

        assertThat(userId).isNull()
    }

    // ========================================
    // COMPREHENSIVE TOKEN TESTS
    // ========================================

    @Test
    fun `should extract all claims from complete JWT token`() {
        val expectedUserId = UUID.randomUUID()
        val expectedJti = "test-jti-456"
        val expectedExp = 1234567890L
        val payload = """{"sub":"$expectedUserId","jti":"$expectedJti","exp":$expectedExp,"iat":1234567000}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "eyJhbGciOiJIUzI1NiJ9.$encodedPayload.signature"

        val userId = extractor.extractUserId(token)
        val jti = extractor.extractJti(token)
        val expiration = extractor.extractExpiration(token)
        val hasValidStructure = extractor.hasValidStructure(token)

        assertThat(userId).isEqualTo(expectedUserId)
        assertThat(jti).isEqualTo(expectedJti)
        assertThat(expiration).isEqualTo(Instant.ofEpochSecond(expectedExp))
        assertThat(hasValidStructure).isTrue()
    }

    @Test
    fun `should handle token with spaces in payload`() {
        val expectedJti = "test-jti-789"
        val payload = """{ "jti" : "$expectedJti" , "exp" : 1234567890 }"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val jti = extractor.extractJti(token)

        assertThat(jti).isEqualTo(expectedJti)
    }

    @Test
    fun `should handle token with nested JSON objects`() {
        val expectedJti = "nested-jti"
        val payload = """{"jti":"$expectedJti","nested":{"key":"value"},"exp":1234567890}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val jti = extractor.extractJti(token)

        assertThat(jti).isEqualTo(expectedJti)
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    fun `should handle empty payload`() {
        val payload = ""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val jti = extractor.extractJti(token)
        val userId = extractor.extractUserId(token)

        assertThat(jti).isNull()
        assertThat(userId).isNull()
    }

    @Test
    fun `should handle payload with empty JSON object`() {
        val payload = "{}"
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val jti = extractor.extractJti(token)
        val userId = extractor.extractUserId(token)

        assertThat(jti).isNull()
        assertThat(userId).isNull()
    }

    @Test
    fun `should handle token with special characters in claims`() {
        val jtiWithSpecialChars = "test-!@#$%^&*()-jti"
        val payload = """{"jti":"$jtiWithSpecialChars"}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val jti = extractor.extractJti(token)

        assertThat(jti).isEqualTo(jtiWithSpecialChars)
    }

    @Test
    fun `should handle very long tokens`() {
        val longJti = "a".repeat(1000)
        val payload = """{"jti":"$longJti"}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val jti = extractor.extractJti(token)

        assertThat(jti).isEqualTo(longJti)
    }

    @Test
    fun `should handle token with URL-safe base64 encoding`() {
        // URL-safe base64 uses - and _ instead of + and /
        val userId = UUID.randomUUID()
        val payload = """{"sub":"$userId"}"""
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val extractedUserId = extractor.extractUserId(token)

        assertThat(extractedUserId).isEqualTo(userId)
    }

    @Test
    fun `should handle token with extra whitespace`() {
        val expectedJti = "whitespace-jti"
        val payload = """  { "jti" : "$expectedJti"  }  """
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val token = "header.$encodedPayload.signature"

        val jti = extractor.extractJti(token)

        assertThat(jti).isEqualTo(expectedJti)
    }
}
