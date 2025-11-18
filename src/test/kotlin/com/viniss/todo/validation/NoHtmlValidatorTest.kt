package com.viniss.todo.validation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NoHtmlValidatorTest {

    private val validator = NoHtmlValidator()

    @Test
    fun `should accept plain text`() {
        assertTrue(validator.isValid("This is a plain text", null))
        assertTrue(validator.isValid("Hello World 123", null))
    }

    @Test
    fun `should accept null or blank strings`() {
        assertTrue(validator.isValid(null, null))
        assertTrue(validator.isValid("", null))
        assertTrue(validator.isValid("   ", null))
    }

    @Test
    fun `should reject script tags`() {
        assertFalse(validator.isValid("<script>alert('xss')</script>", null))
        assertFalse(validator.isValid("Hello <script>bad()</script> World", null))
        assertFalse(validator.isValid("<SCRIPT>alert('xss')</SCRIPT>", null))
    }

    @Test
    fun `should reject event handlers`() {
        assertFalse(validator.isValid("<img onclick='alert(1)'>", null))
        assertFalse(validator.isValid("<div onerror='bad()'>", null))
        assertFalse(validator.isValid("<body onload='hack()'>", null))
    }

    @Test
    fun `should reject iframe tags`() {
        assertFalse(validator.isValid("<iframe src='evil.com'></iframe>", null))
        assertFalse(validator.isValid("<IFRAME src='evil.com'>", null))
    }

    @Test
    fun `should reject javascript protocol`() {
        assertFalse(validator.isValid("javascript:alert(1)", null))
        assertFalse(validator.isValid("JAVASCRIPT:alert(1)", null))
    }

    @Test
    fun `should reject img tags`() {
        assertFalse(validator.isValid("<img src='x'>", null))
        assertFalse(validator.isValid("<IMG src='x' onerror='alert(1)'>", null))
    }

    @Test
    fun `should reject object and embed tags`() {
        assertFalse(validator.isValid("<object data='evil.swf'></object>", null))
        assertFalse(validator.isValid("<embed src='evil.swf'>", null))
    }

    @Test
    fun `should reject style tags`() {
        assertFalse(validator.isValid("<style>body{background:red}</style>", null))
        assertFalse(validator.isValid("<STYLE>evil</STYLE>", null))
    }

    @Test
    fun `should accept special characters without html`() {
        assertTrue(validator.isValid("Price: $100 & more!", null))
        assertTrue(validator.isValid("Email: test@example.com", null))
        assertTrue(validator.isValid("Math: 2 > 1 < 3", null))
    }
}
