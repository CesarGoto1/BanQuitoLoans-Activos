package com.grupob.bancobanquito.loans;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumenPrestamo(
        String numeroOperacion,
        Integer idSolicitud,
        String cedulaCliente,
        BigDecimal montoAprobado,
        BigDecimal tasaEfectivaAnual,
        Integer plazoMeses,
        AmortizationMethod sistemaAmortizacion,
        LocalDate fechaDesembolso,
        BigDecimal saldoCapital,
        Integer diasMora,
        EstadoCartera estadoCartera) {
}
