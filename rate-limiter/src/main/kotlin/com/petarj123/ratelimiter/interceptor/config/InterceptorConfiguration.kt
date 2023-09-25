package com.petarj123.ratelimiter.interceptor.config

import com.petarj123.ratelimiter.interceptor.RateLimiterInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class InterceptorConfiguration(val interceptor: RateLimiterInterceptor) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(interceptor)
    }
    
}