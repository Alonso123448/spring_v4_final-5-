package com.example.spring_v4.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // Servir /imagenes/** desde el filesystem (src/main/resources/static/imagenes/)
        String absolutePath = Paths.get("src/main/resources/static/imagenes/")
                .toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/imagenes/**")
                .addResourceLocations(absolutePath)
                .setCachePeriod(0); // sin caché en desarrollo

        // Servir /uploads/** para compatibilidad con imágenes antiguas
        String uploadsPath = Paths.get("src/main/resources/static/uploads/")
                .toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsPath)
                .setCachePeriod(0);
    }
}
