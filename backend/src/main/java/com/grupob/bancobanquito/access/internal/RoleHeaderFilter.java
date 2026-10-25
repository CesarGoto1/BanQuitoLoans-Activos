package com.grupob.bancobanquito.access.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class RoleHeaderFilter extends OncePerRequestFilter {

    static final String ROLE_HEADER = "X-Rol";
    static final String IDENTIFICATION_HEADER = "X-Identificacion";
    static final String CUSTOMER = "CLIENTE";
    static final String ANALYST = "ANALISTA";

    private final AnalystRepository analystRepository;

    RoleHeaderFilter(AnalystRepository analystRepository) {
        this.analystRepository = analystRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Set<String> allowedRoles = allowedRoles(request.getRequestURI());
        if (allowedRoles.isEmpty() || "OPTIONS".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String role = request.getHeader(ROLE_HEADER);
        String identification = request.getHeader(IDENTIFICATION_HEADER);
        if (identification == null || !identification.matches("\\d{10}")) {
            reject(response, HttpStatus.UNAUTHORIZED, "Inicia sesión: falta la cabecera X-Identificacion con una cédula de 10 dígitos.");
            return;
        }
        if (role == null || !allowedRoles.contains(role)) {
            reject(response, HttpStatus.FORBIDDEN, "Tu rol no tiene acceso a esta información.");
            return;
        }
        if (ANALYST.equals(role) && analystRepository.findByNationalId(identification).isEmpty()) {
            reject(response, HttpStatus.FORBIDDEN, "El analista no está registrado en el sistema.");
            return;
        }
        chain.doFilter(request, response);
    }

    private Set<String> allowedRoles(String path) {
        if (path.startsWith("/api/analista/") || path.startsWith("/api/disbursements")
                || path.startsWith("/api/disbursement-previews/") || path.startsWith("/api/pending-disbursements")) {
            return Set.of(ANALYST);
        }
        if (path.startsWith("/api/loan-applications")) {
            return Set.of(CUSTOMER);
        }
        if (path.startsWith("/api/loans")) {
            return Set.of(CUSTOMER, ANALYST);
        }
        return Set.of();
    }

    private void reject(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":" + status.value() + ",\"title\":\"Acceso no autorizado\",\"detail\":\""
                + detail + "\"}");
    }
}
