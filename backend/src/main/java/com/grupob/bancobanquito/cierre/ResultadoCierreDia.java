package com.grupob.bancobanquito.cierre;

import java.time.LocalDate;
import java.util.List;

public record ResultadoCierreDia(
        LocalDate fechaNegocio,
        String estadoCierre,
        List<EjecucionEtapaCierre> ejecuciones) {
}
