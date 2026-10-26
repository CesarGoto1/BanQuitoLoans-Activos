package com.grupob.bancobanquito.prestamos;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DesembolsoPendiente(
        Integer idSolicitud,
        Integer idCliente,
        String cedulaCliente,
        String nombreCliente,
        BigDecimal montoAprobado,
        BigDecimal teaAsignada,
        Integer plazoMeses,
        MetodoAmortizacion metodoAmortizacion,
        LocalDate fechaResolucion) {
}
