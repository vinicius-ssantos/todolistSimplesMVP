package com.viniss.todo.auth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.jwt")
data class JwtProps(
    val issuer: String,
    val audience: String,
    val secretB64: String,
    val ttlSeconds: Long,
    val clockSkewSeconds: Long,
    val version: Int
)