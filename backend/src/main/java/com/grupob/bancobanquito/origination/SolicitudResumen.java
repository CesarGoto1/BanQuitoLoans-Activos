package com.grupob.bancobanquito.origination;

import java.math.BigDecimal;

public record SolicitudResumen(
        Integer idSolicitud,
        Integer idCliente,
        BigDecimal montoSolicitado,
        Integer plazoMeses,
        String sistemaAmortizacion,
        String estadoSolicitud
) {
}
