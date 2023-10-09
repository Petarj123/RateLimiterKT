package com.petarj123.ratelimiter.global

import com.petarj123.ratelimiter.interceptor.RateLimiterInterceptor
import com.petarj123.ratelimiter.rate_limiter.config.RateLimiterProperties
import com.petarj123.ratelimiter.rate_limiter.service.AdaptiveRateLimiter
import com.petarj123.ratelimiter.rate_limiter.service.LeakyTokenBucketLimiter
import com.petarj123.ratelimiter.rate_limiter.service.FixedWindowLimiter
import com.petarj123.ratelimiter.rate_limiter.service.TokenBucketLimiter
import com.petarj123.ratelimiter.redis.config.RedisProperties
import com.petarj123.ratelimiter.redis.health.RedisHealthIndicator
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
class GlobalConfiguration {
    @Bean
    @ConfigurationProperties(prefix = "spring.redis")
    fun redisProperties(): RedisProperties {
        return RedisProperties()
    }
    @Bean
    fun rateLimiterInterceptor(fixedWindowLimiter: FixedWindowLimiter, tokenBucketLimiter: TokenBucketLimiter, leakyTokenBucketLimiter: LeakyTokenBucketLimiter, rateLimiterProperties: RateLimiterProperties, redisHealthIndicator: RedisHealthIndicator, adaptiveRateLimiter: AdaptiveRateLimiter): RateLimiterInterceptor {
        return RateLimiterInterceptor(fixedWindowLimiter, tokenBucketLimiter, leakyTokenBucketLimiter, rateLimiterProperties, redisHealthIndicator, adaptiveRateLimiter)
    }
    @Bean
    fun redisHealthIndicator(stringRedisTemplate: StringRedisTemplate): RedisHealthIndicator {
        return RedisHealthIndicator(stringRedisTemplate)
    }
}