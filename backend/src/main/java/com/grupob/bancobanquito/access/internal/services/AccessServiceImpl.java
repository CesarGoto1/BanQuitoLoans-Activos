package com.grupob.bancobanquito.access.internal.services;
import com.grupob.bancobanquito.access.internal.config.*;
import com.grupob.bancobanquito.access.internal.cors.*;
import com.grupob.bancobanquito.access.internal.security.*;
import com.grupob.bancobanquito.access.internal.exceptions.*;
import com.grupob.bancobanquito.access.AccessService;
import com.grupob.bancobanquito.access.Identity;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Service
public class AccessServiceImpl implements AccessService {

    @Override
    public Identity currentIdentity() {
        return identidadSiExiste().orElseThrow(
                () -> new AccessDeniedException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión para continuar."));
    }

    Optional<Identity> identidadSiExiste() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos == null) {
            return Optional.empty();
        }
        Object valor = atributos.getAttribute(IdentificationInterceptor.ATRIBUTO, RequestAttributes.SCOPE_REQUEST);
        return valor instanceof Identity identidad ? Optional.of(identidad) : Optional.empty();
    }
}
















