package com.example.spring_v4.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TwoFactorService {

    private record CodigoEntry(String codigo, LocalDateTime expiracion, int intentos) {}

    private final Map<String, CodigoEntry> codigos = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private static final int MAX_INTENTOS_2FA = 5;

    public String generarCodigo(String username) {
        String codigo = String.format("%06d", random.nextInt(1_000_000));
        codigos.put(username, new CodigoEntry(codigo, LocalDateTime.now().plusMinutes(5), 0));
        return codigo;
    }

    public boolean verificarCodigo(String username, String codigo) {
        CodigoEntry entry = codigos.get(username);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiracion())) {
            codigos.remove(username);
            return false;
        }
        if (entry.intentos() >= MAX_INTENTOS_2FA) {
            codigos.remove(username);
            return false;
        }
        if (entry.codigo().equals(codigo)) {
            codigos.remove(username);
            return true;
        }
        // Incrementar intentos fallidos
        codigos.put(username, new CodigoEntry(entry.codigo(), entry.expiracion(), entry.intentos() + 1));
        return false;
    }

    public boolean estaAgotado(String username) {
        CodigoEntry entry = codigos.get(username);
        return entry != null && entry.intentos() >= MAX_INTENTOS_2FA;
    }

    public void limpiar(String username) {
        codigos.remove(username);
    }
}
