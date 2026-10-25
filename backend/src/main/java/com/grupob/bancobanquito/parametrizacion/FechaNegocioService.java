package com.grupob.bancobanquito.parametrizacion;

import java.time.LocalDate;

public interface FechaNegocioService {

    /**
     * Retorna la fecha de negocio bancaria actualmente vigente (QA-11).
     * PROHIBIDO usar LocalDate.now().
     *
     * @return Fecha de negocio activa.
     */
    LocalDate fechaActual();
}
