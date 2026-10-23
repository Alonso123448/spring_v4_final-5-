package com.example.spring_v4.controller;

import com.example.spring_v4.entity.ImagenProducto;
import com.example.spring_v4.entity.LogoImagen;
import com.example.spring_v4.entity.SliderImagen;
import com.example.spring_v4.repository.ImagenProductoRepository;
import com.example.spring_v4.repository.LogoImagenRepository;
import com.example.spring_v4.repository.SliderImagenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@RestController
@RequestMapping("/admin/migrar")
public class MigracionController {

    private final ImagenProductoRepository imagenProductoRepo;
    private final SliderImagenRepository   sliderRepo;
    private final LogoImagenRepository     logoRepo;

    private static final String STATIC = "src/main/resources/static";

    public MigracionController(ImagenProductoRepository imagenProductoRepo,
                               SliderImagenRepository sliderRepo,
                               LogoImagenRepository logoRepo) {
        this.imagenProductoRepo = imagenProductoRepo;
        this.sliderRepo         = sliderRepo;
        this.logoRepo           = logoRepo;
    }

    @GetMapping("/imagenes")
    public Map<String, Object> migrar() {
        Map<String, Object> resultado = new LinkedHashMap<>();
        List<String> movidos  = new ArrayList<>();
        List<String> errores  = new ArrayList<>();
        List<String> noExiste = new ArrayList<>();

        for (ImagenProducto img : imagenProductoRepo.findAll()) {
            String rutaActual = img.getRuta(); 
            if (rutaActual == null || !rutaActual.startsWith("/uploads/")) continue;

            Integer productoId = img.getProducto().getId();
            String nombreArchivo = rutaActual.replace("/uploads/", "");

            Path origen  = Paths.get(STATIC + rutaActual);
            String destDir = STATIC + "/imagenes/productos/" + productoId + "/";
            Path destino = Paths.get(destDir + nombreArchivo);

            try {
                new File(destDir).mkdirs();
                if (Files.exists(origen)) {
                    Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
                    String nuevaRuta = "/imagenes/productos/" + productoId + "/" + nombreArchivo;
                    img.setRuta(nuevaRuta);
                    imagenProductoRepo.save(img);
                    movidos.add("PRODUCTO[" + productoId + "] " + nombreArchivo + " → " + nuevaRuta);
                } else {
                    noExiste.add("No encontrado: " + origen);
                }
            } catch (Exception e) {
                errores.add("ERROR producto img " + nombreArchivo + ": " + e.getMessage());
            }
        }

        for (SliderImagen slider : sliderRepo.findAll()) {
            String rutaActual = slider.getRuta();
            if (rutaActual == null || !rutaActual.startsWith("/uploads/")) continue;

            String nombreArchivo = rutaActual.replace("/uploads/", "");
            Path origen  = Paths.get(STATIC + rutaActual);
            String destDir = STATIC + "/imagenes/slider/";
            Path destino = Paths.get(destDir + nombreArchivo);

            try {
                new File(destDir).mkdirs();
                if (Files.exists(origen)) {
                    Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
                    String nuevaRuta = "/imagenes/slider/" + nombreArchivo;
                    slider.setRuta(nuevaRuta);
                    sliderRepo.save(slider);
                    movidos.add("SLIDER " + nombreArchivo + " → " + nuevaRuta);
                } else {
                    noExiste.add("No encontrado: " + origen);
                }
            } catch (Exception e) {
                errores.add("ERROR slider " + nombreArchivo + ": " + e.getMessage());
            }
        }

        for (LogoImagen logo : logoRepo.findAll()) {
            String rutaActual = logo.getRuta();
            if (rutaActual == null || !rutaActual.startsWith("/uploads/")) continue;

            String nombreArchivo = rutaActual.replace("/uploads/", "");
            Path origen  = Paths.get(STATIC + rutaActual);
            String destDir = STATIC + "/imagenes/slider/logos/";
            Path destino = Paths.get(destDir + nombreArchivo);

            try {
                new File(destDir).mkdirs();
                if (Files.exists(origen)) {
                    Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
                    String nuevaRuta = "/imagenes/slider/logos/" + nombreArchivo;
                    logo.setRuta(nuevaRuta);
                    logoRepo.save(logo);
                    movidos.add("LOGO " + nombreArchivo + " → " + nuevaRuta);
                } else {
                    noExiste.add("No encontrado: " + origen);
                }
            } catch (Exception e) {
                errores.add("ERROR logo " + nombreArchivo + ": " + e.getMessage());
            }
        }

        resultado.put("movidos",   movidos);
        resultado.put("noExiste",  noExiste);
        resultado.put("errores",   errores);
        resultado.put("total",     movidos.size() + " archivos migrados correctamente");
        return resultado;
    }
}
