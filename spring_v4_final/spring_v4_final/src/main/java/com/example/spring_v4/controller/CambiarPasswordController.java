package com.example.spring_v4.controller;

import com.example.spring_v4.entity.Usuario;
import com.example.spring_v4.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CambiarPasswordController {

    private final UsuarioRepository usuarioRepo;
    private final BCryptPasswordEncoder encoder;

    public CambiarPasswordController(UsuarioRepository usuarioRepo, BCryptPasswordEncoder encoder) {
        this.usuarioRepo = usuarioRepo;
        this.encoder     = encoder;
    }

    @GetMapping("/cambiar-password")
    public String mostrar(Authentication auth) {
        if (auth == null) return "redirect:/login";
        return "cambiar-password";
    }

    @PostMapping("/cambiar-password")
    public String cambiar(@RequestParam String nuevaPassword,
                          @RequestParam String confirmarPassword,
                          Authentication auth,
                          HttpSession session,
                          Model model) {

        if (nuevaPassword.length() < 6) {
            model.addAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
            return "cambiar-password";
        }
        if (!nuevaPassword.equals(confirmarPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "cambiar-password";
        }

        String username = auth.getName();
        Usuario u = usuarioRepo.findByUsername(username).orElseThrow();
        u.setPassword(encoder.encode(nuevaPassword));
        u.setPasswordTemporal(false);
        usuarioRepo.save(u);

        return "redirect:/?passwordCambiado=true";
    }
}
