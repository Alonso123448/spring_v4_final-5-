package com.example.spring_v4.repository;

import com.example.spring_v4.entity.LogoImagen;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LogoImagenRepository extends JpaRepository<LogoImagen, Integer> {
    List<LogoImagen> findByActivoTrue();
}
