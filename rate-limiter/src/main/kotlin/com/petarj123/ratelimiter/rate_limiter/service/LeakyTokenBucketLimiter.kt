package com.petarj123.ratelimiter.rate_limiter.service

import com.petarj123.ratelimiter.rate_limiter.data.RateLimitParamsDTO
import com.petarj123.ratelimiter.rate_limiter.implementation.RateLimiter
import org.springframework.stereotype.Service

@Service
class LeakyTokenBucketLimiter : RateLimiter {
    override fun isAllowed(params: RateLimitParamsDTO): Boolean {
        TODO("Not yet implemented")
    }
}