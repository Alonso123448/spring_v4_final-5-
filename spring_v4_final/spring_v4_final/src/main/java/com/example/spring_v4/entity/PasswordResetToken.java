package com.example.spring_v4.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private LocalDateTime expiracion;

    public PasswordResetToken() {}

    public PasswordResetToken(String token, String username, LocalDateTime expiracion) {
        this.token = token;
        this.username = username;
        this.expiracion = expiracion;
    }

    public Integer getId() { return id; }
    public String getToken() { return token; }
    public String getUsername() { return username; }
    public LocalDateTime getExpiracion() { return expiracion; }
    public void setToken(String token) { this.token = token; }
    public void setUsername(String username) { this.username = username; }
    public void setExpiracion(LocalDateTime expiracion) { this.expiracion = expiracion; }
}
