package com.antony.todo.security;

import com.antony.todo.database.model.UserEntity;
import com.antony.todo.database.repository.RefreshTokenRepository;
import com.antony.todo.database.repository.UserRepository;
import com.antony.todo.utils.MessageDto;
import com.antony.todo.utils.TokenDto;
import com.antony.todo.utils.UserDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, TokenService tokenService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
    }

    public MessageDto registerUser(UserDto body){
    if (userRepository.existsByEmail(body.getEmail())){
        throw new RuntimeException();
    }
    String encrypetdPassword = new BCryptPasswordEncoder().encode(body.getPassword());

    userRepository.save(new UserEntity(body.getEmail(), encrypetdPassword));

    return new MessageDto(HttpStatus.OK, "Account created successfully");
    }
    public TokenDto loginUser(UserDto body){
        if (!userRepository.existsByEmail(body.getEmail())){
            throw new RuntimeException();
        }

        var usernamePassword = new UsernamePasswordAuthenticationToken(body.getEmail(), body.getPassword());
        Authentication auth = this.authenticationManager.authenticate(usernamePassword);

        var user = (UserEntity) auth.getPrincipal();

        String accessToken = tokenService.generateAcessToken(user.getEmail());
        String refreshToken = tokenService.generateRefreshToken(user.getEmail());

        return new TokenDto(accessToken, refreshToken);
    }
    public TokenDto refreshTOken(String requestRefreshToken){
        if (!tokenService.validateRefreshToken(requestRefreshToken)){
            throw new RuntimeException();
            }
        String email = tokenService.getEmail(requestRefreshToken);

        tokenService.deleteRefreshToken(requestRefreshToken);

        String newAccessToken = tokenService.generateAcessToken(email);
        String newRefreshToken = tokenService.generateRefreshToken(email);

        return new TokenDto(newAccessToken, newRefreshToken);
    }

}
