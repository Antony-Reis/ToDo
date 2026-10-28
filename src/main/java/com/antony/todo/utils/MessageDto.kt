package com.antony.todo.utils

import org.springframework.http.HttpStatus

data class MessageDto(
    val status: HttpStatus,
    val message: String
)
