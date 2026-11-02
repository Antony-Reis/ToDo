package com.antony.todo.service;

import com.antony.todo.database.model.UserEntity;
import com.antony.todo.database.repository.UserRepository;
import com.antony.todo.exceptions.ConflictErrorException;
import com.antony.todo.exceptions.NotFoundErrorException;
import com.antony.todo.security.UserPrincipal;
import com.antony.todo.utils.MessageDto;
import com.antony.todo.utils.TokenDto;
import com.antony.todo.utils.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    private static final String EMAIL = "user@test.com";
    private static final String PASSWORD = "secret";
    private static final String REFRESH_TOKEN = "refresh-token";

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        authService = new AuthService(userRepository, tokenService, tokenService, authenticationConfiguration);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);
    }

    // registerUser

    @Test
    void registerUser_success_encodesPasswordAndSavesUser() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);

        MessageDto result = authService.registerUser(new UserDto(EMAIL, PASSWORD));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity savedUser = captor.getValue();

        assertEquals(EMAIL, savedUser.getEmail());
        assertNotEquals(PASSWORD, savedUser.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches(PASSWORD, savedUser.getPassword()));
        assertEquals(HttpStatus.CREATED, result.getStatus());
        assertEquals("Account created successfully", result.getMessage());
    }

    @Test
    void registerUser_emailAlreadyExists_throwsConflictAndDoesNotSave() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThrows(ConflictErrorException.class,
                () -> authService.registerUser(new UserDto(EMAIL, PASSWORD)));

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    // loginUser

    @Test
    void loginUser_success_authenticatesAndReturnsTokens() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        Authentication auth = mock(Authentication.class);
        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        when(userPrincipal.getUsername()).thenReturn(EMAIL);
        when(auth.getPrincipal()).thenReturn(userPrincipal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        when(tokenService.generateAcessToken(EMAIL)).thenReturn("access-token-123");
        when(tokenService.generateRefreshToken(EMAIL)).thenReturn("refresh-token-123");

        TokenDto tokens = authService.loginUser(new UserDto(EMAIL, PASSWORD));

        assertEquals("access-token-123", tokens.getToken());
        assertEquals("refresh-token-123", tokens.getRefreshToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService).generateAcessToken(EMAIL);
        verify(tokenService).generateRefreshToken(EMAIL);
    }

    @Test
    void loginUser_unknownEmail_throwsNotFoundAndDoesNotAuthenticate() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);

        assertThrows(NotFoundErrorException.class,
                () -> authService.loginUser(new UserDto(EMAIL, PASSWORD)));

        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(tokenService);
    }

    @Test
    void loginUser_wrongPassword_propagatesBadCredentialsAndGeneratesNoTokens() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authService.loginUser(new UserDto(EMAIL, PASSWORD)));

        verifyNoInteractions(tokenService);
    }

    // refreshToken

    @Test
    void refreshToken_success_deletesOldTokenAndGeneratesNewTokens() {
        when(tokenService.validateRefreshToken(REFRESH_TOKEN)).thenReturn(true);
        when(tokenService.getEmail(REFRESH_TOKEN)).thenReturn(EMAIL);
        when(tokenService.generateAcessToken(EMAIL)).thenReturn("new-access-token");
        when(tokenService.generateRefreshToken(EMAIL)).thenReturn("new-refresh-token");

        TokenDto tokens = authService.refreshToken(REFRESH_TOKEN);

        assertEquals("new-access-token", tokens.getToken());
        assertEquals("new-refresh-token", tokens.getRefreshToken());
        verify(tokenService).deleteRefreshToken(REFRESH_TOKEN);
        verify(tokenService).generateAcessToken(EMAIL);
        verify(tokenService).generateRefreshToken(EMAIL);
    }

    @Test
    void refreshToken_invalidToken_throwsConflictAndDoesNotRotateTokens() {
        when(tokenService.validateRefreshToken(REFRESH_TOKEN)).thenReturn(false);

        assertThrows(ConflictErrorException.class, () -> authService.refreshToken(REFRESH_TOKEN));

        verify(tokenService, never()).deleteRefreshToken(anyString());
        verify(tokenService, never()).generateAcessToken(anyString());
        verify(tokenService, never()).generateRefreshToken(anyString());
    }
}
