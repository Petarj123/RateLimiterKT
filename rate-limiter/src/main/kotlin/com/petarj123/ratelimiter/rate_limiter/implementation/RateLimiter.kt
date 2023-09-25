package com.petarj123.ratelimiter.rate_limiter.implementation

interface RateLimiter {
    fun isAllowed(identifier: String, maxRequests: Int, timeWindowSeconds: Long, suspensionDuration: Long, suspensionThreshold: Long): Boolean
}