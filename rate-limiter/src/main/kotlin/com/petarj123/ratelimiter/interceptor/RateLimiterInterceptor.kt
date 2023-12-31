package com.petarj123.ratelimiter.interceptor

import com.petarj123.ratelimiter.rate_limiter.annotation.RateLimit
import com.petarj123.ratelimiter.rate_limiter.config.RateLimiterProperties
import com.petarj123.ratelimiter.rate_limiter.implementation.RateLimiter
import com.petarj123.ratelimiter.rate_limiter.data.Algorithm
import com.petarj123.ratelimiter.rate_limiter.data.FallbackStrategy
import com.petarj123.ratelimiter.rate_limiter.data.RateLimitParamsDTO
import com.petarj123.ratelimiter.rate_limiter.service.AdaptiveRateLimiter
import com.petarj123.ratelimiter.rate_limiter.service.LeakyTokenBucketLimiter
import com.petarj123.ratelimiter.rate_limiter.service.FixedWindowLimiter
import com.petarj123.ratelimiter.rate_limiter.service.TokenBucketLimiter
import com.petarj123.ratelimiter.redis.health.RedisHealthIndicator
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Status
import org.springframework.http.HttpStatus
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

class RateLimiterInterceptor(
    private val fixedWindowLimiter: FixedWindowLimiter,
    private val tokenBucketLimiter: TokenBucketLimiter,
    private val leakyTokenBucketLimiter: LeakyTokenBucketLimiter,
    private val rateLimiterProperties: RateLimiterProperties,
    private val redisHealthIndicator: RedisHealthIndicator,
    private val adaptiveRateLimiter: AdaptiveRateLimiter
) : HandlerInterceptor {
    private val rateLimiterAlgorithm: RateLimiter = when(rateLimiterProperties.algorithm) {
        Algorithm.FIXED_WINDOW -> fixedWindowLimiter
        Algorithm.TOKEN_BUCKET -> tokenBucketLimiter
        Algorithm.LEAKY_BUCKET -> leakyTokenBucketLimiter
    }
    @Throws(Exception::class)
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (handler !is HandlerMethod) {
            return true // If not a HandlerMethod, just let it go.
        }
        val clientIP = extractClientIP(request)
        val rateLimit: RateLimit? = handler.method.getAnnotation(RateLimit::class.java)

        // Check global whitelist and blacklist first
        if (rateLimiterProperties.whitelistedIps.contains(clientIP)) {
            return true
        } else if (rateLimiterProperties.blacklistedIps.contains(clientIP)) {
            response.status = HttpStatus.FORBIDDEN.value()
            response.contentType = "application/json"
            val message = """{"message": "You have been blacklisted."}"""
            response.writer.write(message)
            return false
        }

        // Check if method-specific whitelist and blacklist if RateLimit is present
        if (rateLimit != null) {
            if (clientIP in setOf(*rateLimit.whitelist)) {
                return true
            } else if (clientIP in setOf(*rateLimit.blacklist)) {
                response.status = HttpStatus.FORBIDDEN.value()
                response.contentType = "application/json"
                val message = """{"message": "You have been blacklisted."}"""
                response.writer.write(message)
                return false
            }
        }
        // If Redis is down
        val health: Health? = redisHealthIndicator.getHealth(false)
        if (health != null) {
            if (health.status.equals(Status.DOWN)) {
                val fallback: FallbackStrategy = if (rateLimiterProperties.fallback != null) {
                    rateLimiterProperties.fallback!!
                } else {
                    rateLimit!!.fallback
                }
                return if (fallback != FallbackStrategy.BLOCK) {
                    response.status = HttpStatus.OK.value()
                    response.contentType = "application/json"
                    val message = """{"message": "Service is currently experiencing issues, but your request is being processed with degraded performance."}"""
                    response.writer.write(message)
                    return true
                } else {
                    response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                    response.contentType = "application/json"
                    val message = """{"message": "Service is currently unavailable due to technical issues."}"""
                    response.writer.write(message)
                    false
                }
            }
        }

        val rateLimitParams = buildRateLimitParamsDTO(
            identifier = clientIP,
            rateLimit = rateLimit,
            endpoint = request.requestURI
        )
        val rateLimiterResponse = rateLimiterAlgorithm.isAllowed(rateLimitParams)
        if (!rateLimiterResponse.isAllowed) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.addHeader("X-Remaining-Requests", rateLimiterResponse.remainingRequests.toString())
            return false
        }

        response.addHeader("X-Remaining-Requests", rateLimiterResponse.remainingRequests.toString())
        return true
    }
    private fun buildRateLimitParamsDTO(identifier: String, rateLimit: RateLimit?,  endpoint: String): RateLimitParamsDTO {
        var maxRequests: Int
        val timeWindowSeconds: Long

        if (rateLimit != null) {
            maxRequests = rateLimit.maxRequests
            timeWindowSeconds = rateLimit.timeWindowSeconds.toLong()
        } else {
            maxRequests = rateLimiterProperties.defaultMaxRequests
            timeWindowSeconds = rateLimiterProperties.defaultTimeWindowSeconds
        }

        // If Adaptive Rate Limiting is enabled
        if (rateLimiterProperties.adaptiveRateLimit) {
            adaptiveRateLimiter.adjustRateLimitBasedOnSystemLoad(endpoint)
            maxRequests = adaptiveRateLimiter.getCurrentRateLimit(endpoint)
        }

        return RateLimitParamsDTO(identifier = identifier,
            maxRequests = maxRequests,
            timeWindowSeconds = timeWindowSeconds,
            suspensionDuration = rateLimiterProperties.suspensionDuration,
            suspensionThreshold = rateLimiterProperties.suspensionThreshold,
            bucketCapacity = rateLimiterProperties.defaultBucketCapacity,
            bucketRefillRate = rateLimiterProperties.defaultRefillRate,
            bucketRefillTime = rateLimiterProperties.defaultBucketRefillTime,
            bucketTTL = rateLimiterProperties.userBucketTTL,
            dripRate = rateLimiterProperties.defaultDripRate
        )
    }

    private fun extractClientIP(request: HttpServletRequest?): String {
        var remoteAddr = ""
        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR") ?: ""
            if (remoteAddr.isBlank()) {
                remoteAddr = request.remoteAddr
            }
        }
        return remoteAddr
    }
}