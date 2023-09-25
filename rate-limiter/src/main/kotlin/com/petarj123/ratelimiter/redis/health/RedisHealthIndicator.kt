package com.petarj123.ratelimiter.redis.health

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.util.*

class RedisHealthIndicator(private val stringRedisTemplate: StringRedisTemplate) : HealthIndicator {
    // TODO: Expand this
    public override fun health(): Health? {
        return try {
            Objects.requireNonNull(stringRedisTemplate.connectionFactory)?.connection?.ping()
            val lettuceConnectionFactory = stringRedisTemplate.connectionFactory as LettuceConnectionFactory?
            val configuration = lettuceConnectionFactory!!.standaloneConfiguration
            val port = configuration.port
            val host = configuration.hostName
            val database = configuration.database
            val serverVersion = Objects.requireNonNull(
                stringRedisTemplate.connectionFactory!!.connection.serverCommands().info("server")
            )?.getProperty("redis_version")
            val memoryInfo = stringRedisTemplate.connectionFactory!!.connection.serverCommands().info("memory")!!
            val usedMemory = memoryInfo.getProperty("used_memory")
            val usedMemoryHuman = memoryInfo.getProperty("used_memory_human")
            Health.up()
                .withDetail("host", host)
                .withDetail("port", port)
                .withDetail("database", database)
                .withDetail("version", serverVersion)
                .withDetail("usedMemory", usedMemory)
                .withDetail("usedMemoryHuman", usedMemoryHuman)
                .build()
        } catch (e: Exception) {
            Health.down().withDetail("Error", e.message + " " + e.cause).build()
        }
    }

    override fun getHealth(includeDetails: Boolean): Health? {
        val health = health()
        return if (!includeDetails) {
            Health.status(health()!!.status.code).build()
        } else health
    }
}