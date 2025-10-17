# JWT Configuration Modes

The application can issue and validate tokens using two strategies:

## HS384 (default)

```
jwt.acceptRS256=false
jwt.secretB64=<base64-url 48+ bytes>
```

Only `secretB64` is required. All other RSA/JWKS properties are ignored.

## RS256 with JWKS

```
jwt.acceptRS256=true
jwt.rsaPrivateKeyPem=<PKCS#8 private key in PEM format>
jwt.rsaKeyId=<kid published in JWKS>
jwt.jwksUri=https://example.com/.well-known/jwks.json
jwt.jwksCacheTtlSeconds=300      # Optional (default 300)
jwt.jwksCacheRefreshSeconds=30   # Optional (default 30)
```

- `rsaPrivateKeyPem` must contain the private key that matches the public key exposed in the JWKS.
- `rsaKeyId` is embedded as the JWT header `kid`, enabling consumers to locate the right public key.
- `jwksUri` points to the JWKS endpoint used to validate RS256 signatures.
- Cache settings control how long JWKS responses are reused before refreshing.

When RS256 is enabled, `secretB64` is still read but only used by legacy HS384 consumers.
