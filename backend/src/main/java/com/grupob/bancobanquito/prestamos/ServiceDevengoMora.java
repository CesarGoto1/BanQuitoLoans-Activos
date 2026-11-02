package com.grupob.bancobanquito.prestamos;

import java.time.LocalDate;

// Procesos del cierre de día sobre la cartera: devengo diario de intereses (paso 2) y mora (paso 3).
public interface ServiceDevengoMora {

    int ejecutarDevengo(LocalDate fechaNegocio);

    int ejecutarAjustesDevengo(LocalDate fechaNegocio);

    int ejecutarMora(LocalDate fechaNegocio);
}
