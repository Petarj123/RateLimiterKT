package com.petarj123.ratelimiter.leaky_bucket

import com.petarj123.ratelimiter.rate_limiter.config.RateLimiterProperties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = ["rate-limiter.algorithm=LEAKY_BUCKET", "rate-limiter.default-bucket-capacity=1", "rate-limiter.default-drip-rate=10", "rate-limiter.suspension-threshold=10", "rate-limiter.suspension-duration=60"])
class LeakyTokenBucketTest {
    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var stringRedisTemplate: StringRedisTemplate
    @Autowired
    lateinit var rateLimiterProperties: RateLimiterProperties

    @AfterEach
    fun tearDown() {
        stringRedisTemplate.delete("leaky:127.0.0.1")
    }

    @Test
    fun `exceeds bucket capacity immediately, should return status 429`() {
        repeat(10) {  // Exhaust the capacity
            mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
        }
        mockMvc.perform(get("/test/hi"))
            .andExpect(status().isTooManyRequests)
    }

    @Test
    fun `does not exceed bucket capacity immediately, should return status 200`() {
        repeat(10) {
            mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
        }
    }

    @Test
    fun `bucket drips at expected rate`() {
        repeat(5) {
            mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
        }

        // Wait for tokens to drip (restore) using the drip rate
        Thread.sleep((rateLimiterProperties.defaultDripRate * 1000 * 5).toLong())

        // After waiting for 5 seconds (defaultDripRate = 1 token/sec), we should have 5 more tokens
        repeat(5) {
            mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
        }
    }

    @Test
    fun `ensure long waits restore the bucket`() {
        repeat(10) {
            mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
        }

        // Wait for the entire bucket to fill up
        Thread.sleep((rateLimiterProperties.defaultBucketCapacity * rateLimiterProperties.defaultDripRate * 1000).toLong())

        // The bucket should be full again
        repeat(10) {
            mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
        }
    }

    @Test
    fun `intermittent requests respect the drip rate`() {
        repeat(5) {
            mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
            Thread.sleep((rateLimiterProperties.defaultDripRate * 1000).toLong())
        }
    }

    @Test
    fun `ensure tokens do not drip if bucket is not used`() {
        // The idea here is to verify that unused tokens don't lead to "overflow", even if we wait a long time.
        Thread.sleep((60000).toLong()) // Double the required time

        repeat(10) {
            mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
        }
    }

    @Test
    fun `verify rate limiter respects IP whitelist`() {
        rateLimiterProperties.whitelistedIps = setOf("127.0.0.1")

        repeat(20) {
            mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
        }
    }

    @Test
    fun `verify rate limiter respects IP blacklist`() {
        rateLimiterProperties.blacklistedIps = setOf("127.0.0.1")

        mockMvc.perform(get("/test/hi")).andExpect(status().isForbidden)
    }

    @Test
    fun `exceeds suspension threshold, should return status 429`() {
        repeat(30) {
            mockMvc.perform(get("/test/hi"))
        }
        mockMvc.perform(get("/test/hi")).andExpect(status().isTooManyRequests)
    }
}