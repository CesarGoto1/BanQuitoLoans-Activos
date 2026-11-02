package com.grupob.bancobanquito.cobranza;

import java.time.LocalDate;

public interface ServicioClasificacionCartera {

    int clasificarCartera(LocalDate fechaNegocio);
}
