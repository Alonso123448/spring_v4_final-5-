package com.example.spring_v4.repository;

import com.example.spring_v4.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {

    // Verificar nombre duplicado al crear
    boolean existsByNombreIgnoreCase(String nombre);

    // Verificar nombre duplicado al editar (excluye el propio registro)
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);
}