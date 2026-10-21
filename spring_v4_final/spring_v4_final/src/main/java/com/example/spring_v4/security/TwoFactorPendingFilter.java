package com.example.spring_v4.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Bloquea el acceso al sistema mientras exista una verificación 2FA pendiente.
 *
 * Sin este filtro, un usuario que ya ingresó su contraseña correctamente podía
 * retroceder/avanzar en el navegador y entrar al sistema SIN escribir el código
 * enviado a su correo, porque Spring Security guarda la autenticación en la
 * sesión antes de que se ejecute el successHandler.
 */
public class TwoFactorPendingFilter extends OncePerRequestFilter {

    private static final String PENDING_AUTH = "PENDING_AUTH";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute(PENDING_AUTH) != null) {

            String path = request.getRequestURI();
            String ctx  = request.getContextPath();
            if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
                path = path.substring(ctx.length());
            }

            if (!esRutaPermitida(path)) {
                // Hay un 2FA pendiente: anular cualquier autenticación y devolver al 2FA
                SecurityContextHolder.clearContext();
                session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.sendRedirect(request.getContextPath() + "/2fa");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /** Rutas a las que sí se puede acceder con un 2FA pendiente. */
    private boolean esRutaPermitida(String path) {
        return path.equals("/2fa")
            || path.startsWith("/2fa/")
            || path.equals("/login")
            || path.equals("/logout")
            || path.equals("/error")
            || path.equals("/recuperar-password")
            || path.startsWith("/recuperar-password/")
            || path.equals("/catalogo")
            || path.startsWith("/catalogo/")
            || path.startsWith("/css/")
            || path.startsWith("/js/")
            || path.startsWith("/images/")
            || path.startsWith("/imagenes/")
            || path.startsWith("/uploads/")
            || path.startsWith("/slider-logo/api/");
    }
}
