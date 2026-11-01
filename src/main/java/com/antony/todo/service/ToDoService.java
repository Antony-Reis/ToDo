package com.antony.todo.service;

import com.antony.todo.database.model.ToDoEntity;
import com.antony.todo.database.model.UserEntity;
import com.antony.todo.database.repository.ToDoRepository;
import com.antony.todo.database.repository.UserRepository;
import com.antony.todo.exceptions.NotFoundErrorException;
import com.antony.todo.exceptions.UnauthorizedErrorException;
import com.antony.todo.security.UserPrincipal;
import com.antony.todo.utils.MessageDto;
import com.antony.todo.utils.ToDoDto;
import com.antony.todo.utils.ToDoResponseDto;
import org.apache.catalina.User;
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
import java.util.Objects;
import java.util.Optional;

@Service
public class ToDoService {
    private final ToDoRepository toDoRepository;
    private final UserRepository userRepository;

    public ToDoService(ToDoRepository toDoRepository, UserRepository userRepository) {
        this.toDoRepository = toDoRepository;
        this.userRepository = userRepository;
    }

    private String getUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = ((UserPrincipal) authentication.getPrincipal()).getUsername();
        UserEntity user = userRepository.findByEmail(email).orElse(null);
        return user.getId();
    }

    private boolean toDoOwnerIdEqualsAuthUserId(ToDoEntity toDo){
        if (Objects.equals(toDo.getOwnerId(), getUserId())){return true;}
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
    public MessageDto deleteToDo(String toDoId) throws UnauthorizedErrorException, NotFoundErrorException{
        ToDoEntity toDo = toDoRepository.findById(toDoId).orElseThrow(NotFoundErrorException::new);
        if (!toDoOwnerIdEqualsAuthUserId(toDo)){
            throw new UnauthorizedErrorException();
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
    public MessageDto changeIsDoneStatus(String toDoId) throws UnauthorizedErrorException, NotFoundErrorException{
        ToDoEntity toDo = toDoRepository.findById(toDoId).orElseThrow(NotFoundErrorException::new);
        if (!toDoOwnerIdEqualsAuthUserId(toDo)){
            throw new UnauthorizedErrorException();
        }
        toDo.setDone(!toDo.getDone());
        toDoRepository.save(toDo);

        return new MessageDto(HttpStatus.OK, ("To-do 'Done' status changed to: "+toDo.getDone()));
    }

    @Transactional
    public MessageDto updateToDo(String toDoId,ToDoDto toDo) throws UnauthorizedErrorException, NotFoundErrorException{
        ToDoEntity toDoEntity = toDoRepository.findById(toDoId).orElseThrow(NotFoundErrorException::new);
        if (!toDoOwnerIdEqualsAuthUserId(toDoEntity)){
            throw new UnauthorizedErrorException();
        }
        Optional.ofNullable(toDo.getTitle()).ifPresent(toDoEntity::setTitle);
        Optional.ofNullable(toDo.getDescription()).ifPresent(toDoEntity::setDescription);

        toDoRepository.save(toDoEntity);

        return new MessageDto(HttpStatus.OK, "To-do updated successfully");
    }
}
