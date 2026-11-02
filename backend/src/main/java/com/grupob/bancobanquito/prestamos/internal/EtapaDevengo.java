package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.cierre.EtapaCierre;
import com.grupob.bancobanquito.cierre.PasoCierre;
import com.grupob.bancobanquito.prestamos.ServiceDevengoMora;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

// Paso 2 del cierre de día: devengo diario de intereses y ajuste de las cuotas que vencen ese día.
@Component
class EtapaDevengo implements EtapaCierre {

    private final ServiceDevengoMora servicioDevengoMora;

    EtapaDevengo(ServiceDevengoMora servicioDevengoMora) {
        this.servicioDevengoMora = servicioDevengoMora;
    }

    @Override
    public PasoCierre paso() {
        return PasoCierre.DEVENGO;
    }

    @Override
    public int ejecutar(LocalDate fechaNegocio) {
        return servicioDevengoMora.ejecutarDevengo(fechaNegocio)
                + servicioDevengoMora.ejecutarAjustesDevengo(fechaNegocio);
    }
}
