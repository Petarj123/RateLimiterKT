package com.petarj123.ratelimiter.token_bucket

import com.petarj123.ratelimiter.interceptor.RateLimiterInterceptor
import com.petarj123.ratelimiter.rate_limiter.config.RateLimiterProperties
import com.petarj123.ratelimiter.rate_limiter.service.ClientSuspensionService
import io.lettuce.core.KillArgs.Builder.user
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.util.concurrent.TimeUnit


@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = ["rate-limiter.algorithm=TOKEN_BUCKET", "rate-limiter.default-bucket-capacity=1", "rate-limiter.suspension-threshold=10"])
class TokenBucketTest() {
    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var stringRedisTemplate: StringRedisTemplate
    @Autowired
    lateinit var rateLimiterProperties: RateLimiterProperties

    @AfterEach
    fun tearDown() {
        stringRedisTemplate.delete("bucket:127.0.0.1")
    }

    @Test
    fun `exceeds bucket capacity, should return status 429`() {
        repeat(10) {
            mockMvc.perform(get("/test/hi"))
        }
        mockMvc.perform(get("/test/hi"))
            .andExpect(status().isTooManyRequests)
    }
    @Test
    fun `does not exceed bucket capacity, should return status 200`() {
        repeat(10) {
            mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
        }
    }

    @Test
    fun `bucket refills at expected rate`() {
        // Exhaust the bucket initially
        repeat(10) {
            mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
        }

        // Ensure a request fails immediately after
        mockMvc.perform(get("/test/hi")).andExpect(status().isTooManyRequests)

        // Wait for a time equivalent to half the refill rate and check if you can make a request
        Thread.sleep(rateLimiterProperties.defaultBucketRefillTime / 2 * 1000)
        mockMvc.perform(get("/test/hi")).andExpect(status().isTooManyRequests)

        // Wait for another half, completing the refill duration, then check if a request passes
        Thread.sleep(rateLimiterProperties.defaultBucketRefillTime / 2 * 1000)
        mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
    }

    @Test
    fun `bucket refills partially and only allows equivalent number of requests`() {
        // Make initial requests to use half the tokens
        repeat(5) {
            mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
        }
        // Wait for a full refill duration
        Thread.sleep(rateLimiterProperties.defaultBucketRefillTime / 2 * 1000)
        // Ensure only 5 more requests can be made before hitting the rate limit
        repeat(5) {
            mockMvc.perform(get("/test/hi")).andExpect(status().isOk)
        }
        mockMvc.perform(get("/test/hi")).andExpect(status().isTooManyRequests)
    }

    @Test
    fun `exceeds suspension threshold, should return status 429`() {
        repeat(30) {
            mockMvc.perform(get("/test/hi"))
        }
        mockMvc.perform(get("/test/hi")).andExpect(status().isTooManyRequests)
    }
}