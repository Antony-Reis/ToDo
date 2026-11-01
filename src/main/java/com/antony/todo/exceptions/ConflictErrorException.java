package com.antony.todo.exceptions;

public class ConflictErrorException extends RuntimeException{
    public ConflictErrorException() {
        super("Erro de conflito");
    }
}
