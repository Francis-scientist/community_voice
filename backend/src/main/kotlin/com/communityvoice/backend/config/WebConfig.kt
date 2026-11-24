package com.communityvoice.backend.config

import com.communityvoice.backend.security.CurrentUserArgumentResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val currentUserArgumentResolver: CurrentUserArgumentResolver,
    private val properties: AppProperties
) : WebMvcConfigurer {

    /**
     * Hashes passwords with BCrypt.
     *
     * BCrypt is deliberately slow and salts every hash, so two people with the
     * same password get different stored values and guessing one by brute
     * force is expensive. We never store or log the password itself.
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    /** Makes `@CurrentUser` work on controller methods. */
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentUserArgumentResolver)
    }

    /**
     * Lets a browser-based front end call this API. The Android app does not
     * need any of this - CORS is a browser rule - but it costs nothing to
     * allow and means a web dashboard works against the same server.
     */
    override fun addCorsMappings(registry: CorsRegistry) {
        val origins = properties.corsAllowedOrigins
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf("*") }

        registry.addMapping("/api/**")
            // allowedOriginPatterns (rather than allowedOrigins) is what
            // permits the "*" wildcard alongside credentials.
            .allowedOriginPatterns(*origins.toTypedArray())
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .maxAge(3600)
    }
}
