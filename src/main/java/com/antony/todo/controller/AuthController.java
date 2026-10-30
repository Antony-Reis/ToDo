package com.antony.todo.controller;

import com.antony.todo.service.AuthService;
import com.antony.todo.utils.MessageDto;
import com.antony.todo.utils.TokenDto;
import com.antony.todo.utils.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@Validated
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDto registerUser(@Valid @RequestBody UserDto body){
        return authService.registerUser(body);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public TokenDto loginUser(@Valid @RequestBody UserDto body){
        return authService.loginUser(body);
    }

    @GetMapping("/refresh/{token}")
    @ResponseStatus(HttpStatus.OK)
    public TokenDto refreshToken(@PathVariable String token){
        return authService.refreshToken(token);
    }
}
