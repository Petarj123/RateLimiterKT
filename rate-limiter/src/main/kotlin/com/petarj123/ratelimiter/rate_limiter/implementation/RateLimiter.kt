package com.petarj123.ratelimiter.rate_limiter.implementation

import com.petarj123.ratelimiter.rate_limiter.data.RateLimitParamsDTO
import com.petarj123.ratelimiter.rate_limiter.data.RateLimiterResponse

interface RateLimiter {
    fun isAllowed(params: RateLimitParamsDTO): RateLimiterResponse

}
