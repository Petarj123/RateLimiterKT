package com.petarj123.ratelimiter.redis.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.redis")
class RedisProperties {
    lateinit var host: String
    var port: Int = 0
}