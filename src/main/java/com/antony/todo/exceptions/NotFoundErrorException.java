package com.antony.todo.exceptions;

public class NotFoundErrorException extends RuntimeException{
    public NotFoundErrorException() {
    super("Não encontrado");
    }
}
