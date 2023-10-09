package com.petarj123.ratelimiter.rate_limiter.data

data class RateLimitParamsDTO(val identifier: String,
                              val maxRequests: Int,
                              val timeWindowSeconds: Long,
                              val bucketCapacity: Int,
                              val bucketRefillRate: Int,
                              val bucketRefillTime: Long,
                              val suspensionDuration: Long,
                              val suspensionThreshold: Int,
                              val bucketTTL: Long,
                              val dripRate: Int)
