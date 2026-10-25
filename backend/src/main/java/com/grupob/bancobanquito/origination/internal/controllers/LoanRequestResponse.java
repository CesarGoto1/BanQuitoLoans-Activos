package com.grupob.bancobanquito.origination.internal.controllers;
import com.grupob.bancobanquito.origination.internal.entities.*;
import com.grupob.bancobanquito.origination.internal.repositories.*;
import com.grupob.bancobanquito.origination.internal.services.*;
import com.grupob.bancobanquito.origination.internal.controllers.*;
import com.grupob.bancobanquito.origination.internal.exceptions.*;
import com.grupob.bancobanquito.origination.LoanRequestSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


public record LoanRequestResponse(
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

    public static LoanRequestResponse from(LoanRequestSummary r) {
        return new LoanRequestResponse(r.idSolicitud(), r.sistemaAmortizacion(), r.montoSolicitado(), r.plazoMeses(),
                r.destinoCredito(), r.fechaSolicitud(), r.fechaHoraRegistro(), r.cuotaEvaluacionReferencial(),
                r.teaAsignada(), r.cuotaEvaluacionDefinitiva(), r.estadoSolicitud(), r.fechaResolucion(),
                r.motivoRechazo());
    }
}
















