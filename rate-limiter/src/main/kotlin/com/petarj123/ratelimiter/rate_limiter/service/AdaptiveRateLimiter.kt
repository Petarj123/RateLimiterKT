package com.petarj123.ratelimiter.rate_limiter.service

import com.petarj123.ratelimiter.rate_limiter.annotation.RateLimit
import com.petarj123.ratelimiter.rate_limiter.config.RateLimiterProperties
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import java.util.stream.Collectors

class AdaptiveRateLimiter
    (
    private val meterRegistry: MeterRegistry,
    private val listableBeanFactory: ListableBeanFactory,
    private val rateLimiterProperties: RateLimiterProperties,
    private val endpointRateLimits: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
) {

    fun adjustRateLimitBasedOnSystemLoad(endpoint: String?) {
        val systemLoad = meterRegistry["system.cpu.usage"].gauge().value()

        // Get the default or current rate limit for the endpoint
        var currentRateLimit =
            endpointRateLimits.getOrDefault(endpoint!!, rateLimiterProperties.defaultMaxRequests)
        if (rateLimiterProperties.decreaseOnHighLoad && systemLoad > rateLimiterProperties.highLoadThreshold) {
            currentRateLimit = (currentRateLimit * rateLimiterProperties.highLoadMultiplier).toInt()
        } else if (rateLimiterProperties.increaseOnLowLoad && systemLoad < rateLimiterProperties.lowLoadThreshold) {
            currentRateLimit = (currentRateLimit * rateLimiterProperties.lowLoadMultiplier).toInt()
        }

        endpointRateLimits[endpoint] = currentRateLimit
    }

    fun getCurrentRateLimit(endpoint: String?): Int {
        return endpointRateLimits.getOrDefault(endpoint!!, rateLimiterProperties.defaultMaxRequests)
    }

    @PostConstruct
    fun initializeRateLimits() {
        // Fetching all beans of type Object and filter by Controller and RestController annotations
        val beans = listableBeanFactory.getBeansOfType(Any::class.java).values.stream()
            .filter { bean: Any ->
                bean.javaClass.isAnnotationPresent(Controller::class.java) || bean.javaClass.isAnnotationPresent(
                    RestController::class.java
                )
            }
            .collect(Collectors.toMap(
                Function { obj: Any -> obj.toString() }, Function.identity()
            )
            )
        for (bean in beans.values) {
            for (method in bean.javaClass.getDeclaredMethods()) {
                // Exclude unwanted methods(Some spring default methods)
                if (method.name == "error" || method.name == "errorHtml") {
                    continue
                }

                // Check if method has Spring's mapping annotations
                if (isRequestMappingAnnotated(method)) {
                    if (method.isAnnotationPresent(RateLimit::class.java)) {
                        endpointRateLimits[method.name] = method.getAnnotation(RateLimit::class.java).maxRequests
                    } else {
                        // For methods without the RateLimit annotation
                        endpointRateLimits.putIfAbsent(method.name, rateLimiterProperties.defaultMaxRequests)
                    }
                }
            }
        }
    }

    private fun isRequestMappingAnnotated(method: Method): Boolean {
        return method.isAnnotationPresent(RequestMapping::class.java) ||
                method.isAnnotationPresent(GetMapping::class.java) ||
                method.isAnnotationPresent(PostMapping::class.java) ||
                method.isAnnotationPresent(PutMapping::class.java) ||
                method.isAnnotationPresent(DeleteMapping::class.java) ||
                method.isAnnotationPresent(PatchMapping::class.java)
    }
}