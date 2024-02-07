package com.echozoo.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EagleAuthorizationServerApplication

fun main(args: Array<String>) {
    runApplication<EagleAuthorizationServerApplication>(*args)
}
