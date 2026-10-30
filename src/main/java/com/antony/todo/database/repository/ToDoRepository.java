package com.antony.todo.database.repository;

import com.antony.todo.database.model.ToDoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ToDoRepository extends MongoRepository<ToDoEntity, String> {
    Page<ToDoEntity> findByOwnerId(String ownerId, Pageable pageable);
}
