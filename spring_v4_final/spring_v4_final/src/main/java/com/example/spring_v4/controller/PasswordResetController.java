package com.example.spring_v4.controller;

import com.example.spring_v4.entity.Usuario;
import com.example.spring_v4.repository.UsuarioRepository;
import com.example.spring_v4.service.EmailService;
import com.example.spring_v4.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PasswordResetController {

    private final UsuarioRepository usuarioRepo;
    private final PasswordResetService resetService;
    private final EmailService emailService;
    private final BCryptPasswordEncoder encoder;

    public PasswordResetController(UsuarioRepository usuarioRepo,
                                   PasswordResetService resetService,
                                   EmailService emailService,
                                   BCryptPasswordEncoder encoder) {
        this.usuarioRepo  = usuarioRepo;
        this.resetService = resetService;
        this.emailService = emailService;
        this.encoder      = encoder;
    }

    @GetMapping("/recuperar-password")
    public String mostrarFormCorreo() {
        return "recuperar-password";
    }

    @PostMapping("/recuperar-password")
    public String procesarCorreo(@RequestParam String email,
                                 HttpServletRequest request,
                                 Model model) {
        var usuarioOpt = usuarioRepo.findByEmail(email.trim());

        if (usuarioOpt.isPresent()) {
            Usuario u = usuarioOpt.get();
            String token  = resetService.generarToken(u.getUsername());
            String base   = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String enlace = base + "/recuperar-password/nueva?token=" + token;
            try {
                emailService.enviarRecuperacion(u.getEmail(), u.getUsername(), enlace);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        model.addAttribute("enviado", true);
        return "recuperar-password";
    }

    @GetMapping("/recuperar-password/nueva")
    public String mostrarFormNueva(@RequestParam String token, Model model) {
        String username = resetService.getUsernameByToken(token);
        if (username == null) {
            model.addAttribute("tokenInvalido", true);
            return "recuperar-password-nueva";
        }
        model.addAttribute("token", token);
        return "recuperar-password-nueva";
    }

    @PostMapping("/recuperar-password/nueva")
    public String guardarNueva(@RequestParam String token,
                               @RequestParam String nuevaPassword,
                               @RequestParam String confirmarPassword,
                               Model model) {

        String username = resetService.getUsernameByToken(token);
        if (username == null) {
            model.addAttribute("tokenInvalido", true);
            return "recuperar-password-nueva";
        }
        if (nuevaPassword.length() < 6) {
            model.addAttribute("token", token);
            model.addAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
            return "recuperar-password-nueva";
        }
        if (!nuevaPassword.equals(confirmarPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "recuperar-password-nueva";
        }

        Usuario u = usuarioRepo.findByUsername(username).orElseThrow();
        u.setPassword(encoder.encode(nuevaPassword));
        u.setPasswordTemporal(false);
        usuarioRepo.save(u);
        resetService.invalidarToken(token);

        return "redirect:/login?passwordReset=true";
    }
}
