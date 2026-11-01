package com.antony.todo.controller;

import com.antony.todo.service.ToDoService;
import com.antony.todo.utils.MessageDto;
import com.antony.todo.utils.ToDoDto;
import com.antony.todo.utils.ToDoResponseDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v1/todo")
@Validated
public class ToDoController {
    private final ToDoService toDoService;

    public ToDoController(ToDoService toDoService) {
        this.toDoService = toDoService;
    }

    @GetMapping
    public Page<ToDoResponseDto> getToDos(@RequestParam Integer page, @RequestParam Integer size){
        return toDoService.getToDosPage(page, size);
    }

    @PostMapping()
    public MessageDto postToDo(@Valid @RequestBody ToDoDto body) {
        return toDoService.createToDo(body);
    }

    @DeleteMapping("{toDoId}")
    public MessageDto deleteToDo(@PathVariable String toDoId) {
        return toDoService.deleteToDo(toDoId);
    }

    @PatchMapping("{toDoId}")
    public MessageDto changeToDoIsDoneStatus(@PathVariable String toDoId){
        return toDoService.changeIsDoneStatus(toDoId);
    }

    @PutMapping("{toDoId}")
    public MessageDto putToDo (@PathVariable String toDoId,@Valid @RequestBody ToDoDto body){
        return toDoService.updateToDo(toDoId, body);
    }

}
