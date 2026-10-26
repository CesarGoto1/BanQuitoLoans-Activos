package com.grupob.bancobanquito.acceso;

import java.util.Optional;

public interface ServiceAnalista {

    SesionAnalista login(String cedula);

    Optional<SesionAnalista> findAnalyst(Integer idAnalista);

    Optional<SesionAnalista> findByNationalId(String cedula);
}
