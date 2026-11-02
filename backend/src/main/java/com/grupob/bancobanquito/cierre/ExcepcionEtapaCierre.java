package com.grupob.bancobanquito.cierre;

// Error de negocio de una etapa: su mensaje se muestra al analista en la bitácora del cierre.
public class ExcepcionEtapaCierre extends RuntimeException {

    public ExcepcionEtapaCierre(String message) {
        super(message);
    }
}
