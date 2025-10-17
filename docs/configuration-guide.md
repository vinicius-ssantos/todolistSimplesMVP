# Configuration Guide

## Property Reference

| Property | Default | Description | Production Tips |
|----------|---------|-------------|------------------|
| `server.port` | `8081` | HTTP server port | Override via `SERVER_PORT` |
| `spring.datasource.url` | `jdbc:h2:mem:todo;...` | JDBC connection string | Point to managed DB (Postgres) |
| `spring.datasource.username` | `sa` | DB user | Use secret storage |
| `spring.datasource.password` | _blank_ | DB password | Never commit defaults; use env vars |
| `spring.jpa.hibernate.ddl-auto` | `none` | Schema strategy | Keep `none`; Flyway manages DDL |
| `spring.jpa.open-in-view` | _enabled_ | Lazy loading during view rendering | Set `false` in production |
| `jwt.issuer` | `tickr-api` | JWT `iss` claim | Align with environment |
| `jwt.audience` | `tickr-web` | JWT `aud` claim | Match expected clients |
| `jwt.secretB64` | Random 64-byte fallback | HS384 signing secret | Provide a strong base64 secret |
| `jwt.ttlSeconds` | `900` | Token validity window | Adjust to session requirements |
| `jwt.clockSkewSeconds` | `60` | Allowed clock drift | Keep minimal |
| `jwt.version` | `1` | Custom claim `v` | Increment on breaking changes |
| `jwt.acceptRS256` | `false` | Switch to RS256 flow | See RS256 setup |
| `jwt.jwksUri` | `null` | JWKS endpoint for RS256 | Required when enabling RS256 |
| `jwt.rsaPrivateKeyPem` | `null` | Signing key (PKCS#8) | Store securely (vault, secret manager) |
| `jwt.rsaKeyId` | `null` | `kid` for RS256 header | Must match JWKS entry |
| `jwt.jwksCacheTtlSeconds` | `300` | JWKS cache TTL | Tune to JWKS rotation policy |
| `jwt.jwksCacheRefreshSeconds` | `30` | Re-fetch margin | Prevent stale keys |

## HS384 (Default)

1. Generate a 48+ byte secret (Base64 URL-safe preferred).
2. Export as `JWT_HS384_SECRET_B64` (or set in `application.yml` for dev).
3. Keep `jwt.acceptRS256=false`. All `TokenService` consumers work transparently.

## RS256 Setup

1. **Dependencies**: already bundled (`spring-security-oauth2-jose`, `nimbus-jose-jwt`).
2. **Prepare Key Pair**:
   - Generate RSA key (2048+ bits).
   - Publish public key in JWKS (with unique `kid`).
   - Convert private key to PKCS#8 PEM.
3. **Configuration**:
   ```properties
   jwt.acceptRS256=true
   jwt.rsaPrivateKeyPem=-----BEGIN PRIVATE KEY-----...-----END PRIVATE KEY-----
   jwt.rsaKeyId=prod-key-2025-01
   jwt.jwksUri=https://auth.example.com/.well-known/jwks.json
   ```
4. **Rotation**:
   - Introduce new JWKS entry with fresh `kid`.
   - Update application config with new private key and `rsaKeyId`.
   - `NimbusRsaTokenService` will fallback to cached key until JWKS refresh succeeds.
5. **Verification**:
   - Ensure clients fetch JWKS and verify `alg=RS256`.
   - Monitor logs around JWKS fetch failures (`Unable to fetch JWKS`) to catch connectivity issues.
