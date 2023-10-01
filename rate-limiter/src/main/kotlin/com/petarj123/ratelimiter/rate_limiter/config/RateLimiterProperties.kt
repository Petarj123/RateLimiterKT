package com.petarj123.ratelimiter.rate_limiter.config

import com.petarj123.ratelimiter.rate_limiter.model.FallbackStrategy
import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.HashSet

@ConfigurationProperties(prefix = "rate-limiter")
class RateLimiterProperties {
    var defaultMaxRequests: Int = 100
    var defaultTimeWindowSeconds: Long = 60
    var whitelistedIps: Set<String> = HashSet()
    var blacklistedIps: Set<String> = HashSet()
    var fallback: FallbackStrategy? = null
    var adaptiveRateLimit: Boolean = false
    var highLoadThreshold: Double = 0.8
    var lowLoadThreshold: Double = 0.5
    var highLoadMultiplier: Double = 0.8
    var lowLoadMultiplier: Double = 1.1
    var decreaseOnHighLoad: Boolean = false
    var increaseOnLowLoad: Boolean = false
    var suspensionDuration: Long = 600
    var suspensionThreshold: Int = 1000
}