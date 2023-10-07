package com.petarj123.ratelimiter.rate_limiter.service

import com.petarj123.ratelimiter.rate_limiter.data.RateLimitParamsDTO
import com.petarj123.ratelimiter.rate_limiter.implementation.RateLimiter
import com.petarj123.ratelimiter.rate_limiter.implementation.RateLimiterManagement
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class SlidingWindowLimiter(val stringRedisTemplate: StringRedisTemplate, val clientSuspensionService: ClientSuspensionService) : RateLimiter, RateLimiterManagement {
    private val logger = LoggerFactory.getLogger(SlidingWindowLimiter::class.java)
    override fun isAllowed(params: RateLimitParamsDTO): Boolean {
        val rateLimitKey = "rate_limit:${params.identifier}"
        val currentCount: Long? = increment(rateLimitKey)
        if (currentCount == null) {
            logger.error("Current request count for $params.identifier is null")
            return false
        }
        if (clientSuspensionService.isSuspended(params.identifier)) {
            logger.error("Client $params.identifier is suspended")
            return false
        }
        if (currentCount == 1L) {
            stringRedisTemplate.expire(rateLimitKey, params.timeWindowSeconds, TimeUnit.SECONDS)
        }
        if (currentCount > params.suspensionThreshold) {
            clientSuspensionService.suspend(params.identifier, params.suspensionDuration)
        }
        return currentCount <= params.maxRequests
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