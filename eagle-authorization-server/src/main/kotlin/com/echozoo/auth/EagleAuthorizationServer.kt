package com.echozoo.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EagleAuthorizationServer

fun main(args: Array<String>) {
    runApplication<EagleAuthorizationServer>(*args)
}
