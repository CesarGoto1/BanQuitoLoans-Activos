package com.grupob.bancobanquito.originacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SolicitudBandejaResponse(
        Integer idSolicitud,
        Integer idCliente,
        String cedulaCliente,
        String nombreCliente,
        BigDecimal montoSolicitado,
        Integer plazoMeses,
        String sistemaAmortizacion,
        String destinoCredito,
        BigDecimal cuotaEvaluacionReferencial,
        LocalDate fechaSolicitud,
        LocalDateTime fechaHoraRegistro,
        String estadoSolicitud
) {
}
