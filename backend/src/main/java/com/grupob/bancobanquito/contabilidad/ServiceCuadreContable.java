package com.grupob.bancobanquito.contabilidad;

import java.time.LocalDate;

public interface ServiceCuadreContable {

    ResultadoCuadreContable consultarCuadre(LocalDate fechaNegocio);

    ResultadoCuadreContable consultarCuadreActual();
}
