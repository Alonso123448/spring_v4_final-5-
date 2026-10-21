package com.example.spring_v4.security;

import com.example.spring_v4.entity.Usuario;
import com.example.spring_v4.repository.UsuarioRepository;
import com.example.spring_v4.service.EmailService;
import com.example.spring_v4.service.TwoFactorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@Configuration
public class SecurityConfig {

    private final TwoFactorService twoFactorService;
    private final EmailService emailService;
    private final UsuarioRepository usuarioRepo;

    public SecurityConfig(TwoFactorService twoFactorService,
                          EmailService emailService,
                          UsuarioRepository usuarioRepo) {
        this.twoFactorService = twoFactorService;
        this.emailService     = emailService;
        this.usuarioRepo      = usuarioRepo;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )

            // Bloquea el sistema mientras haya una verificación 2FA pendiente
            .addFilterBefore(new TwoFactorPendingFilter(), UsernamePasswordAuthenticationFilter.class)

            .authorizeHttpRequests(auth -> auth

                // RUTAS PÚBLICAS
                .requestMatchers(
                        "/login",
                        "/2fa",
                        "/2fa/verificar",
                        "/2fa/reenviar",
                        "/cambiar-password",
                        "/recuperar-password",
                        "/recuperar-password/nueva",
                        "/error",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/uploads/**",
                        "/imagenes/**",
                        "/admin/migrar/**",
                        "/catalogo",
                        "/catalogo/",
                        "/catalogo/pedido",
                        "/catalogo/clientes/buscar",
                        "/slider-logo/api/**"
                ).permitAll()

                // DASHBOARD
                .requestMatchers("/").hasAuthority("DASHBOARD")

                // PRODUCTOS
                .requestMatchers("/productos/**").hasAuthority("PRODUCTOS")

                // CATEGORÍAS
                .requestMatchers("/categorias/**").hasAuthority("CATEGORIAS")

                // PROVEEDORES
                .requestMatchers("/proveedores/**").hasAuthority("PROVEEDORES")

                // USUARIOS
                .requestMatchers("/usuarios/**").hasAuthority("USUARIOS")

                // ROLES / PERFILES
                .requestMatchers("/roles/**").hasAuthority("PERFILES")

                // MOVIMIENTOS
                .requestMatchers("/movimientos/**").hasAuthority("MOVIMIENTOS")

                // CLIENTES
                .requestMatchers("/clientes/**").hasAuthority("CLIENTES")

                // VENTAS
                .requestMatchers("/ventas/**").hasAuthority("VENTAS")

                // NOTAS DE VENTA
                .requestMatchers("/notas-venta/**").hasAuthority("VENTAS")

                // VERIFICACIÓN DE PAGOS
                .requestMatchers("/verificacion-pagos/**").hasAuthority("VENTAS")

                // REPORTES
                .requestMatchers("/reportes/**").hasAuthority("VENTAS")

                // SLIDER-LOGO
                .requestMatchers("/slider-logo/**").hasAuthority("SLIDER_LOGO")

                // TODO LO DEMÁS

                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                    .loginPage("/login")
                    .successHandler(twoFactorSuccessHandler())
                    .failureHandler((request, response, exception) -> {
                        if (exception instanceof DisabledException) {
                            response.sendRedirect("/login?inactivo=true");
                            return;
                        }
                        if (exception instanceof LockedException) {
                            response.sendRedirect("/login?bloqueado=true");
                            return;
                        }
                        // Contraseña incorrecta: incrementar intentos
                        String username = request.getParameter("username");
                        final int[] intentosRestantes = {3};
                        if (username != null) {
                            var usuarioOpt = usuarioRepo.findByUsername(username.trim());
                            if (usuarioOpt.isPresent()) {
                                Usuario u = usuarioOpt.get();
                                int intentos = u.getIntentosFallidos() + 1;
                                u.setIntentosFallidos(intentos);
                                if (intentos >= 3) {
                                    u.setBloqueado(true);
                                }
                                usuarioRepo.save(u);
                                intentosRestantes[0] = Math.max(0, 3 - intentos);
                            }
                        }
                        if (intentosRestantes[0] == 0) {
                            response.sendRedirect("/login?bloqueado=true");
                        } else {
                            response.sendRedirect("/login?error=true&restantes=" + intentosRestantes[0]);
                        }
                    })
                    .permitAll()
            )

            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
            );

        return http.build();
    }

    private AuthenticationSuccessHandler twoFactorSuccessHandler() {
        return (request, response, authentication) -> {
            String username = authentication.getName();
            HttpSession session = request.getSession();

            session.setAttribute("PENDING_AUTH", authentication);
            session.setAttribute("TWO_FA_USERNAME", username);

            // IMPORTANTE: Spring Security ya guardó la autenticación en la SESIÓN
            // antes de llegar aquí. Limpiar solo el contexto del hilo NO basta:
            // hay que borrarla también de la sesión, o el usuario entra sin 2FA
            // simplemente retrocediendo/avanzando en el navegador.
            SecurityContextHolder.clearContext();
            session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

            try {
                var usuarioOpt = usuarioRepo.findByUsername(username);
                if (usuarioOpt.isPresent()) {
                    Usuario u = usuarioOpt.get();
                    // Resetear contador de intentos al ingresar correctamente
                    u.setIntentosFallidos(0);
                    usuarioRepo.save(u);
                    if (u.getEmail() != null) {
                        String codigo = twoFactorService.generarCodigo(username);
                        emailService.enviarCodigo2FA(u.getEmail(), codigo, username);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            response.sendRedirect("/2fa");
        };
    }
}
