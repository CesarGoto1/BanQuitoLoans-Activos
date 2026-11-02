package com.grupob.bancobanquito.cierre;

import java.time.LocalDate;

public interface EtapaCierre {

    PasoCierre paso();

    int ejecutar(LocalDate fechaNegocio);
}
