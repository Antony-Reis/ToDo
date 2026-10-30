package com.antony.todo.database.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "todo")
public class ToDoEntity {
    @Id
    String id;

    @NotBlank(message = "Title cannot be blank")
    String title;

    String description = "";

    LocalDateTime createdAt = LocalDateTime.now();

    Boolean isDone = false;

    Boolean active = true;

    @NotNull
    String ownerId;

    public ToDoEntity() {
    }

    public ToDoEntity(String title, String description, LocalDateTime createdAt, String ownerId) {
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
        this.ownerId = ownerId;
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getDone() {
        return isDone;
    }

    public void setDone(Boolean done) {
        isDone = done;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
