package com.petarj123.ratelimiter.rate_limiter.service

import com.petarj123.ratelimiter.rate_limiter.implementation.SuspensionService
import org.springframework.data.redis.core.StringRedisTemplate
import java.util.concurrent.TimeUnit


class ClientSuspensionService(private val stringRedisTemplate: StringRedisTemplate) : SuspensionService {
        override fun isSuspended(identifier: String): Boolean {
            return stringRedisTemplate.hasKey("suspended:${identifier}")
        }

        override fun suspend(identifier: String, durationInSeconds: Long) {
            val duration = if (durationInSeconds <= 0) 600 else durationInSeconds
            stringRedisTemplate.opsForValue().set("suspended:${identifier}", "true", duration, TimeUnit.SECONDS)
        }

        override fun removeSuspension(identifier: String) {
            stringRedisTemplate.delete("suspended:${identifier}")
        }

        override fun getRemainingSuspensionTime(identifier: String): Long {
            return stringRedisTemplate.getExpire("suspended:${identifier}")
        }
    }