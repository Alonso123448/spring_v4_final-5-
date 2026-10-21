package com.example.spring_v4.controller;

import com.example.spring_v4.entity.Rol;
import com.example.spring_v4.entity.Usuario;
import com.example.spring_v4.repository.RolRepository;
import com.example.spring_v4.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.security.SecureRandom;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepo;
    private final RolRepository rolRepo;
    private final BCryptPasswordEncoder encoder;

    public UsuarioController(UsuarioRepository usuarioRepo,
                             RolRepository rolRepo,
                             BCryptPasswordEncoder encoder) {
        this.usuarioRepo = usuarioRepo;
        this.rolRepo = rolRepo;
        this.encoder = encoder;
    }

    // =========================
    // LISTAR USUARIOS
    // =========================
    @GetMapping
    public String listar(Model model) {

        model.addAttribute("usuarios", usuarioRepo.findAll());
        model.addAttribute("roles", rolRepo.findByEstadoTrue());
        model.addAttribute("usuario", new Usuario());

        model.addAttribute("modoVer", false);
        model.addAttribute("modoEditar", false);

        return "usuarios";
    }

    // =========================
    // GUARDAR / EDITAR
    // =========================
    @PostMapping("/guardar")
public String guardar(@ModelAttribute Usuario usuario,
                      @RequestParam(required = false) Integer rolesSeleccionados,
                      RedirectAttributes redirectAttributes) {

    Usuario usuarioGuardar;

    // =========================
    // EDITAR
    // =========================
    if (usuario.getId() != null) {

        // VALIDAR DUPLICADOS EXCLUYENDO EL MISMO ID
        if (usuarioRepo.existsByUsernameAndIdNot(usuario.getUsername(), usuario.getId())) {
            redirectAttributes.addFlashAttribute("error", "El username ya está en uso");
            return "redirect:/usuarios";
        }

        if (usuarioRepo.existsByNombreAndIdNot(usuario.getNombre(), usuario.getId())) {
            redirectAttributes.addFlashAttribute("error", "El nombre ya está en uso");
            return "redirect:/usuarios";
        }

        if (usuarioRepo.existsByEmailAndIdNot(usuario.getEmail(), usuario.getId())) {
            redirectAttributes.addFlashAttribute("error", "El email ya está en uso");
            return "redirect:/usuarios";
        }

        usuarioGuardar = usuarioRepo.findById(usuario.getId()).orElseThrow();

        usuarioGuardar.setUsername(usuario.getUsername());
        usuarioGuardar.setNombre(usuario.getNombre());
        usuarioGuardar.setEmail(usuario.getEmail());

        // Proteger superadmin
        if (!usuarioGuardar.getUsername().equalsIgnoreCase("superadmin")) {
            usuarioGuardar.setActivo(usuario.getActivo());
        }

        // Actualizar contraseña solo si se escribe nueva
        if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
            usuarioGuardar.setPassword(encoder.encode(usuario.getPassword()));
        }

    } else {

        // =========================
        // NUEVO
        // =========================
        if (usuarioRepo.existsByUsername(usuario.getUsername())) {
            redirectAttributes.addFlashAttribute("error", "El username ya está en uso");
            return "redirect:/usuarios";
        }

        if (usuarioRepo.existsByNombre(usuario.getNombre())) {
            redirectAttributes.addFlashAttribute("error", "El nombre ya está en uso");
            return "redirect:/usuarios";
        }

        if (usuarioRepo.existsByEmail(usuario.getEmail())) {
            redirectAttributes.addFlashAttribute("error", "El email ya está en uso");
            return "redirect:/usuarios";
        }

        // Validar que la contraseña no esté vacía para nuevo usuario
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "La contraseña es obligatoria para un nuevo usuario.");
            return "redirect:/usuarios";
        }

        // Validar que solo exista un SUPERADMIN
        if (rolesSeleccionados != null) {
            Rol rolSeleccionado = rolRepo.findById(rolesSeleccionados).orElse(null);
            if (rolSeleccionado != null && rolSeleccionado.getNombre().equalsIgnoreCase("SUPERADMIN")) {
                if (usuarioRepo.existsByRolesNombre("SUPERADMIN")) {
                    redirectAttributes.addFlashAttribute("error", "Ya existe un SUPERADMIN registrado. Solo puede haber uno.");
                    return "redirect:/usuarios";
                }
            }
        }

        // Construir objeto limpio (evita mass assignment de campos internos
        // como bloqueado, intentosFallidos o passwordTemporal desde el form)
        usuarioGuardar = new Usuario();
        usuarioGuardar.setUsername(usuario.getUsername());
        usuarioGuardar.setNombre(usuario.getNombre());
        usuarioGuardar.setEmail(usuario.getEmail());
        usuarioGuardar.setActivo(true);
        usuarioGuardar.setPasswordTemporal(false);
        usuarioGuardar.setBloqueado(false);
        usuarioGuardar.setIntentosFallidos(0);
        usuarioGuardar.setPassword(encoder.encode(usuario.getPassword()));
    }

    // =========================
    // ROLES
    // =========================
    Set<Rol> roles = new HashSet<>();

    if (rolesSeleccionados != null) {
        Rol rol = rolRepo.findById(rolesSeleccionados).orElseThrow();
        roles.add(rol);
    }

    usuarioGuardar.setRoles(roles);

    usuarioRepo.save(usuarioGuardar);

    redirectAttributes.addFlashAttribute("success", "Usuario guardado correctamente");

    return "redirect:/usuarios";
}

    // =========================
    // ELIMINAR
    // =========================
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {

        Usuario usuario = usuarioRepo.findById(id)
                .orElseThrow();

        // PROTEGER SUPERADMIN
        if (!usuario.getUsername()
                .equalsIgnoreCase("superadmin")) {

            usuarioRepo.delete(usuario);
        }

        return "redirect:/usuarios";
    }

    // =========================
    // CAMBIAR ESTADO
    // =========================
    @PostMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable Integer id) {

        Usuario usuario = usuarioRepo.findById(id)
                .orElseThrow();

        // SUPERADMIN SIEMPRE ACTIVO
        if (!usuario.getUsername()
                .equalsIgnoreCase("superadmin")) {

            usuario.setActivo(!usuario.getActivo());

            usuarioRepo.save(usuario);
        }

        return "redirect:/usuarios";
    }

    // =========================
    // EDITAR USUARIO
    // =========================
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id,
                        Model model) {

        Usuario usuario = usuarioRepo.findById(id)
                .orElseThrow();

        model.addAttribute("usuario", usuario);

        model.addAttribute("usuarios",
                usuarioRepo.findAll());

        model.addAttribute("roles",
                rolRepo.findAll());

        model.addAttribute("modoEditar", true);

        model.addAttribute("modoVer", false);

        return "usuarios";
    }

    // =========================
    // RESETEAR CONTRASEÑA TEMPORAL
    // =========================
    @PostMapping("/resetear/{id}")
    @ResponseBody
    public Map<String, Object> resetearPassword(@PathVariable Integer id,
                                                 org.springframework.security.core.Authentication auth) {
        Map<String, Object> res = new HashMap<>();
        try {
            // Solo superadmin puede resetear
            boolean puedeResetear = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN")
                               || a.getAuthority().equals("ROLE_ADMIN"));
            if (!puedeResetear) {
                res.put("ok", false);
                res.put("msg", "No tienes permiso para realizar esta acción.");
                return res;
            }
            Usuario u = usuarioRepo.findById(id).orElseThrow();
            if (u.getUsername().equalsIgnoreCase("superadmin")) {
                res.put("ok", false);
                res.put("msg", "No se puede resetear la contraseña del superadmin.");
                return res;
            }
            String temporal = generarPasswordTemporal();
            u.setPassword(encoder.encode(temporal));
            u.setPasswordTemporal(true);
            usuarioRepo.save(u);
            res.put("ok", true);
            res.put("password", temporal);
        } catch (Exception e) {
            res.put("ok", false);
            res.put("msg", e.getMessage());
        }
        return res;
    }

    private String generarPasswordTemporal() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#$!";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    // =========================
    // VER USUARIO
    // =========================
    @GetMapping("/ver/{id}")
    public String ver(@PathVariable Integer id,
                    Model model) {

        Usuario usuario = usuarioRepo.findById(id)
                .orElseThrow();

        model.addAttribute("usuario", usuario);

        model.addAttribute("usuarios",
                usuarioRepo.findAll());

        model.addAttribute("roles",
                rolRepo.findAll());

        model.addAttribute("modoVer", true);

        model.addAttribute("modoEditar", false);

        return "usuarios";
    }
}