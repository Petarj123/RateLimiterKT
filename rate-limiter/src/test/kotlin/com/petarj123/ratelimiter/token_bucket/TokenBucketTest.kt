package com.petarj123.ratelimiter.token_bucket

import com.petarj123.ratelimiter.interceptor.RateLimiterInterceptor
import com.petarj123.ratelimiter.rate_limiter.service.ClientSuspensionService
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


@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = ["rate-limiter.algorithm=LEAKY_BUCKET", "rate-limiter.default-bucket-capacity=10"])
class TokenBucketTest() {
    @Autowired
    lateinit var mockMvc: MockMvc


    @Test
    fun `test token bucket`() {
        repeat(100) {
            mockMvc.perform(get("/test/hi"))
        }
        mockMvc.perform(get("/test/hi"))
            .andExpect(status().isTooManyRequests)
    }


}