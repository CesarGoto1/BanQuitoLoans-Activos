package com.grupob.bancobanquito.contabilidad;

import java.time.LocalDate;

public interface ServiceProvisiones {

    ResultadoProvisiones calcular(LocalDate fechaNegocio);
}
