package com.grupob.bancobanquito.origination;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SolicitudDetalleResponse(
        Integer idSolicitud,
        Integer idCliente,
        String estadoSolicitud,
        LocalDate fechaSolicitud,
        LocalDateTime fechaHoraRegistro,
        BigDecimal montoSolicitado,
        Integer plazoMeses,
        String sistemaAmortizacion,
        String destinoCredito,
        BigDecimal cuotaEvaluacionReferencial,
        BigDecimal ingresosMensuales,
        BigDecimal gastosMensuales,
        BigDecimal ingresoRemanente,
        BigDecimal porcentajeImpactoCuota,
        BigDecimal limiteImpactoMaximo,
        BigDecimal cuotaMaximaAdmisible,
        boolean capacidadPagoValida,
        String mensajeCapacidadPago,
        BigDecimal teaAsignada,
        BigDecimal cuotaEvaluacionDefinitiva,
        LocalDate fechaResolucion,
        String motivoRechazo,
        Integer idAnalista
) {
}
