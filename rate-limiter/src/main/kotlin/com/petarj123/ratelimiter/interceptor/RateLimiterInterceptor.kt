package com.petarj123.ratelimiter.interceptor

import com.petarj123.ratelimiter.rate_limiter.annotation.RateLimit
import com.petarj123.ratelimiter.rate_limiter.config.RateLimiterProperties
import com.petarj123.ratelimiter.rate_limiter.model.FallbackStrategy
import com.petarj123.ratelimiter.rate_limiter.service.AdaptiveRateLimiter
import com.petarj123.ratelimiter.rate_limiter.service.RateLimiterService
import com.petarj123.ratelimiter.redis.health.RedisHealthIndicator
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Status
import org.springframework.http.HttpStatus
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

class RateLimiterInterceptor(private val rateLimiterService: RateLimiterService, private val rateLimiterProperties: RateLimiterProperties, private val redisHealthIndicator: RedisHealthIndicator, private val adaptiveRateLimiter: AdaptiveRateLimiter) : HandlerInterceptor {

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
            return false
        }

        // Check if method-specific whitelist and blacklist if RateLimit is present
        if (rateLimit != null) {
            if (clientIP in setOf(*rateLimit.whitelist)) {
                return true
            } else if (clientIP in setOf(*rateLimit.blacklist)) {
                return false
            }
        }

        // If Redis is down
        val health: Health? = redisHealthIndicator.getHealth(false)
        println(health?.status)
        if (health != null) {
            if (health.status.equals(Status.DOWN)) {
                val fallback: FallbackStrategy = rateLimit?.fallback ?: rateLimiterProperties.fallback
                return if (fallback != FallbackStrategy.BLOCK) {
                    response.status = HttpStatus.OK.value()
                    true
                } else {
                    response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                    false
                }
            }
        }

        // Determine maxRequests and timeWindowSeconds
        var maxRequests: Int
        val timeWindowSeconds: Long
        val endpoint = request.requestURI

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
        println(maxRequests)
        println(clientIP)
        val suspensionDuration: Long = rateLimiterProperties.suspensionDuration
        val suspensionThreshold: Long = rateLimiterProperties.suspensionThreshold.toLong()

        if (!rateLimiterService.isAllowed(clientIP, maxRequests, timeWindowSeconds, suspensionDuration, suspensionThreshold)) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            return false
        }
        return true

    }

    private fun extractClientIP(request: HttpServletRequest?): String {
        var remoteAddr = ""
        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR")
            if (remoteAddr.isNullOrEmpty()) {
                remoteAddr = request.remoteAddr
            }
        }
        return remoteAddr
    }
}