package com.antony.todo.utils

import com.antony.todo.database.model.ToDoEntity
import java.time.LocalDateTime

data class ToDoResponseDto(
    val id : String,
    val title : String,
    val description : String,
    val createdAt : LocalDateTime,
    val isDone: Boolean,
    val active: Boolean
)
