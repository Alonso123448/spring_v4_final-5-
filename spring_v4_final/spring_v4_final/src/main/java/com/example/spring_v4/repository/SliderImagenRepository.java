package com.example.spring_v4.repository;

import com.example.spring_v4.entity.SliderImagen;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SliderImagenRepository extends JpaRepository<SliderImagen, Integer> {
    List<SliderImagen> findByActivoTrue();
}
