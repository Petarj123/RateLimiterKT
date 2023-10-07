package com.petarj123.ratelimiter.rate_limiter.implementation

import com.petarj123.ratelimiter.rate_limiter.data.RateLimitParamsDTO

interface RateLimiter {
    fun isAllowed(params: RateLimitParamsDTO): Boolean

}
