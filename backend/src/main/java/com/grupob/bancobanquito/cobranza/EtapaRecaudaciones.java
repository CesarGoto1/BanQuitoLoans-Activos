package com.grupob.bancobanquito.cobranza;

import com.grupob.bancobanquito.cierre.EtapaCierre;
import com.grupob.bancobanquito.cierre.PasoCierre;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class EtapaRecaudaciones implements EtapaCierre {

    private final ServiceRecaudaciones servicioRecaudaciones;

    public EtapaRecaudaciones(ServiceRecaudaciones servicioRecaudaciones) {
        this.servicioRecaudaciones = servicioRecaudaciones;
    }

    @Override
    public PasoCierre paso() {
        return PasoCierre.RECAUDACIONES;
    }

    @Override
    public int ejecutar(LocalDate fechaNegocio) {
        return this.servicioRecaudaciones.procesarCarpeta(fechaNegocio);
    }
}
