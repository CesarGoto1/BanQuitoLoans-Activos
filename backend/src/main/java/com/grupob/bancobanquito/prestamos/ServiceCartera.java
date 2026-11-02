package com.grupob.bancobanquito.prestamos;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ServiceCartera {

    Optional<OperacionPorPagar> buscarOperacionPorPagar(String numeroOperacion);

    EstadoCartera aplicarImputacion(Integer idOperacion, LocalDate fechaPago, List<ImputacionCuota> imputaciones);
}
