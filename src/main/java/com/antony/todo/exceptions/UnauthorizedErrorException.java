package com.antony.todo.exceptions;

import org.springframework.security.core.AuthenticationException;

public class UnauthorizedErrorException extends AuthenticationException {
    public UnauthorizedErrorException() {
        super("Não autorizado");
    }
}
