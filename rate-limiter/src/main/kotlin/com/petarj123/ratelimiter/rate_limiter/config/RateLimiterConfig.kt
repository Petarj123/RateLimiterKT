package com.petarj123.ratelimiter.rate_limiter.config

import com.petarj123.ratelimiter.rate_limiter.service.AdaptiveRateLimiter
import com.petarj123.ratelimiter.rate_limiter.service.ClientSuspensionService
import com.petarj123.ratelimiter.rate_limiter.service.RateLimiterService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import java.util.concurrent.ConcurrentHashMap

@Configuration
class RateLimiterConfig {

    @Bean
    fun rateLimiterProperties(): RateLimiterProperties {
        return RateLimiterProperties()
    }
    @Bean
    fun rateLimiter(stringRedisTemplate: StringRedisTemplate, clientSuspensionService: ClientSuspensionService): RateLimiterService {
        return RateLimiterService(stringRedisTemplate, clientSuspensionService)
    }
    @Bean
    fun clientSuspensionService(stringRedisTemplate: StringRedisTemplate): ClientSuspensionService {
        return ClientSuspensionService(stringRedisTemplate)
    }
    @Bean
    fun adaptiveRateLimiter(meterRegistry: MeterRegistry, listableBeanFactory: ListableBeanFactory, rateLimiterProperties: RateLimiterProperties): AdaptiveRateLimiter {
        return AdaptiveRateLimiter(meterRegistry, listableBeanFactory, rateLimiterProperties, ConcurrentHashMap())
    }

}