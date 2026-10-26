package com.grupob.bancobanquito.originacion.internal;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

record RespuestaSolicitud(
        Integer idSolicitud,
        Integer idCliente,
        BigDecimal monto,
        Integer plazoMeses,
        MetodoAmortizacion metodoAmortizacion,
        DestinoCredito destinoCredito,
        BigDecimal ingresosMensuales,
        BigDecimal gastosMensuales,
        LocalDate fechaSolicitud,
        LocalDateTime fechaHoraRegistro,
        BigDecimal cuotaEvaluacionReferencial,
        BigDecimal tasaAsignada,
        EstadoSolicitud status,
        LocalDate fechaResolucion,
        String motivoRechazo) {

    static RespuestaSolicitud from(Solicitud application) {
        return new RespuestaSolicitud(application.getId(), application.getCustomerId(),
                application.getRequestedAmount(), application.getTermMonths(), application.getAmortizationMethod(),
                application.getPurpose(), application.getMonthlyIncome(), application.getMonthlyExpenses(),
                application.getApplicationDate(), application.getRegisteredAt(), application.getReferenceInstallment(),
                application.getAssignedRate(), application.getStatus(), application.getResolutionDate(),
                application.getRejectionReason());
    }
}
