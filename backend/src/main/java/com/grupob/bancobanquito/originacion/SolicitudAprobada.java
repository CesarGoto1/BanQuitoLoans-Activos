package com.grupob.bancobanquito.originacion;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import java.math.BigDecimal;

public record SolicitudAprobada(
        Integer idSolicitud,
        Integer idCliente,
        BigDecimal montoAprobado,
        BigDecimal teaAsignada,
        Integer plazoMeses,
        MetodoAmortizacion metodoAmortizacion) {
}
