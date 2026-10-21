package com.example.spring_v4.repository;

import com.example.spring_v4.entity.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;



public interface PermisoRepository extends JpaRepository<Permiso, Integer> {
}