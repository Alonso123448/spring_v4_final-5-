package com.example.spring_v4.repository;

import com.example.spring_v4.entity.Cuota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CuotaRepository extends JpaRepository<Cuota, Integer> {
    List<Cuota> findByVentaId(Integer ventaId);
}
