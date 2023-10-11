package com.petarj123.ratelimiter.rate_limiter.service

import com.petarj123.ratelimiter.rate_limiter.data.RateLimitParamsDTO
import com.petarj123.ratelimiter.rate_limiter.data.RateLimiterResponse
import com.petarj123.ratelimiter.rate_limiter.implementation.RateLimiter
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.TimeUnit

@Service
class LeakyTokenBucketLimiter(private val stringRedisTemplate: StringRedisTemplate, private val clientSuspensionService: ClientSuspensionService) : RateLimiter {

    private val logger = LoggerFactory.getLogger(LeakyTokenBucketLimiter::class.java)

    override fun isAllowed(params: RateLimitParamsDTO): RateLimiterResponse {
        val limiterKey = "leaky:${params.identifier}"

        if (clientSuspensionService.isSuspended(params.identifier)) {
            logger.error("Client ${params.identifier} is suspended")
            return RateLimiterResponse(false, getRemainingRequests(limiterKey))

        }

        val now = Instant.now().epochSecond
        val bucket = stringRedisTemplate.opsForHash<String, String>().entries(limiterKey)

        // Drip logic
        val lastDripTime = bucket["LastDripTime"]?.toLong() ?: now
        val elapsed = now - lastDripTime
        val tokensToAdd = (params.dripRate * elapsed).toInt() // Number of tokens added since last request
        var tokens = bucket["Tokens"]?.toInt() ?: params.bucketCapacity // Default to full bucket if not present
        tokens = (tokens + tokensToAdd).coerceAtMost(params.bucketCapacity) // Ensuring we don't exceed bucket capacity

        // Request logic
        return if (tokens > 0) {
            stringRedisTemplate.opsForHash<String, String>().putAll(
                limiterKey,
                mapOf(
                    "Tokens" to (tokens - 1).toString(),
                    "LastDripTime" to now.toString()
                )
            )

            stringRedisTemplate.expire(limiterKey, 24, TimeUnit.HOURS)
            RateLimiterResponse(true, getRemainingRequests(limiterKey))
        } else {
            RateLimiterResponse(false, getRemainingRequests(limiterKey))
        }
    }
    private fun getRemainingRequests(identifier: String): Long {
        return stringRedisTemplate.opsForHash<String, String>().get(identifier, "Tokens")?.toLong() ?: 0
    }
}