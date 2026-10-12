package com.communityvoice.backend

import com.communityvoice.backend.config.AppProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class CommunityVoiceApplication

fun main(args: Array<String>) {
    runApplication<CommunityVoiceApplication>(*args)
}
