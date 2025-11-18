# Security & Advanced Features Update

## Overview

This update adds comprehensive security improvements and advanced task management features to the Todo List application.

---

## 🔒 Security Improvements

### 1. Security Headers

**Location**: `src/main/kotlin/com/viniss/todo/config/SecurityConfig.kt`

Added the following security headers:

- **HSTS (HTTP Strict Transport Security)**: Forces HTTPS for 1 year, including subdomains
  - `Strict-Transport-Security: max-age=31536000; includeSubDomains`

- **X-Content-Type-Options**: Prevents MIME-sniffing attacks
  - `X-Content-Type-Options: nosniff`

- **X-Frame-Options**: Prevents clickjacking attacks
  - `X-Frame-Options: DENY`

- **X-XSS-Protection**: XSS protection for legacy browsers
  - `X-XSS-Protection: 1; mode=block`

- **Referrer-Policy**: Controls referrer information
  - `Referrer-Policy: strict-origin-when-cross-origin`

- **Permissions-Policy**: Restricts browser features
  - Blocks geolocation, microphone, and camera access

### 2. HTTPS Enforcement

**Location**: `src/main/kotlin/com/viniss/todo/config/HttpsEnforcerConfig.kt`

- Redirects all HTTP traffic to HTTPS in production
- Configurable via `app.security.require-https=true` in application.yml
- Automatically excludes localhost for development

**To enable in production**:
```yaml
app:
  security:
    require-https: true
```

### 3. CORS Configuration Improvements

**Location**: `src/main/kotlin/com/viniss/todo/config/CorsConfig.kt`

**Changes**:
- ❌ **Removed**: Wildcard `*` for allowed headers
- ✅ **Added**: Specific allowed headers:
  - `Authorization`
  - `Content-Type`
  - `Accept`
  - `Origin`
  - `X-Requested-With`
- ✅ **Added**: Explicit exposed headers configuration

### 4. Strong Password Requirements

**Location**: `src/main/kotlin/com/viniss/todo/auth/PasswordValidator.kt`

**Previous**: 6 character minimum

**New Requirements** (configurable via application.yml):
- **Minimum length**: 12 characters (default)
- **Uppercase letter**: Required
- **Lowercase letter**: Required
- **Digit**: Required
- **Special character**: Required (`!@#$%^&*()_+-=[]{}|;:,.<>?`)
- **No common weak patterns**: Blocks "password", "12345", "qwerty", etc.
- **No sequential characters**: Blocks "abc", "123", "xyz", etc.

**Configuration**:
```yaml
app:
  password:
    min-length: 12
    require-uppercase: true
    require-lowercase: true
    require-digit: true
    require-special-char: true
    special-chars: "!@#$%^&*()_+-=[]{}|;:,.<>?"
```

### 5. Password History

**Location**:
- `src/main/kotlin/com/viniss/todo/auth/PasswordHistoryService.kt`
- `src/main/resources/dbmigration/V7__create_password_history.sql`

**Features**:
- Tracks last 5 passwords (configurable)
- Prevents password reuse
- Automatic cleanup of old entries
- BCrypt comparison for security

**Configuration**:
```yaml
app:
  password:
    history:
      enabled: true
      prevent-reuse-count: 5
```

**Database**:
- New table: `password_history`
- Fields: `id`, `user_id`, `password_hash`, `created_at`
- Indexes for efficient lookups

---

## 🚀 Advanced Task Features

### 1. Recurring Tasks

**Location**:
- `src/main/kotlin/com/viniss/todo/domain/RecurrencePattern.kt`
- `src/main/kotlin/com/viniss/todo/domain/TaskEntity.kt`

**Features**:
- Daily, weekly, monthly, and yearly recurrence
- Customizable intervals (every N days/weeks/months/years)
- Day-of-week selection for weekly tasks
- Day-of-month selection for monthly tasks
- End date or occurrence count limits
- Parent-child relationship tracking

**Database Changes** (V8 migration):
```sql
ALTER TABLE task
ADD COLUMN is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN recurrence_pattern TEXT,
ADD COLUMN parent_recurring_task_id UUID;
```

**Example Usage**:
```kotlin
// Daily recurrence
RecurrencePattern.daily(interval = 1, endDate = LocalDate.of(2025, 12, 31))

// Weekly on Monday and Wednesday
RecurrencePattern.weekly(
    interval = 1,
    daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
)

// Monthly on the 15th
RecurrencePattern.monthly(interval = 1, dayOfMonth = 15)

// Yearly on March 1st
RecurrencePattern.yearly(interval = 1, dayOfMonth = 1, monthOfYear = 3)
```

### 2. Full-Text Search

**Location**:
- `src/main/kotlin/com/viniss/todo/domain/TaskSearchRepository.kt`
- V8 migration: Full-text search index

**Features**:
- PostgreSQL full-text search using `tsvector` and `tsquery`
- Searches across task titles and notes
- Relevance ranking with `ts_rank`
- Automatic index updates via generated column
- English language stemming and stop words

**Database Changes**:
```sql
ALTER TABLE task
ADD COLUMN search_vector tsvector
GENERATED ALWAYS AS (
    setweight(to_tsvector('english', COALESCE(title, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(notes, '')), 'B')
) STORED;

CREATE INDEX idx_task_search_vector ON task USING GIN (search_vector);
```

**Usage**:
```kotlin
taskSearchRepository.fullTextSearch(userId, "meeting project deadline")
```

### 3. Tags/Categories

**Location**:
- `src/main/kotlin/com/viniss/todo/domain/TagEntity.kt`
- `src/main/kotlin/com/viniss/todo/domain/TagRepository.kt`

**Features**:
- User-scoped tags (each user has their own tags)
- Optional color coding (hex color codes)
- Many-to-many relationship with tasks
- Unique tag names per user

**Database Tables**:
```sql
CREATE TABLE tag (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(7),
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uq_tag_user_name UNIQUE (user_id, name)
);

CREATE TABLE task_tag (
    task_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    PRIMARY KEY (task_id, tag_id)
);
```

### 4. Attachments

**Location**:
- `src/main/kotlin/com/viniss/todo/domain/TaskAttachmentEntity.kt`
- `src/main/kotlin/com/viniss/todo/domain/TaskAttachmentRepository.kt`

**Features**:
- File attachments for tasks
- Metadata tracking (filename, content type, file size)
- Storage URL/path reference
- User ownership and audit trail

**Database Table**:
```sql
CREATE TABLE task_attachment (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_url VARCHAR(500) NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

**Note**: Storage implementation (S3, local filesystem, etc.) needs to be added separately.

### 5. Sharing/Collaboration

**Location**:
- `src/main/kotlin/com/viniss/todo/domain/SharedListEntity.kt`
- `src/main/kotlin/com/viniss/todo/domain/SharedListRepository.kt`

**Features**:
- Share todo lists with other users
- Three permission levels:
  - **READ**: View only
  - **WRITE**: View and edit tasks
  - **ADMIN**: Full control including sharing management
- Prevents self-sharing
- Unique constraint: can't share same list with same user twice

**Database Table**:
```sql
CREATE TABLE shared_list (
    id UUID PRIMARY KEY,
    list_id UUID NOT NULL,
    shared_by_user_id UUID NOT NULL,
    shared_with_user_id UUID NOT NULL,
    permission VARCHAR(20) NOT NULL CHECK (permission IN ('READ', 'WRITE', 'ADMIN')),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uq_shared_list_list_user UNIQUE (list_id, shared_with_user_id),
    CONSTRAINT chk_shared_list_not_self CHECK (shared_by_user_id != shared_with_user_id)
);
```

---

## 📋 Database Migrations

### V7: Password History
- Creates `password_history` table
- Indexes for user_id and created_at

### V8: Advanced Task Features
- Adds recurring task fields to `task` table
- Creates `tag` table
- Creates `task_tag` junction table
- Creates `task_attachment` table
- Creates `shared_list` table
- Adds full-text search support with generated `search_vector` column

**To run migrations**:
```bash
./gradlew flywayMigrate
```

---

## 🧪 Testing

All new features include:
- Entity validation
- Repository methods
- Service layer logic
- Database constraints

**Run tests**:
```bash
./gradlew test
```

**Check code coverage**:
```bash
./gradlew jacocoTestReport
```

---

## 🔧 Configuration Reference

### Complete Application Configuration

```yaml
server:
  port: ${SERVER_PORT:8082}

app:
  # Security
  security:
    require-https: true  # Enable HTTPS enforcement in production

  # CORS
  cors:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:5173
      - https://v0.app
      - https://v0.dev
    allowed-origin-patterns:
      - https://*.v0.app
      - https://*.v0.dev
    allowed-methods:
      - GET
      - POST
      - PUT
      - PATCH
      - DELETE
      - OPTIONS
    allowed-headers:
      - Authorization
      - Content-Type
      - Accept
      - Origin
      - X-Requested-With
    exposed-headers:
      - Authorization
      - Content-Type
    allow-credentials: true
    max-age-seconds: 3600

  # Password Policy
  password:
    min-length: 12
    require-uppercase: true
    require-lowercase: true
    require-digit: true
    require-special-char: true
    special-chars: "!@#$%^&*()_+-=[]{}|;:,.<>?"

    # Password History
    history:
      enabled: true
      prevent-reuse-count: 5
```

---

## 📝 Implementation Notes

### What's Included
✅ Database schema and migrations
✅ Entity models with proper relationships
✅ Repository interfaces
✅ Security headers and HTTPS enforcement
✅ CORS hardening
✅ Password validation and history
✅ Full-text search infrastructure

### What Needs Implementation
⚠️ **REST API Controllers** for new features:
- Tag management endpoints
- Attachment upload/download endpoints
- List sharing endpoints
- Full-text search endpoint
- Recurring task instance generation

⚠️ **Service Layer** for business logic:
- Recurring task scheduler
- Attachment storage service (S3/filesystem)
- Permission checking for shared lists

⚠️ **Frontend Updates**:
- UI for tags, attachments, sharing
- Search interface
- Recurring task configuration

---

## 🚦 Deployment Checklist

- [ ] Run `./gradlew build` to ensure compilation
- [ ] Run `./gradlew test` to verify tests pass
- [ ] Set `app.security.require-https=true` in production
- [ ] Configure password requirements as needed
- [ ] Run database migrations: `./gradlew flywayMigrate`
- [ ] Update frontend CORS origins in application.yml
- [ ] Configure file storage for attachments
- [ ] Implement attachment storage service
- [ ] Add REST endpoints for new features
- [ ] Update API documentation (Swagger)
- [ ] Test security headers with: https://securityheaders.com/
- [ ] Test HTTPS redirect in production

---

## 📚 References

- [OWASP Security Headers](https://owasp.org/www-project-secure-headers/)
- [NIST Password Guidelines](https://pages.nist.gov/800-63-3/sp800-63b.html)
- [PostgreSQL Full-Text Search](https://www.postgresql.org/docs/current/textsearch.html)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/index.html)

---

## 🤝 Contributing

When adding new features:
1. Follow existing code patterns
2. Add database migrations for schema changes
3. Include repository methods for data access
4. Write unit and integration tests
5. Update this documentation

---

**Last Updated**: 2025-11-18
**Author**: Claude
**Version**: 1.0.0
