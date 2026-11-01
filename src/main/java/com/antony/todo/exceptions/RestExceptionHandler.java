package com.antony.todo.exceptions;

import com.antony.todo.utils.MessageDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(UnauthorizedErrorException.class)
    public ResponseEntity<MessageDto> unauthorizedErrorException (UnauthorizedErrorException ex){
        MessageDto response = new MessageDto(HttpStatus.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NotFoundErrorException.class)
    public ResponseEntity<MessageDto> notFoundErrorException(NotFoundErrorException ex) {
        MessageDto response = new MessageDto(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ConflictErrorException.class)
    public ResponseEntity<MessageDto> conflictFoundErrorException(ConflictErrorException ex){
        MessageDto response = new MessageDto(HttpStatus.CONFLICT, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}
