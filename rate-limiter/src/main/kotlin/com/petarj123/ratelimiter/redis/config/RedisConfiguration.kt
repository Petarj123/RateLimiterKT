package com.petarj123.ratelimiter.redis.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

@Configuration
class RedisConfiguration(val properties: RedisProperties) {

    @Bean
    fun connectionFactory(): LettuceConnectionFactory {
        val redisStandAloneConfigurationProperties = RedisStandaloneConfiguration(properties.host, properties.port)
        val clientConfiguration: LettuceClientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(3000))
                .shutdownTimeout(Duration.ofMillis(100))
                .build()
        return LettuceConnectionFactory(redisStandAloneConfigurationProperties, clientConfiguration)
    }

    @Bean
    fun stringRedisTemplate(connectionFactory: LettuceConnectionFactory): StringRedisTemplate {
        val template = StringRedisTemplate()
        template.connectionFactory = connectionFactory
        return template
    }
}