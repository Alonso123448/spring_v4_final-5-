package com.example.spring_v4.service;

import com.example.spring_v4.entity.PasswordResetToken;
import com.example.spring_v4.repository.PasswordResetTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepo;

    public PasswordResetService(PasswordResetTokenRepository tokenRepo) {
        this.tokenRepo = tokenRepo;
    }

    public String generarToken(String username) {
        // Eliminar tokens anteriores del mismo usuario y tokens expirados
        tokenRepo.deleteByUsername(username);
        tokenRepo.deleteExpired(LocalDateTime.now());

        String token = UUID.randomUUID().toString();
        tokenRepo.save(new PasswordResetToken(token, username, LocalDateTime.now().plusMinutes(30)));
        return token;
    }

    public String getUsernameByToken(String token) {
        return tokenRepo.findByToken(token).map(entry -> {
            if (LocalDateTime.now().isAfter(entry.getExpiracion())) {
                tokenRepo.delete(entry);
                return null;
            }
            return entry.getUsername();
        }).orElse(null);
    }

    public void invalidarToken(String token) {
        tokenRepo.findByToken(token).ifPresent(tokenRepo::delete);
    }
}
