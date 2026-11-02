package com.grupob.bancobanquito.cobranza;

import com.grupob.bancobanquito.cierre.EtapaCierre;
import com.grupob.bancobanquito.cierre.PasoCierre;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class EtapaClasificacionCartera implements EtapaCierre {

    private final ServiceClasificacionCartera servicioClasificacionCartera;

    public EtapaClasificacionCartera(ServiceClasificacionCartera servicioClasificacionCartera) {
        this.servicioClasificacionCartera = servicioClasificacionCartera;
    }

    @Override
    public PasoCierre paso() {
        return PasoCierre.CLASIFICACION;
    }

    @Override
    public int ejecutar(LocalDate fechaNegocio) {
        return this.servicioClasificacionCartera.clasificarCartera(fechaNegocio);
    }
}
