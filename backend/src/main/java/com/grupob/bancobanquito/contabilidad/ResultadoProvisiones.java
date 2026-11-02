package com.grupob.bancobanquito.contabilidad;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResultadoProvisiones(LocalDate fechaNegocio, int operacionesProcesadas,
                                   int asientosGenerados, BigDecimal variacionTotal) {
}
