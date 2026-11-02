package com.grupob.bancobanquito.cobranza;

import java.time.LocalDate;

public interface ServiceProvisionesCartera {

    int provisionarCartera(LocalDate fechaNegocio);
}
