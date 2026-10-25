package com.grupob.bancobanquito.loans.internal.controllers;
import com.grupob.bancobanquito.loans.internal.entities.*;
import com.grupob.bancobanquito.loans.internal.repositories.*;
import com.grupob.bancobanquito.loans.internal.services.*;
import com.grupob.bancobanquito.loans.internal.controllers.*;
import com.grupob.bancobanquito.loans.internal.exceptions.*;
import com.grupob.bancobanquito.loans.LoanOperationSummary;
import java.math.BigDecimal;
import java.time.LocalDate;


public record LoanOperationResponse(
        Integer idSolicitud,
        String numeroOperacion,
        String sistemaAmortizacion,
        BigDecimal montoAprobado,
        BigDecimal teaAsignada,
        Integer plazoMeses,
        BigDecimal seguroMensual,
        BigDecimal contribucionSolca,
        BigDecimal montoNeto,
        String cuentaDestino,
        LocalDate fechaDesembolso,
        String estadoInstruccionDesembolso,
        String estadoCartera,
        BigDecimal saldoCapital,
        Integer diasMora) {

    public static LoanOperationResponse from(LoanOperationSummary r) {
        return new LoanOperationResponse(r.idSolicitud(), r.numeroOperacion(), r.sistemaAmortizacion(),
                r.montoAprobado(), r.teaAsignada(), r.plazoMeses(), r.seguroMensual(), r.contribucionSolca(),
                r.montoNeto(), r.cuentaDestino(), r.fechaDesembolso(), r.estadoInstruccionDesembolso(),
                r.estadoCartera(), r.saldoCapital(), r.diasMora());
    }
}
















