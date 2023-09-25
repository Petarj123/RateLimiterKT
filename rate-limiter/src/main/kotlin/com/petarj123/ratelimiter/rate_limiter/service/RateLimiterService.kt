package com.petarj123.ratelimiter.rate_limiter.service

import com.petarj123.ratelimiter.rate_limiter.implementation.RateLimiter
import com.petarj123.ratelimiter.rate_limiter.implementation.RateLimiterManagement
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RateLimiterService(val stringRedisTemplate: StringRedisTemplate, val clientSuspensionService: ClientSuspensionService) : RateLimiter, RateLimiterManagement {
    private val logger = LoggerFactory.getLogger(RateLimiterService::class.java)
    override fun isAllowed(identifier: String, maxRequests: Int, timeWindowSeconds: Long, suspensionDuration: Long, suspensionThreshold: Long): Boolean {
        val rateLimitKey = "rate_limit:${identifier}"
        val currentCount: Long? = increment(rateLimitKey)
        if (currentCount == null) {
            logger.error("Current request count for $identifier is null")
            return false
        }
        if (clientSuspensionService.isSuspended(identifier)) {
            logger.error("Client $identifier is suspended")
            return false
        }
        if (currentCount == 1L) {
            stringRedisTemplate.expire(rateLimitKey, timeWindowSeconds, TimeUnit.SECONDS)
        }
        if (currentCount > suspensionThreshold) {
            clientSuspensionService.suspend(identifier, suspensionDuration)
        }
        return currentCount <= maxRequests
    }

    override fun decrement(identifier: String) {
        stringRedisTemplate.opsForValue().decrement(identifier)
    }

    override fun increment(identifier: String): Long? {
        return stringRedisTemplate.opsForValue().increment(identifier, 1)
    }

    override fun getCurrentCount(identifier: String): Long? {
        return stringRedisTemplate.opsForValue().get(identifier)?.toLong()
    }

    override fun resetCount(identifier: String) {
        stringRedisTemplate.delete(identifier)
    }

    override fun getRemainingTTL(identifier: String): Long? {
        return stringRedisTemplate.getExpire(identifier, TimeUnit.SECONDS)
    }
}