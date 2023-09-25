package com.petarj123.ratelimiter.rate_limiter.implementation

interface RateLimiterManagement {
    fun decrement(identifier: String)
    fun increment(identifier: String): Long?
    fun getCurrentCount(identifier: String): Long?
    fun resetCount(identifier: String)
    fun getRemainingTTL(identifier: String): Long?
}