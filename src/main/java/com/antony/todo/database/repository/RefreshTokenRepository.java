package com.antony.todo.database.repository;

import com.antony.todo.database.model.RefreshTokenEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends MongoRepository<RefreshTokenEntity, String> {
    public Optional<RefreshTokenEntity> findByToken(String token);
}
