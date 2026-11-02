package com.grupob.bancobanquito.cobranza;

import com.grupob.bancobanquito.cierre.EtapaCierre;
import com.grupob.bancobanquito.cierre.PasoCierre;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class EtapaFotografiaCartera implements EtapaCierre {

    private final ServiceFotografiaCartera servicioFotografiaCartera;

    public EtapaFotografiaCartera(ServiceFotografiaCartera servicioFotografiaCartera) {
        this.servicioFotografiaCartera = servicioFotografiaCartera;
    }

    @Override
    public PasoCierre paso() {
        return PasoCierre.FOTOGRAFIA;
    }

    @Override
    public int ejecutar(LocalDate fechaNegocio) {
        return this.servicioFotografiaCartera.tomarFotografiaDiaria(fechaNegocio);
    }
}
