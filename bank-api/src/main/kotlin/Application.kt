package com.example.bank

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.example.bank"])
class BankApplication

fun main(args: Array<String>) {
    runApplication<BankApplication>(*args)
}