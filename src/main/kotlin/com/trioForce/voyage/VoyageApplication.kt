package com.trioForce.voyage

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class VoyageApplication

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    runApplication<VoyageApplication>(*args)
}
