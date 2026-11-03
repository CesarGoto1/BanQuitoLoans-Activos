package com.grupob.bancobanquito.pasivos;

import java.math.BigDecimal;

public record ArchivoRecaudacionGenerado(String nombreArchivo, int cantidadCobros, BigDecimal montoTotal) {
}
