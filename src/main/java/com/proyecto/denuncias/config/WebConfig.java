package com.proyecto.denuncias.config;

import com.proyecto.denuncias.security.SesionInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración general de la capa web.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SesionInterceptor sesionInterceptor;

    public WebConfig(SesionInterceptor sesionInterceptor) {
        this.sesionInterceptor = sesionInterceptor;
    }

    /**
     * Registra el interceptor de sesión:
     *  - protege todas las rutas /api/**
     *  - excepto /api/auth/** (login, recuperación de contraseña, etc.),
     *    que deben poder usarse sin haber iniciado sesión.
     * Los archivos estáticos (index.html, css, js) no pasan por el interceptor.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sesionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**");
    }

    /**
     * Codificador de contraseñas BCrypt. Se usa para cifrar la contraseña
     * al registrarla y para compararla al iniciar sesión.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
