package com.viniss.todo.service.exception

import java.util.UUID

class TodoListNotFoundException(listId: UUID) : RuntimeException(
    "Todo list $listId not found"
)
