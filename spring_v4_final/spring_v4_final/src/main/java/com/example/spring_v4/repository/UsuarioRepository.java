package com.example.spring_v4.repository;

import com.example.spring_v4.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {


    // LOGIN

    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByEmail(String email);

    // =========================
    // VALIDAR DUPLICADOS
    // =========================
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByNombre(String nombre);

    // =========================
    // VALIDAR DUPLICADOS EN EDICIÓN
    // EXCLUYENDO EL MISMO ID
    // =========================
    boolean existsByUsernameAndIdNot(String username, Integer id);

    boolean existsByEmailAndIdNot(String email, Integer id);

    boolean existsByNombreAndIdNot(String nombre, Integer id);

    // Verificar si existe al menos un usuario con un rol específico (por nombre)
    boolean existsByRolesNombre(String rolNombre);

    // Verificar si existe al menos un usuario con un rol específico (por id)
    boolean existsByRolesId(Integer rolId);
}