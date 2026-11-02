package com.grupob.bancobanquito.prestamos;

import java.time.LocalDate;

public record MovimientoEstado(
        LocalDate fechaCambio,
        EstadoCartera estadoAnterior,
        EstadoCartera estadoNuevo,
        Integer diasMora,
        String registradoPor) {
}
