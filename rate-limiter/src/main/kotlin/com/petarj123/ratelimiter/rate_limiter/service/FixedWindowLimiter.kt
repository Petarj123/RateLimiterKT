package com.petarj123.ratelimiter.rate_limiter.service

import com.petarj123.ratelimiter.rate_limiter.data.RateLimitParamsDTO
import com.petarj123.ratelimiter.rate_limiter.data.RateLimiterResponse
import com.petarj123.ratelimiter.rate_limiter.implementation.RateLimiter
import com.petarj123.ratelimiter.rate_limiter.implementation.RateLimiterManagement
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class FixedWindowLimiter(val stringRedisTemplate: StringRedisTemplate, val clientSuspensionService: ClientSuspensionService) : RateLimiter, RateLimiterManagement {
    private val logger = LoggerFactory.getLogger(FixedWindowLimiter::class.java)
    // TODO Handle exceptions
    override fun isAllowed(params: RateLimitParamsDTO): RateLimiterResponse {
        val rateLimitKey = "rate_limit:${params.identifier}"
        val currentCount: Long? = increment(rateLimitKey)
        if (currentCount == null) {
            logger.error("Current request count for $params.identifier is null")
            return RateLimiterResponse(false, getRemainingRequests(rateLimitKey))
        }
        if (clientSuspensionService.isSuspended(params.identifier)) {
            logger.error("Client $params.identifier is suspended")
            return RateLimiterResponse(false, getRemainingRequests(rateLimitKey))
        }
        if (currentCount == 1L) {
            stringRedisTemplate.expire(rateLimitKey, params.timeWindowSeconds, TimeUnit.SECONDS)
        }
        if (currentCount > params.suspensionThreshold) {
            clientSuspensionService.suspend(params.identifier, params.suspensionDuration)
        }
        return RateLimiterResponse(currentCount <= params.maxRequests, getRemainingRequests(rateLimitKey))
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

    private fun getRemainingRequests(identifier: String): Long {
        return stringRedisTemplate.opsForValue().get(identifier)?.toLong() ?: 0
    }

}