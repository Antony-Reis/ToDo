package com.antony.todo.service;

import com.antony.todo.database.model.RefreshTokenEntity;
import com.antony.todo.database.model.UserEntity;
import com.antony.todo.database.repository.RefreshTokenRepository;
import com.antony.todo.database.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Service
public class TokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private SecretKey secret = Keys.hmacShaKeyFor(Base64.getDecoder().decode("YTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI="));

    public TokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    private String generateToken(String email, String type, int duration){
        return Jwts.builder().subject(email).issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(duration, ChronoUnit.MINUTES)))
                .claim("type", type).signWith(secret).compact();
    }
    public String generateAcessToken(String email){
        return generateToken(email, "access", 15);
    }

    public String generateRefreshToken(String email){
        String token = generateToken(email, "refresh", 21600); // 21600 minutes = 15 days
        UserEntity user = userRepository.findByEmail(email).orElse(null);
        Instant expirationInstant = Instant.now().plus(15, ChronoUnit.DAYS);

        RefreshTokenEntity refreshToken = new RefreshTokenEntity(user.getId(), token, expirationInstant);

        refreshTokenRepository.save(refreshToken);
        return token;
    }
    public boolean validateRefreshToken(String token){
        try{
            if (!"refresh".equals(getType(token))){return false;}

            String email = getEmail(token);

            RefreshTokenEntity refreshToken = refreshTokenRepository.findByToken(token).orElse(null);
            if (refreshToken == null){return false;}

            return !isTokenExpired(token) && refreshToken.getExpiryDate().isAfter(Instant.now());
        } catch (Exception e){return false;}
    }

    public void deleteRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    public String getEmail(String token){
        return Jwts.parser().verifyWith(secret).build().parseSignedClaims(token).getPayload().getSubject();
    }

    private String getType(String token){
        return Jwts.parser().verifyWith(secret).build().parseSignedClaims(token).getPayload().get("type", String.class);
    }

    public Boolean isTokenExpired(String token){
        try{
        Date expiration = Jwts.parser().verifyWith(secret)
                .build().parseSignedClaims(token).getPayload().getExpiration();

        return expiration.before(new Date());}
        catch (Exception e){
            return false;
        }
    }

    public Boolean validateAccessToken(String token, UserDetails user){
        try{
        final String email = getEmail(token);
        return (email.equals(user.getUsername()) && !isTokenExpired(token));}
        catch (Exception e){
            return false;
        }
    }
}
