package com.petarj123.ratelimiter.rate_limiter.annotation

import com.petarj123.ratelimiter.rate_limiter.model.FallbackStrategy


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class RateLimit(
    val maxRequests: Int = -1,
    val timeWindowSeconds: Int = -1,
    val whitelist: Array<String> = [],
    val blacklist: Array<String> = [],
    val fallback: FallbackStrategy = FallbackStrategy.ALLOW
)

