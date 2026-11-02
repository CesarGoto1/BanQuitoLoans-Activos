package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.cierre.EtapaCierre;
import com.grupob.bancobanquito.cierre.PasoCierre;
import com.grupob.bancobanquito.prestamos.ServiceDevengoMora;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
class EtapaMora implements EtapaCierre {

    private final ServiceDevengoMora servicioDevengoMora;

    EtapaMora(ServiceDevengoMora servicioDevengoMora) {
        this.servicioDevengoMora = servicioDevengoMora;
    }

    @Override
    public PasoCierre paso() {
        return PasoCierre.MORA;
    }

    @Override
    public int ejecutar(LocalDate fechaNegocio) {
        return servicioDevengoMora.ejecutarMora(fechaNegocio);
    }
}
