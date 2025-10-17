# Architecture & Security Overview

## Module Map

| Area | Package | Responsibility |
|------|---------|----------------|
| Auth | `com.viniss.todo.auth` | Authentication models, JWT services, security filter |
| Config | `com.viniss.todo.config` | Spring Security setup |
| Persistence | `com.viniss.todo.auth.AppUserEntity` (+ Flyway scripts) | User storage |
| Tests | `src/test/kotlin/com/viniss/todo` | Unit and integration coverage |

Flow highlights:

1. `AuthService` handles register/login, delegating token issuance to a `TokenService` implementation (`JjwtHmacTokenService` for HS384 and `NimbusRsaTokenService` for RS256).
2. `JwtAuthFilter` extracts bearer tokens and uses `TokenService` for validation. Successful authentication yields an `AuthUser` principal.
3. `SecurityConfig` wires the filter before `UsernamePasswordAuthenticationFilter` and sets stateless sessions.

## JWT Flow

```
Client -> /api/auth/register|login -> AuthService -> TokenService -> signed JWT (HS384 or RS256)
Client -> Protected endpoint with Bearer token -> JwtAuthFilter -> TokenService validation -> SecurityContext
```

Key aspects:

- HS384 tokens use the shared secret from `JwtProps.hmacSecretBytes`.
- RS256 tokens rely on `NimbusRsaTokenService`, which signs with the configured private key and validates against JWKS.
- `TokenService` remains the stable contract to swap implementations without changing consumers.

## Security Notes

- **Secret Management:** For HS384, ensure `JWT_HS384_SECRET_B64` is at least 48 bytes (base64). Keep it rotated regularly.
- **RS256 Rotation:** When using RS256, publish a JWKS with versioned `kid`s. `NimbusRsaTokenService` caches keys and re-fetches when a token references an unknown `kid`.
- **spring.jpa.open-in-view:** Enabled by default; consider setting `spring.jpa.open-in-view=false` in production to avoid lazy-loading during view rendering.
- **Password Policy:** `AuthService` enforces minimum password length (>= 6). Adopt stronger requirements if needed.
- **Clock Skew:** Both token services honour `clockSkewSeconds`. Ensure systems are reasonably synchronized.
- **Error Responses:** `JsonAuthEntryPoint` ensures 401 responses are JSON (`{"error":"invalid_token"}`), reducing information leakage while still being consistent for clients.
