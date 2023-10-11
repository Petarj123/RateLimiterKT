package com.petarj123.ratelimiter.rate_limiter.data

data class RateLimiterResponse(val isAllowed: Boolean, val remainingRequests: Long)
