package com.antony.todo.database.model;

import org.springframework.cglib.core.Local;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;

@Document(collection = "refresh_token")
public class RefreshTokenEntity {
    @Id
    private String id;

    private Instant expiryDate;

    private String token;

    private String ownerId;

    public RefreshTokenEntity() {
    }

    public RefreshTokenEntity(String ownerId, String token, Instant expiryDate) {
        this.ownerId = ownerId;
        this.token = token;
        this.expiryDate = expiryDate;
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }
}
