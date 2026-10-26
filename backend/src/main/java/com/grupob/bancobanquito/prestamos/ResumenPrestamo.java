package com.grupob.bancobanquito.prestamos;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumenPrestamo(
        String numeroOperacion,
        Integer idSolicitud,
        String cedulaCliente,
        BigDecimal montoAprobado,
        BigDecimal tasaEfectivaAnual,
        Integer plazoMeses,
        MetodoAmortizacion sistemaAmortizacion,
        LocalDate fechaDesembolso,
        BigDecimal saldoCapital,
        Integer diasMora,
        EstadoCartera estadoCartera) {
}
