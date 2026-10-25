package com.grupob.bancobanquito.loans.internal.controllers;
import com.grupob.bancobanquito.loans.internal.entities.*;
import com.grupob.bancobanquito.loans.internal.repositories.*;
import com.grupob.bancobanquito.loans.internal.services.*;
import com.grupob.bancobanquito.loans.internal.controllers.*;
import com.grupob.bancobanquito.loans.internal.exceptions.*;
import com.grupob.bancobanquito.loans.InstallmentSummary;
import java.math.BigDecimal;
import java.time.LocalDate;


public record InstallmentResponse(
        Integer numeroCuota,
        LocalDate fechaVencimiento,
        BigDecimal valorCapital,
        BigDecimal valorInteres,
        BigDecimal valorSeguro,
        BigDecimal totalCuota,
        BigDecimal saldoCapitalPosterior,
        BigDecimal capitalPagado,
        BigDecimal interesPagado,
        BigDecimal seguroPagado,
        BigDecimal moraPagada,
        BigDecimal moraAcumulada,
        String estadoCuota,
        LocalDate fechaPago) {

    public static InstallmentResponse from(InstallmentSummary r) {
        return new InstallmentResponse(r.numeroCuota(), r.fechaVencimiento(), r.valorCapital(), r.valorInteres(),
                r.valorSeguro(), r.totalCuota(), r.saldoCapitalPosterior(), r.capitalPagado(), r.interesPagado(),
                r.seguroPagado(), r.moraPagada(), r.moraAcumulada(), r.estadoCuota(), r.fechaPago());
    }
}
















