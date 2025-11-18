# Security Improvements

This document outlines the security enhancements implemented in the application.

## 1. Input Validation

### Overview
All DTOs now have comprehensive validation to prevent invalid data from entering the system.

### Implementation
- **AuthRequest** (`auth/AuthRequest.kt`):
  - `@Email` - Validates email format
  - `@NotBlank` - Ensures fields are not empty
  - `@Size(min=6, max=100)` - Password length validation

- **All DTOs** have appropriate validation:
  - `@NotBlank` for required fields
  - `@Size` for length constraints
  - `@Min` for numeric constraints
  - `@Valid` in controllers to trigger validation

### Test Coverage
See `validation/NoHtmlValidatorTest.kt` for validation tests.

## 2. XSS Prevention

### Overview
Custom `@NoHtml` validation annotation prevents HTML/script injection attacks.

### Protected Patterns
- `<script>` tags
- Event handlers (onclick, onerror, etc.)
- `<iframe>`, `<object>`, `<embed>` tags
- `javascript:` protocol
- `<img>` tags
- `<style>` tags

### Usage
```kotlin
@field:NoHtml(message = "Field cannot contain HTML or script tags")
val fieldName: String
```

### Implementation
- `validation/NoHtml.kt` - Annotation definition
- `validation/NoHtmlValidator.kt` - Validation logic

### Example Blocked Input
```json
{
  "title": "<script>alert('xss')</script>"
}
```
Returns: HTTP 400 - "Field cannot contain HTML or script tags"

## 3. Rate Limiting

### Overview
Prevents brute force attacks by limiting requests to authentication endpoints.

### Configuration
- **Limit**: 5 requests per minute per IP address
- **Scope**: All `/api/auth/*` endpoints
- **Storage**: In-memory (ConcurrentHashMap)

### Implementation
- `security/RateLimitService.kt` - Bucket4j rate limiting
- `security/RateLimitFilter.kt` - Servlet filter

### Response
When rate limit is exceeded:
```json
HTTP 429 Too Many Requests
{
  "error": "Too many requests. Please try again later."
}
```

### IP Detection
Supports `X-Forwarded-For` header for proxy/load balancer scenarios.

## 4. Account Lockout

### Overview
Locks accounts after repeated failed login attempts to prevent credential stuffing.

### Configuration
- **Max Attempts**: 5 failed logins
- **Lockout Duration**: 15 minutes
- **Storage**: In-memory (ConcurrentHashMap)

### Implementation
- `security/LoginAttemptService.kt` - Tracks failed attempts
- Integrated in `auth/AuthService.kt` login method

### Behavior
1. Track failed login attempts per email
2. After 5 failed attempts, lock account for 15 minutes
3. Reset counter on successful login
4. Auto-expire after lockout period

### Error Messages
```
Account temporarily locked due to too many failed attempts. Please try again later.
```

### Test Coverage
See `security/LoginAttemptServiceTest.kt`

## 5. Logging & Auditing

### Overview
Comprehensive logging for security events and audit trail.

### Configuration
- **Framework**: SLF4J + Logback
- **Config File**: `resources/logback-spring.xml`

### Log Files
1. **application.log** - General application logs
   - Retention: 30 days
   - Max size: 1GB

2. **security-audit.log** - Security events only
   - Retention: 90 days
   - Max size: 2GB

### Logged Events
- ✅ User registration (success)
- ✅ User registration failures (duplicate email)
- ✅ Successful logins
- ⚠️ Failed login attempts (with remaining attempts count)
- ⚠️ Login attempts on locked accounts
- ℹ️ All authentication events

### Log Format
```
yyyy-MM-dd HH:mm:ss.SSS [thread] LEVEL logger - message
```

### Example Log Entries
```
2025-11-18 10:30:45.123 [http-nio-8082-exec-1] INFO  c.v.t.auth.AuthService - User registered successfully: user@example.com
2025-11-18 10:31:12.456 [http-nio-8082-exec-2] WARN  c.v.t.auth.AuthService - Failed login attempt for email: user@example.com. Remaining attempts: 4
2025-11-18 10:32:05.789 [http-nio-8082-exec-3] INFO  c.v.t.auth.AuthService - User logged in successfully: user@example.com
```

## 6. Security Testing

### Unit Tests
- `validation/NoHtmlValidatorTest.kt` - XSS prevention tests
- `security/LoginAttemptServiceTest.kt` - Account lockout tests
- `security/RateLimitServiceTest.kt` - Rate limiting tests

### Test Coverage
Run tests with:
```bash
./gradlew test
```

Check coverage with:
```bash
./gradlew jacocoTestReport
```

## Dependencies Added

### build.gradle.kts
```kotlin
// Rate Limiting
implementation("com.bucket4j:bucket4j-core:8.10.1")
```

### Existing Dependencies Used
- `spring-boot-starter-validation` - Bean validation
- `spring-boot-starter-security` - Security framework
- SLF4J (included in Spring Boot) - Logging

## Security Best Practices

### ✅ Implemented
1. Input validation on all endpoints
2. XSS prevention via input sanitization
3. Rate limiting on authentication endpoints
4. Account lockout after failed attempts
5. Comprehensive security logging
6. Email format validation
7. Password length enforcement

### 🔒 Recommendations for Production
1. Use Redis/Hazelcast for distributed rate limiting
2. Add CAPTCHA after 2-3 failed attempts
3. Implement password complexity rules (uppercase, numbers, symbols)
4. Add email verification for registration
5. Implement 2FA/MFA
6. Use HTTPS only (enforce in production)
7. Add security headers (CSP, HSTS, X-Frame-Options)
8. Monitor security-audit.log for suspicious activity
9. Set up alerts for multiple failed login attempts
10. Implement password reset with secure tokens

## Monitoring

### Key Metrics to Monitor
- Failed login attempts per IP/email
- Rate limit violations
- Account lockouts
- Suspicious patterns in security-audit.log

### Alert Thresholds (Recommended)
- Alert on 10+ failed logins from same IP in 5 minutes
- Alert on 50+ rate limit violations in 10 minutes
- Alert on unusual login patterns (time, location)

## Configuration

### Environment Variables
No additional environment variables required. All configurations use sensible defaults.

### Customization
To customize rate limits or lockout settings:
- Edit `security/RateLimitService.kt` for rate limits
- Edit `security/LoginAttemptService.kt` companion object for lockout settings

## Migration Notes

### Breaking Changes
- AuthRequest now requires valid email format
- Text fields reject HTML/script content
- Rate limiting applies to all auth endpoints

### Rollback Plan
If issues occur:
1. Remove `@Valid` from AuthController
2. Remove `@NoHtml` from DTOs
3. Disable RateLimitFilter in security config
4. Disable LoginAttemptService checks in AuthService

## Support

For security concerns or questions, refer to:
- Application logs: `logs/application.log`
- Security audit: `logs/security-audit.log`
- Test files for usage examples
