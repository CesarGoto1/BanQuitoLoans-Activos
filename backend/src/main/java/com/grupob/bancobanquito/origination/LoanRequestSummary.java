package com.grupob.bancobanquito.origination;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


public record LoanRequestSummary(
        Integer idSolicitud,
        String sistemaAmortizacion,
        BigDecimal montoSolicitado,
        Integer plazoMeses,
        String destinoCredito,
        LocalDate fechaSolicitud,
        LocalDateTime fechaHoraRegistro,
        BigDecimal cuotaEvaluacionReferencial,
        BigDecimal teaAsignada,
        BigDecimal cuotaEvaluacionDefinitiva,
        String estadoSolicitud,
        LocalDate fechaResolucion,
        String motivoRechazo) {
}















