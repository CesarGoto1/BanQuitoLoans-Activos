package com.grupob.bancobanquito.access.internal.security;
import com.grupob.bancobanquito.access.internal.config.*;
import com.grupob.bancobanquito.access.internal.cors.*;
import com.grupob.bancobanquito.access.internal.security.*;
import com.grupob.bancobanquito.access.internal.exceptions.*;
import com.grupob.bancobanquito.access.Identity;
import com.grupob.bancobanquito.access.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;


public class IdentificationInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO = "bq.identidad";

    private static final String IDENTIFICATION_HEADER = "X-Identification";
    private static final String ROLE_HEADER = "X-Role";

    @Override
    public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta, Object manejador) {
        if ("OPTIONS".equalsIgnoreCase(peticion.getMethod())) {
            return true;
        }

        String identificacion = peticion.getHeader(IDENTIFICATION_HEADER);
        String rolCabecera = peticion.getHeader(ROLE_HEADER);
        if (identificacion == null || identificacion.isBlank() || rolCabecera == null || rolCabecera.isBlank()) {
            throw new AccessDeniedException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión para continuar.");
        }
        if (!identificacion.matches("[0-9]{10}")) {
            throw new AccessDeniedException(HttpStatus.BAD_REQUEST, "La identificación debe tener 10 dígitos.");
        }

        UserRole rol;
        try {
            rol = UserRole.valueOf(rolCabecera.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AccessDeniedException(HttpStatus.FORBIDDEN, "El rol indicado no es válido.");
        }

        UserRole rolRequerido = peticion.getRequestURI().startsWith("/api/analyst")
                ? UserRole.ANALYST
                : UserRole.CLIENT;
        if (rol != rolRequerido) {
            throw new AccessDeniedException(HttpStatus.FORBIDDEN, "No tiene permisos para acceder a este recurso.");
        }

        peticion.setAttribute(ATRIBUTO, new Identity(identificacion, rol));
        return true;
    }
}
















