package com.example.spring_v4.controller;

import com.example.spring_v4.entity.Permiso;
import com.example.spring_v4.entity.Rol;

import com.example.spring_v4.repository.PermisoRepository;
import com.example.spring_v4.repository.RolRepository;
import com.example.spring_v4.repository.UsuarioRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/roles")
public class RolController {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final UsuarioRepository usuarioRepository;

    // =========================
    // CONSTRUCTOR
    // =========================
    public RolController(
            RolRepository rolRepository,
            PermisoRepository permisoRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.rolRepository    = rolRepository;
        this.permisoRepository = permisoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // =========================
    // LISTAR ROLES
    // =========================
    @GetMapping
    public String listar(Model model) {

        model.addAttribute(
                "roles",
                rolRepository.findAll()
        );

        // enviar permisos al frontend
        model.addAttribute(
                "permisos",
                permisoRepository.findAll()
        );

        return "roles";
    }

    // =========================
    // NUEVO ROL
    // =========================
    @GetMapping("/nuevo")
    public String nuevo(Model model) {

        model.addAttribute(
                "rol",
                new Rol()
        );

        return "rol-form";
    }

    // =========================
    // GUARDAR ROL
    // =========================
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Rol rol, RedirectAttributes ra) {

        // Validar nombre obligatorio
        if (rol.getNombre() == null || rol.getNombre().isBlank()) {
            ra.addFlashAttribute("error", "El nombre del perfil es obligatorio.");
            return "redirect:/roles";
        }

        // Validar nombre solo letras y espacios (sin números ni caracteres especiales)
        if (!rol.getNombre().matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$")) {
            ra.addFlashAttribute("error", "El nombre del perfil solo puede contener letras y espacios.");
            return "redirect:/roles";
        }

        // Validar descripción obligatoria
        if (rol.getDescripcion() == null || rol.getDescripcion().isBlank()) {
            ra.addFlashAttribute("error", "La descripción del perfil es obligatoria.");
            return "redirect:/roles";
        }

        // SUPERADMIN siempre activo
        if ("SUPERADMIN".equalsIgnoreCase(rol.getNombre())) {
            rol.setEstado(true);
        }

        // Normalizar nombre antes de verificar duplicados
        String nombreNormalizado = rol.getNombre().trim().toUpperCase();

        // Validar unicidad: distinto comportamiento para nuevo vs edición
        if (rol.getId() == null) {
            if (rolRepository.existsByNombre(nombreNormalizado)) {
                ra.addFlashAttribute("error", "Ya existe un perfil con el nombre '" + nombreNormalizado + "'. Elige un nombre diferente.");
                return "redirect:/roles";
            }
        } else {
            if (rolRepository.existsByNombreAndIdNot(nombreNormalizado, rol.getId())) {
                ra.addFlashAttribute("error", "Ya existe otro perfil con el nombre '" + nombreNormalizado + "'. Elige un nombre diferente.");
                return "redirect:/roles";
            }
        }

        rol.setNombre(nombreNormalizado);
        rol.setDescripcion(rol.getDescripcion().trim());

        rolRepository.save(rol);

        ra.addFlashAttribute("success", "Perfil '" + nombreNormalizado + "' guardado correctamente.");
        return "redirect:/roles";
    }

    // =========================
    // EDITAR
    // =========================
    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Integer id,
            Model model
    ) {

        Rol rol = rolRepository
                .findById(id)
                .orElseThrow();

        model.addAttribute("rol", rol);

        return "rol-form";
    }

    // =========================
    // ELIMINAR
    // =========================
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes ra) {

        Rol rol = rolRepository.findById(id).orElseThrow();

        // Proteger SUPERADMIN
        if ("SUPERADMIN".equalsIgnoreCase(rol.getNombre())) {
            ra.addFlashAttribute("error", "El perfil 'SUPERADMIN' no se puede eliminar.");
            return "redirect:/roles";
        }

        // Verificar si el perfil está asignado a algún usuario
        if (usuarioRepository.existsByRolesId(id)) {
            ra.addFlashAttribute("error",
                "No se puede eliminar el perfil '" + rol.getNombre() +
                "' porque está asignado a uno o más usuarios. Cambia su estado a Inactivo si ya no lo necesitas.");
            return "redirect:/roles";
        }

        try {
            rolRepository.deleteById(id);
            ra.addFlashAttribute("success", "Perfil '" + rol.getNombre() + "' eliminado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error",
                "No se pudo eliminar el perfil '" + rol.getNombre() + "' porque está en uso por el sistema.");
        }

        return "redirect:/roles";
    }

    // =========================
    // GUARDAR PERMISOS
    // =========================
    @PostMapping("/guardar-permisos")
public String guardarPermisos(
        @RequestParam Integer rolId,
        @RequestParam(required = false) List<Integer> permisos
) {

    Rol rol = rolRepository
            .findById(rolId)
            .orElseThrow();

    // =========================
    // PROTEGER SUPERADMIN
    // =========================
    if ("SUPERADMIN".equalsIgnoreCase(rol.getNombre())) {

        return "redirect:/roles";
    }

    Set<Permiso> nuevosPermisos = new HashSet<>();

    // si seleccionó permisos
    if (permisos != null) {

        nuevosPermisos.addAll(
                permisoRepository.findAllById(permisos)
        );
    }

    rol.setPermisos(nuevosPermisos);

    rolRepository.save(rol);

    return "redirect:/roles";
}
}