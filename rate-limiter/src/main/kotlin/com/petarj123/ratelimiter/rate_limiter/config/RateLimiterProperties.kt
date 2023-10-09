package com.petarj123.ratelimiter.rate_limiter.config

import com.petarj123.ratelimiter.rate_limiter.data.Algorithm
import com.petarj123.ratelimiter.rate_limiter.data.FallbackStrategy
import jdk.jfr.Description
import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.HashSet

@ConfigurationProperties(prefix = "rate-limiter")
class RateLimiterProperties {

    @Description("Default maximum requests allowed in the specified time window.")
    var defaultMaxRequests: Int = 100

    @Description("Default time window in seconds for rate limiting.")
    var defaultTimeWindowSeconds: Long = 60

    @Description("Set of IPs that are whitelisted and will bypass the rate limiting.")
    var whitelistedIps: Set<String> = HashSet()

    @Description("Set of IPs that are explicitly blocked from making requests.")
    var blacklistedIps: Set<String> = HashSet()

    @Description("Fallback strategy in case of unhandled scenarios or system failures.")
    var fallback: FallbackStrategy? = null

    @Description("Flag to determine if adaptive rate limiting based on system load is enabled.")
    var adaptiveRateLimit: Boolean = false

    @Description("Threshold above which the system is considered under high load.")
    var highLoadThreshold: Double = 0.8

    @Description("Threshold below which the system is considered under low load.")
    var lowLoadThreshold: Double = 0.5

    @Description("Multiplier applied during high load situations. Values less than 1 will decrease allowed requests.")
    var highLoadMultiplier: Double = 0.8

    @Description("Multiplier applied during low load situations. Values greater than 1 will increase allowed requests.")
    var lowLoadMultiplier: Double = 1.1

    @Description("Flag to determine if rate should decrease under high load.")
    var decreaseOnHighLoad: Boolean = false

    @Description("Flag to determine if rate should increase under low load.")
    var increaseOnLowLoad: Boolean = false

    @Description("Duration in seconds for which a client will be suspended upon reaching suspension threshold.")
    var suspensionDuration: Long = 600

    @Description("Request threshold beyond which a client will be suspended.")
    var suspensionThreshold: Int = 1000

    @Description("Default capacity of the token bucket.")
    var defaultBucketCapacity: Int = 500

    @Description("Default number of tokens refilled in the token bucket per refill cycle.")
    var defaultRefillRate: Int = 10

    @Description("Time in seconds for each refill cycle of the token bucket.")
    var defaultBucketRefillTime: Long = 60

    @Description("Time-to-live for a user's token bucket in Redis. Adjust based on expected user activity patterns.")
    var userBucketTTL: Long = 3 * defaultBucketRefillTime

    @Description("Rate at which tokens are added for Leaky Bucket (tokens per second)")
    var defaultDripRate: Int = 60;

    @Description("Algorithm chosen for rate limiting.")
    var algorithm:Algorithm = Algorithm.LEAKY_BUCKET
}
