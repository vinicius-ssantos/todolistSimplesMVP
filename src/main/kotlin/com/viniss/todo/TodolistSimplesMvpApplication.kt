package com.viniss.todo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class TodolistSimplesMvpApplication

fun main(args: Array<String>) {
    runApplication<TodolistSimplesMvpApplication>(*args)
}
