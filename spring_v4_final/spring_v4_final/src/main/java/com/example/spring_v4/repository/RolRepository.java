package com.example.spring_v4.repository;

import com.example.spring_v4.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findFirstByNombre(String nombre);

    // Verificar duplicado de nombre excluyendo el propio registro (para edición)
    boolean existsByNombreAndIdNot(String nombre, Integer id);

    // Verificar duplicado al crear (sin id previo)
    boolean existsByNombre(String nombre);

    // Solo roles activos (para el select de usuarios)
    List<Rol> findByEstadoTrue();

}