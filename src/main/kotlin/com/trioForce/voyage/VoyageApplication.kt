package com.trioForce.voyage

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
class VoyageApplication

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    runApplication<VoyageApplication>(*args)
}
