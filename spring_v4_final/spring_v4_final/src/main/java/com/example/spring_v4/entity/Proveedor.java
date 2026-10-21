package com.example.spring_v4.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "proveedores")
@Data
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nombre;

    private String telefono;

    private String direccion;

    private String email;

    private Boolean estado = true;
}