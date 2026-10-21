package com.example.spring_v4.controller;

import com.example.spring_v4.entity.LogoImagen;
import com.example.spring_v4.entity.SliderImagen;
import com.example.spring_v4.repository.LogoImagenRepository;
import com.example.spring_v4.repository.SliderImagenRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/slider-logo")
public class SliderLogoController {

    private final SliderImagenRepository sliderRepo;
    private final LogoImagenRepository logoRepo;

    public SliderLogoController(SliderImagenRepository sliderRepo,
                                LogoImagenRepository logoRepo) {
        this.sliderRepo = sliderRepo;
        this.logoRepo = logoRepo;
    }

    private static final int MAX_SLIDER = 5;
    private static final int MAX_LOGO   = 5;
    private static final String DIR_SLIDER = "src/main/resources/static/imagenes/slider/";
    private static final String DIR_LOGOS  = "src/main/resources/static/imagenes/slider/logos/";

    // Tipos de imagen permitidos
    private static final Set<String> MIME_PERMITIDOS = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );
    private static final Set<String> EXT_PERMITIDAS = Set.of(
        "jpg", "jpeg", "png", "gif", "webp", "svg"
    );

    private boolean esImagenValida(MultipartFile archivo) {
        // Validar MIME type
        String mime = archivo.getContentType();
        if (mime == null || !MIME_PERMITIDOS.contains(mime.toLowerCase())) return false;
        // Validar extensión del nombre original
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !nombre.contains(".")) return false;
        String ext = nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase();
        return EXT_PERMITIDAS.contains(ext);
    }

    // =====================================
    // PANEL PRINCIPAL
    // =====================================

    @GetMapping({"", "/"})
    public String panel(Model model) {
        model.addAttribute("sliders", sliderRepo.findAll());
        model.addAttribute("logos",   logoRepo.findAll());
        model.addAttribute("maxSlider", MAX_SLIDER);
        model.addAttribute("maxLogo",   MAX_LOGO);
        return "slider-logo";
    }

    // =====================================
    // GUARDAR SLIDER
    // =====================================

    @PostMapping("/slider/guardar")
    public String guardarSlider(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "titulo",    required = false) String titulo,
            @RequestParam(value = "subtitulo", required = false) String subtitulo) {

        if (archivo.isEmpty() || !esImagenValida(archivo)) {
            return "redirect:/slider-logo?errorSlider=tipoInvalido";
        }
        try {
            long total = sliderRepo.count();
            if (total >= MAX_SLIDER) {
                return "redirect:/slider-logo?errorSlider=max";
            }
            String nombre = System.currentTimeMillis() + "_" + archivo.getOriginalFilename();
            new File(DIR_SLIDER).mkdirs();
            Files.write(Paths.get(DIR_SLIDER + nombre), archivo.getBytes());

            SliderImagen s = new SliderImagen();
            s.setRuta("/imagenes/slider/" + nombre);
            s.setTitulo(titulo);
            s.setSubtitulo(subtitulo);
            s.setActivo(true);
            sliderRepo.save(s);
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/slider-logo?errorSlider=error";
        }
        return "redirect:/slider-logo?successSlider=guardado";
    }

    // =====================================
    // ELIMINAR SLIDER
    // =====================================

    @GetMapping("/slider/eliminar/{id}")
    public String eliminarSlider(@PathVariable Integer id) {
        try {
            SliderImagen s = sliderRepo.findById(id).orElseThrow();
            eliminarArchivo(s.getRuta());
            sliderRepo.delete(s);
        } catch (Exception e) { e.printStackTrace(); }
        return "redirect:/slider-logo?successSlider=eliminado";
    }

    // =====================================
    // GUARDAR LOGO
    // =====================================

    @PostMapping("/logo/guardar")
    public String guardarLogo(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "nombre", required = false) String nombre) {

        if (archivo.isEmpty() || !esImagenValida(archivo)) {
            return "redirect:/slider-logo?errorLogo=tipoInvalido";
        }
        try {
            long total = logoRepo.count();
            if (total >= MAX_LOGO) {
                return "redirect:/slider-logo?errorLogo=max";
            }
            String nombreArchivo = System.currentTimeMillis() + "_" + archivo.getOriginalFilename();
            new File(DIR_LOGOS).mkdirs();
            Files.write(Paths.get(DIR_LOGOS + nombreArchivo), archivo.getBytes());

            LogoImagen l = new LogoImagen();
            l.setRuta("/imagenes/slider/logos/" + nombreArchivo);
            l.setNombre(nombre);
            l.setActivo(true);
            logoRepo.save(l);
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/slider-logo?errorLogo=error";
        }
        return "redirect:/slider-logo?successLogo=guardado";
    }

    // =====================================
    // ELIMINAR LOGO
    // =====================================

    @GetMapping("/logo/eliminar/{id}")
    public String eliminarLogo(@PathVariable Integer id) {
        try {
            LogoImagen l = logoRepo.findById(id).orElseThrow();
            eliminarArchivo(l.getRuta());
            logoRepo.delete(l);
        } catch (Exception e) { e.printStackTrace(); }
        return "redirect:/slider-logo?successLogo=eliminado";
    }

    // =====================================
    // ACTIVAR / DESACTIVAR SLIDER
    // =====================================

    @GetMapping("/slider/toggle/{id}")
    public String toggleSlider(@PathVariable Integer id) {
        try {
            SliderImagen s = sliderRepo.findById(id).orElseThrow();
            s.setActivo(!s.getActivo());
            sliderRepo.save(s);
        } catch (Exception e) { e.printStackTrace(); }
        return "redirect:/slider-logo";
    }

    // =====================================
    // ACTIVAR / DESACTIVAR LOGO
    // =====================================

    @GetMapping("/logo/toggle/{id}")
    public String toggleLogo(@PathVariable Integer id) {
        try {
            LogoImagen l = logoRepo.findById(id).orElseThrow();
            l.setActivo(!l.getActivo());
            logoRepo.save(l);
        } catch (Exception e) { e.printStackTrace(); }
        return "redirect:/slider-logo";
    }

    // =====================================
    // API PUBLICO — para catálogo
    // =====================================

    @GetMapping("/api/sliders")
    @ResponseBody
    public List<SliderImagen> apiSliders() {
        return sliderRepo.findByActivoTrue();
    }

    @GetMapping("/api/logos")
    @ResponseBody
    public List<LogoImagen> apiLogos() {
        return logoRepo.findByActivoTrue();
    }

    // =====================================
    // HELPER
    // =====================================

    private void eliminarArchivo(String ruta) {
        if (ruta == null) return;
        File f = new File("src/main/resources/static" + ruta);
        if (f.exists()) f.delete();
    }
}
