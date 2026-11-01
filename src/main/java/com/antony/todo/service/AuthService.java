package com.antony.todo.service;

import com.antony.todo.database.model.UserEntity;
import com.antony.todo.database.repository.RefreshTokenRepository;
import com.antony.todo.database.repository.UserRepository;
import com.antony.todo.exceptions.ConflictErrorException;
import com.antony.todo.exceptions.NotFoundErrorException;
import com.antony.todo.security.UserPrincipal;
import com.antony.todo.utils.MessageDto;
import com.antony.todo.utils.TokenDto;
import com.antony.todo.utils.UserDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final AuthenticationConfiguration authenticationConfiguration;

    public AuthService(UserRepository userRepository, TokenService tokenService, TokenService tokenService1, AuthenticationConfiguration authenticationConfiguration) {
        this.userRepository = userRepository;
        this.tokenService = tokenService1;
        this.authenticationConfiguration = authenticationConfiguration;
    }

    public MessageDto registerUser(UserDto body) throws ConflictErrorException {
    if (userRepository.existsByEmail(body.getEmail())){
        throw new ConflictErrorException();
    }
    String encrypetdPassword = new BCryptPasswordEncoder().encode(body.getPassword());

    userRepository.save(new UserEntity(body.getEmail(), encrypetdPassword));

    return new MessageDto(HttpStatus.CREATED, "Account created successfully");
    }
    public TokenDto loginUser(UserDto body) throws NotFoundErrorException{
        if (!userRepository.existsByEmail(body.getEmail())){
            throw new NotFoundErrorException();
        }

        var usernamePassword = new UsernamePasswordAuthenticationToken(body.getEmail(), body.getPassword());
        Authentication auth = authenticationConfiguration.getAuthenticationManager().authenticate(usernamePassword);

        var user = (UserPrincipal) auth.getPrincipal();

        String accessToken = tokenService.generateAcessToken(user.getUsername());
        String refreshToken = tokenService.generateRefreshToken(user.getUsername());

        return new TokenDto(accessToken, refreshToken);
    }
    public TokenDto refreshToken(String requestRefreshToken) throws ConflictErrorException{
        if (!tokenService.validateRefreshToken(requestRefreshToken)){
            throw new ConflictErrorException();
            }
        String email = tokenService.getEmail(requestRefreshToken);

        tokenService.deleteRefreshToken(requestRefreshToken);

        String newAccessToken = tokenService.generateAcessToken(email);
        String newRefreshToken = tokenService.generateRefreshToken(email);

        return new TokenDto(newAccessToken, newRefreshToken);
    }

}
