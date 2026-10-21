package com.example.spring_v4.repository;

import com.example.spring_v4.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);
    boolean existsByDocumento(String documento);
    // findFirst evita crash si existen duplicados en la BD
    java.util.Optional<Cliente> findFirstByDocumento(String documento);
}
