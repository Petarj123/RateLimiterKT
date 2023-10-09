package com.petarj123.ratelimiter.fixed_window

import com.petarj123.ratelimiter.rate_limiter.config.RateLimiterProperties
import com.petarj123.ratelimiter.rate_limiter.data.FallbackStrategy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.concurrent.TimeUnit


@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = ["rate-limiter.algorithm=FIXED_WINDOW", "rate-limiter.default-max-requests=10"])
class FixedWindowTest {

    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var stringRedisTemplate: StringRedisTemplate
    @Autowired
    lateinit var rateLimiterProperties: RateLimiterProperties

    private val logger: Logger = LoggerFactory.getLogger(FixedWindowTest::class.java)


    @Test
    fun `should exceed max requests and return status 429`() {
        repeat(10) {
            mockMvc.perform(get("/test/hi"))
        }
        mockMvc.perform(get("/test/hi")).andExpect(status().isTooManyRequests)
        resetTTL()
    }
    @Test
    fun `should reach max requests and return status 200`() {
        repeat(10) {
            val result = mockMvc.perform(get("/test/hi"))
            logger.info("HTTP Status: " + result.andReturn().response.status)
            result.andExpect(status().isOk)
        }
        resetTTL()
    }
    @Test
    fun `blacklist user, should return status 403`() {
        rateLimiterProperties.blacklistedIps = setOf("127.0.0.1")

        try {
            mockMvc.perform(get("/test/hi"))
                .andExpect(status().isForbidden())
        } finally {
            rateLimiterProperties.blacklistedIps = setOf()
        }
        resetTTL()
    }
    @Test
    fun `whitelist user, exceed max requests, should return status 200`() {
        rateLimiterProperties.whitelistedIps = setOf("127.0.0.1")

        repeat(20) {
            mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
        }
        rateLimiterProperties.whitelistedIps = setOf()
        resetTTL()
    }

    @Test
    fun `hit max requests, wait for them to refresh, try again, should return status 200`() {
        rateLimiterProperties.defaultTimeWindowSeconds = 5

        repeat(10) {
            mockMvc.perform(get("/test/hi"))
        }
        Thread.sleep(6000)

        repeat(10) {
            mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
        }

        rateLimiterProperties.defaultTimeWindowSeconds = 60
        resetTTL()
    }
    @Test
    fun `start sending requests second before max requests refresh, should return true if any request returns status 200`() {
        rateLimiterProperties.defaultTimeWindowSeconds = 5

        resetTTL()

        repeat(10) {
            mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
        }

        Thread.sleep(4000)

        var count = 0
        repeat(10) {
            Thread.sleep(500)
            val resultStatus = mockMvc.perform(get("/test/hi")).andReturn().response.status
            if (resultStatus == 200) {
                count++
            }
        }

        assertTrue(count >= 1)

        rateLimiterProperties.defaultTimeWindowSeconds = 60
        resetTTL()
    }

    private fun resetTTL() {
        stringRedisTemplate.expire("rate_limit:127.0.0.1", 0, TimeUnit.SECONDS)
    }
}