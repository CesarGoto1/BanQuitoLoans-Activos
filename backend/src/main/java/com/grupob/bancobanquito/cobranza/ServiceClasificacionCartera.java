package com.grupob.bancobanquito.cobranza;

import java.time.LocalDate;

public interface ServiceClasificacionCartera {

    int clasificarCartera(LocalDate fechaNegocio);
}
