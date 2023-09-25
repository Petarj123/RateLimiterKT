package com.petarj123.ratelimiter.rate_limiter.implementation

interface SuspensionService {
    fun isSuspended(identifier: String): Boolean
    fun suspend(identifier: String, durationInSeconds: Long)
    fun removeSuspension(identifier: String)
    fun getRemainingSuspensionTime(identifier: String): Long
}