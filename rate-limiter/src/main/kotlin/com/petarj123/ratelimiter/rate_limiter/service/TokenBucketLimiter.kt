package com.petarj123.ratelimiter.rate_limiter.service

import com.petarj123.ratelimiter.rate_limiter.data.RateLimitParamsDTO
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
    override fun isAllowed(params: RateLimitParamsDTO): Boolean {
        val limiterKey = "bucket:${params.identifier}"
        val bucket = stringRedisTemplate.opsForHash<String, String>().entries(limiterKey)

        if (clientSuspensionService.isSuspended(params.identifier)) {
            logger.error("Client ${params.identifier} is suspended")
            return false
        }

        val capacity = params.bucketCapacity
        if (capacity <= 0) {
            logger.error("Capacity for bucket $limiterKey is 0")
            return false
        }

        if (bucket.isEmpty()) {
            // User's first request. Initialize bucket.
            stringRedisTemplate.opsForHash<String, String>().putAll(
                limiterKey,
                mapOf(
                    "TokensRemaining" to capacity.toString(),
                    "LastRefillTime" to Instant.now().epochSecond.toString()
                )
            )
            stringRedisTemplate.expire(limiterKey, params.bucketTTL, TimeUnit.SECONDS)
            return true
        } else {
            val tokensRemaining = bucket["TokensRemaining"]?.toInt() ?: 0
            val lastRefillTime = bucket["LastRefillTime"]?.toLong() ?: 0L

            // Calculate refill
            val now = Instant.now().epochSecond
            val elapsed = now - lastRefillTime
            val tokensToAdd = if (elapsed >= params.bucketRefillTime) {
                params.bucketRefillRate
            } else {
                0
            }
            val newTokens = min(tokensRemaining + tokensToAdd, capacity)

            // Deduct a token for the request
            return if (newTokens > 0) {
                stringRedisTemplate.opsForHash<String, String>().putAll(
                    limiterKey,
                    mapOf(
                        "TokensRemaining" to (newTokens - 1).toString(),
                        "LastRefillTime" to (if(tokensToAdd > 0) now else lastRefillTime).toString() // Update only if refill happened
                    )
                )
                true
            } else {
                false
            }
        }
    }

}