package com.grupob.bancobanquito.contabilidad;

import java.time.LocalDate;
import java.util.List;

public interface ServiceProvisiones {

    ResultadoProvisiones registrarProvisiones(LocalDate fechaAsiento, List<DatosProvision> operaciones);
}
