package com.example.spring_v4.repository;

import com.example.spring_v4.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findByEstadoTrue();

    List<Producto> findByNombreIgnoreCase(String nombre);

    boolean existsByProveedorId(Integer proveedorId);

    // Agotados (stock = 0 o null, activos)
    long countByEstadoTrueAndStockLessThanEqual(Integer stock);

    // Stock bajo: stock > 0 pero <= stockMinimo (o <= 5 si no tiene mínimo)
    @Query("SELECT COUNT(p) FROM Producto p WHERE p.estado = true AND p.stock > 0 AND p.stock <= COALESCE(p.stockMinimo, 5)")
    long countStockBajo();

    // Vencidos: fechaVencimiento < hoy
    @Query("SELECT COUNT(p) FROM Producto p WHERE p.estado = true AND p.fechaVencimiento IS NOT NULL AND p.fechaVencimiento < :hoy")
    long countVencidos(@Param("hoy") LocalDate hoy);

    // Por vencer en 7 días
    @Query("SELECT COUNT(p) FROM Producto p WHERE p.estado = true AND p.fechaVencimiento IS NOT NULL AND p.fechaVencimiento >= :hoy AND p.fechaVencimiento <= :limite")
    long countPorVencer(@Param("hoy") LocalDate hoy, @Param("limite") LocalDate limite);
}
