package com.example.spring_v4.controller;

import com.example.spring_v4.repository.UsuarioRepository;
import com.example.spring_v4.service.EmailService;
import com.example.spring_v4.service.TwoFactorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TwoFactorController {

    private static final String PENDING_AUTH = "PENDING_AUTH";
    private static final String TWO_FA_USERNAME = "TWO_FA_USERNAME";

    private final TwoFactorService twoFactorService;
    private final EmailService emailService;
    private final UsuarioRepository usuarioRepo;

    public TwoFactorController(TwoFactorService twoFactorService,
                                EmailService emailService,
                                UsuarioRepository usuarioRepo) {
        this.twoFactorService = twoFactorService;
        this.emailService     = emailService;
        this.usuarioRepo      = usuarioRepo;
    }

    @GetMapping("/2fa")
    public String mostrarFormulario(HttpSession session, Model model) {
        Authentication pendingAuth = (Authentication) session.getAttribute(PENDING_AUTH);
        if (pendingAuth == null) {
            return "redirect:/login";
        }
        String username = (String) session.getAttribute(TWO_FA_USERNAME);
        String emailMask = "";
        var usuarioOpt = usuarioRepo.findByUsername(username);
        if (usuarioOpt.isPresent() && usuarioOpt.get().getEmail() != null) {
            emailMask = enmascararEmail(usuarioOpt.get().getEmail());
        }
        model.addAttribute("emailMask", emailMask);
        return "verificar-2fa";
    }

    @PostMapping("/2fa/verificar")
    public String verificar(@RequestParam String codigo,
                            HttpSession session,
                            Model model) {

        Authentication pendingAuth = (Authentication) session.getAttribute(PENDING_AUTH);
        String username = (String) session.getAttribute(TWO_FA_USERNAME);

        if (pendingAuth == null || username == null) {
            return "redirect:/login";
        }

        if (twoFactorService.verificarCodigo(username, codigo.trim())) {
            SecurityContext sc = SecurityContextHolder.createEmptyContext();
            sc.setAuthentication(pendingAuth);
            SecurityContextHolder.setContext(sc);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
            session.removeAttribute(PENDING_AUTH);
            session.removeAttribute(TWO_FA_USERNAME);

            // Si tiene contraseña temporal → forzar cambio
            var usuarioOpt2 = usuarioRepo.findByUsername(username);
            if (usuarioOpt2.isPresent() && Boolean.TRUE.equals(usuarioOpt2.get().getPasswordTemporal())) {
                return "redirect:/cambiar-password";
            }
            return "redirect:/";
        }

        if (twoFactorService.estaAgotado(username)) {
            twoFactorService.limpiar(username);
            session.invalidate();
            return "redirect:/login?2fa-bloqueado=true";
        }

        String emailMask = "";
        var usuarioOpt = usuarioRepo.findByUsername(username);
        if (usuarioOpt.isPresent() && usuarioOpt.get().getEmail() != null) {
            emailMask = enmascararEmail(usuarioOpt.get().getEmail());
        }
        model.addAttribute("emailMask", emailMask);
        model.addAttribute("error", true);
        return "verificar-2fa";
    }

    @PostMapping("/2fa/reenviar")
    public String reenviar(HttpSession session) {
        String username = (String) session.getAttribute(TWO_FA_USERNAME);
        Authentication pendingAuth = (Authentication) session.getAttribute(PENDING_AUTH);
        if (username == null || pendingAuth == null) {
            return "redirect:/login";
        }
        try {
            var usuarioOpt = usuarioRepo.findByUsername(username);
            if (usuarioOpt.isPresent() && usuarioOpt.get().getEmail() != null) {
                String codigo = twoFactorService.generarCodigo(username);
                emailService.enviarCodigo2FA(usuarioOpt.get().getEmail(), codigo, username);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/2fa?reenviado=true";
    }

    private String enmascararEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        String local = email.substring(0, at);
        String domain = email.substring(at);
        int visible = Math.min(3, local.length());
        return local.substring(0, visible) + "*".repeat(Math.max(0, local.length() - visible)) + domain;
    }
}
