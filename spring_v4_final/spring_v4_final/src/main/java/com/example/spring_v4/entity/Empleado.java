package com.example.spring_v4.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="Empleado")

public class Empleado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private  Integer id;
    private  String nombre;
    private  String cargo;
    private  String foto;

   
}
    