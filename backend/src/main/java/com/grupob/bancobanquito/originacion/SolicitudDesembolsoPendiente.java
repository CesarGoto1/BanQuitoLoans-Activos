package com.grupob.bancobanquito.originacion;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SolicitudDesembolsoPendiente(
        Integer idSolicitud,
        Integer idCliente,
        BigDecimal montoAprobado,
        BigDecimal teaAsignada,
        Integer plazoMeses,
        MetodoAmortizacion metodoAmortizacion,
        LocalDate fechaResolucion) {
}
