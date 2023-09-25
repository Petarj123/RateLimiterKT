package com.petarj123.ratelimiter.controller

import com.petarj123.ratelimiter.rate_limiter.annotation.RateLimit
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/test")
class TestController {

    @GetMapping("/hi")
    @RateLimit(maxRequests = 2, timeWindowSeconds = 60)
    fun hi(): String {
        return "hi"
    }
}