package com.grupob.bancobanquito.cobranza;

import com.grupob.bancobanquito.cierre.EtapaCierre;
import com.grupob.bancobanquito.cierre.PasoCierre;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class EtapaProvisionesCartera implements EtapaCierre {

    private final ServiceProvisionesCartera servicioProvisionesCartera;

    public EtapaProvisionesCartera(ServiceProvisionesCartera servicioProvisionesCartera) {
        this.servicioProvisionesCartera = servicioProvisionesCartera;
    }

    @Override
    public PasoCierre paso() {
        return PasoCierre.PROVISIONES;
    }

    @Override
    public int ejecutar(LocalDate fechaNegocio) {
        return this.servicioProvisionesCartera.provisionarCartera(fechaNegocio);
    }
}
