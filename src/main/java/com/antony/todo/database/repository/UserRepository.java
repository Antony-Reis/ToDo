package com.antony.todo.database.repository;

import com.antony.todo.database.model.UserEntity;
import jakarta.validation.constraints.Email;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<UserEntity, String> {
    public Optional<UserEntity> findByEmail(String email);

    public boolean existsByEmail(String email);
}
