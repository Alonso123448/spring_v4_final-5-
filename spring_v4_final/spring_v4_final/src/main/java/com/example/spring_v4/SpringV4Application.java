package com.example.spring_v4;

import com.example.spring_v4.entity.Rol;
import com.example.spring_v4.entity.Usuario;
import com.example.spring_v4.repository.RolRepository;
import com.example.spring_v4.repository.UsuarioRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashSet;

@SpringBootApplication
public class SpringV4Application {

    public static void main(String[] args) {
        SpringApplication.run(SpringV4Application.class, args);
    }

    @Bean
    CommandLineRunner init(RolRepository rolRepo, UsuarioRepository userRepo) {
        return args -> {

            
            // CREAR ROLES
            
            if (rolRepo.findFirstByNombre("SUPERADMIN").isEmpty()) {
                Rol r = new Rol();
                r.setNombre("SUPERADMIN");
                rolRepo.save(r);
            }

            if (rolRepo.findFirstByNombre("ADMIN").isEmpty()) {
                Rol r = new Rol();
                r.setNombre("ADMIN");
                rolRepo.save(r);
            }

            if (rolRepo.findFirstByNombre("EMPLEADO").isEmpty()) {
                Rol r = new Rol();
                r.setNombre("EMPLEADO");
                rolRepo.save(r);
            }


            // CREAR USUARIO INICIAL

            if (userRepo.findByUsername("superadmin").isEmpty()) {

                Usuario u = new Usuario();
                u.setUsername("superadmin");

                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                u.setPassword(encoder.encode("1234"));

                u.setNombre("Super Administrador");
                u.setEmail("superadmin@admin.com");
                u.setActivo(true);

                HashSet<Rol> roles = new HashSet<>();
                Rol superadminRol = rolRepo.findFirstByNombre("SUPERADMIN").get();
                roles.add(superadminRol);
                u.setRoles(roles);

                userRepo.save(u);
            }

            System.out.println("Roles y usuario inicial creados");
        };
    }
}