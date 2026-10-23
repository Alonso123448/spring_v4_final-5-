package com.example.spring_v4.service;

import com.example.spring_v4.entity.Usuario;
import com.example.spring_v4.repository.UsuarioRepository;

import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository repo;

    public UsuarioService(UsuarioRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        Usuario usuario = repo.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuario no encontrado"));

        List<SimpleGrantedAuthority> authorities = Stream.concat(

                // ROLES
                usuario.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority(
                                "ROLE_" + r.getNombre()
                        )),

                // PERMISOS
                usuario.getRoles().stream()
                        .flatMap(r -> r.getPermisos().stream())
                        .map(p -> new SimpleGrantedAuthority(
                                p.getNombre()
                        ))

        ).collect(Collectors.toList());

        return new User(
                usuario.getUsername(),
                usuario.getPassword(),
                !Boolean.FALSE.equals(usuario.getActivo()),   // null se trata como activo (valor por defecto)
                true,
                true,
                !usuario.getBloqueado(),   // accountNonLocked
                authorities
        );
    }
}