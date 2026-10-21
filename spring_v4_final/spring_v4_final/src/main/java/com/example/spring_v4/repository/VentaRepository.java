package com.example.spring_v4.repository;

import com.example.spring_v4.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Integer> {
    long countBySerie(String serie);
    List<Venta> findTop10ByClienteDocumentoStartingWith(String prefix);

    // Total vendido en un rango de fechas (excluye ANULADAS)
    @Query("SELECT COALESCE(SUM(v.total),0) FROM Venta v WHERE v.estado != 'ANULADA' AND v.fecha >= :desde AND v.fecha <= :hasta")
    BigDecimal sumTotalEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // Cantidad de ventas en un rango
    @Query("SELECT COUNT(v) FROM Venta v WHERE v.estado != 'ANULADA' AND v.fecha >= :desde AND v.fecha <= :hasta")
    long countVentasEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // Productos vendidos (suma de cantidades) en un rango
    @Query("SELECT COALESCE(SUM(d.cantidad),0) FROM DetalleVenta d WHERE d.venta.estado != 'ANULADA' AND d.venta.fecha >= :desde AND d.venta.fecha <= :hasta")
    long sumProductosVendidosEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // Verificar unicidad del código de operación
    boolean existsByCodigoOperacion(String codigoOperacion);

    // Pedidos online ordenados por fecha
    List<Venta> findBySerieOrderByFechaDesc(String serie);
}
