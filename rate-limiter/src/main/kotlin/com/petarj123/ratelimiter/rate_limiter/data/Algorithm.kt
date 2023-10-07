package com.petarj123.ratelimiter.rate_limiter.data

enum class Algorithm(s: String) {
    SLIDING_WINDOW("sliding_window"),
    TOKEN_BUCKET("token_bucket"),
    LEAKY_BUCKET("leaky_bucket"),

}