package com.grupob.bancobanquito.prestamos;

import java.time.LocalDate;

public interface ServiceDevengoMora {

    int ejecutarDevengo(LocalDate fechaNegocio);

    int ejecutarAjustesDevengo(LocalDate fechaNegocio);

    int ejecutarMora(LocalDate fechaNegocio);
}
