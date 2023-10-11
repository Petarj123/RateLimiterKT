package com.petarj123.ratelimiter.rate_limiter.service

import com.petarj123.ratelimiter.rate_limiter.data.RateLimitParamsDTO
import com.petarj123.ratelimiter.rate_limiter.data.RateLimiterResponse
import com.petarj123.ratelimiter.rate_limiter.implementation.RateLimiter
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.min

@Service
class TokenBucketLimiter(private val stringRedisTemplate: StringRedisTemplate, private val clientSuspensionService: ClientSuspensionService) : RateLimiter{
    private val logger = LoggerFactory.getLogger(TokenBucketLimiter::class.java)
    override fun isAllowed(params: RateLimitParamsDTO): RateLimiterResponse {
        val limiterKey = "bucket:${params.identifier}"
        val bucket = stringRedisTemplate.opsForHash<String, String>().entries(limiterKey)

        if (clientSuspensionService.isSuspended(params.identifier)) {
            logger.error("Client ${params.identifier} is suspended")
            return RateLimiterResponse(false, getRemainingRequests(limiterKey))
        }

        val capacity = params.bucketCapacity
        if (capacity <= 0) {
            logger.error("Capacity for bucket $limiterKey is 0")
            return RateLimiterResponse(false, getRemainingRequests(limiterKey))
        }

        val tokensRemaining = bucket["TokensRemaining"]?.toInt() ?: capacity
        val lastRefillTime = bucket["LastRefillTime"]?.toLong() ?: Instant.now().epochSecond

        val now = Instant.now().epochSecond
        val elapsed = now - lastRefillTime
        val tokensToAdd = if (elapsed >= params.bucketRefillTime) {
            params.bucketRefillRate
        } else {
            0
        }

        val newTokens = min(tokensRemaining + tokensToAdd, capacity)

        if (newTokens == 0) {
            val noTokenCounter = stringRedisTemplate.opsForValue().increment("bucket:noTokens:${params.identifier}") ?: 1
            if (noTokenCounter > params.suspensionThreshold) {
                clientSuspensionService.suspend(params.identifier, params.suspensionDuration)
                logger.error("Client ${params.identifier} has been suspended due to aggressive behavior.")
                return RateLimiterResponse(false, 0)
            }
        } else {
            // If there are tokens available, reset the aggressive behavior counter for the client
            stringRedisTemplate.delete("bucket:noTokens:${params.identifier}")
            stringRedisTemplate.opsForHash<String, String>().putAll(
                limiterKey,
                mapOf(
                    "TokensRemaining" to (newTokens - 1).toString(),
                    "LastRefillTime" to (if(tokensToAdd > 0) now else lastRefillTime).toString() // Update only if refill happened
                )
            )
            return RateLimiterResponse(true, (newTokens - 1).toLong())
        }

        return RateLimiterResponse(false, getRemainingRequests(limiterKey))
    }
    private fun getRemainingRequests(identifier: String): Long {
        return stringRedisTemplate.opsForHash<String, String>().get(identifier, "TokensRemaining")?.toLong() ?: 0
    }
}