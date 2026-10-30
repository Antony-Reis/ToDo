package com.antony.todo.service;

import com.antony.todo.database.model.ToDoEntity;
import com.antony.todo.database.model.UserEntity;
import com.antony.todo.database.repository.ToDoRepository;
import com.antony.todo.utils.MessageDto;
import com.antony.todo.utils.ToDoDto;
import com.antony.todo.utils.ToDoResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ToDoService {
    private final ToDoRepository toDoRepository;

    public ToDoService(ToDoRepository toDoRepository) {
        this.toDoRepository = toDoRepository;
    }

    private String getUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = (UserEntity) authentication.getPrincipal();
        return user.getId();
    }

    private boolean toDoOwnerIdEqualsAuthUserId(ToDoEntity toDo){
        if (toDo.getOwnerId() == getUserId()){return true;}
        return false;
    }

    @Transactional
    public MessageDto createToDo(ToDoDto body){
        String userId = getUserId();
        ToDoEntity toDo = new ToDoEntity(body.getTitle(), body.getDescription()
        , LocalDateTime.now(), userId);

        toDoRepository.save(toDo);

        return new MessageDto(HttpStatus.CREATED, "To-do created successfully");
    }

    private MessageDto softDeleteToDo(ToDoEntity toDo){
        toDo.setActive(false);
        toDoRepository.save(toDo);
        return new MessageDto(HttpStatus.OK, "To-do moved to trash");
    }

    private MessageDto hardDeleteToDo(ToDoEntity toDo){
         toDoRepository.delete(toDo);
         return new MessageDto(HttpStatus.OK, "To-do deleted successfully");
    }

    @Transactional
    public MessageDto deleteToDo(String toDoId){
        ToDoEntity toDo = toDoRepository.findById(toDoId).orElseThrow(RuntimeException::new);
        if (!toDoOwnerIdEqualsAuthUserId(toDo)){
            throw new RuntimeException();
        }

        if (toDo.getActive()){
            return softDeleteToDo(toDo);
        } else {
            return hardDeleteToDo(toDo);
        }
    }

    @Transactional
    public Page<ToDoResponseDto> getToDosPage(Integer page, Integer size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<ToDoEntity> toDos = toDoRepository.findByOwnerId(getUserId(), pageable);

        return toDos.map(entity -> new ToDoResponseDto(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getDone(),
                entity.getActive()
        ));
    }
    @Transactional
    public MessageDto changeIsDoneStatus(String toDoId){
        ToDoEntity toDo = toDoRepository.findById(toDoId).orElseThrow(RuntimeException::new);
        if (!toDoOwnerIdEqualsAuthUserId(toDo)){
            throw new RuntimeException();
        }
        toDo.setDone(!toDo.getDone());
        toDoRepository.save(toDo);

        return new MessageDto(HttpStatus.OK, ("To-do 'Done' status changed to: "+toDo.getDone()));
    }

    @Transactional
    public MessageDto updateToDo(String toDoId,ToDoDto toDo){
        ToDoEntity toDoEntity = toDoRepository.findById(toDoId).orElseThrow(RuntimeException::new);
        if (!toDoOwnerIdEqualsAuthUserId(toDoEntity)){
            throw new RuntimeException();
        }
        Optional.ofNullable(toDo.getTitle()).ifPresent(toDoEntity::setTitle);
        Optional.ofNullable(toDo.getDescription()).ifPresent(toDoEntity::setDescription);

        toDoRepository.save(toDoEntity);

        return new MessageDto(HttpStatus.OK, "To-do updated successfully");
    }
}
