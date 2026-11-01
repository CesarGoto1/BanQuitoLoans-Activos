package com.grupob.bancobanquito.prestamos;

import com.grupob.bancobanquito.amortizacion.ModalidadAbono;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ResultadoAbono(
        String numeroOperacion,
        LocalDate fechaAbono,
        ModalidadAbono modalidad,
        BigDecimal montoAbono,
        BigDecimal interesLiquidado,
        BigDecimal capitalAbonado,
        BigDecimal saldoAnterior,
        BigDecimal saldoNuevo,
        CuadroAmortizacion antes,
        CuadroAmortizacion despues) {
}
