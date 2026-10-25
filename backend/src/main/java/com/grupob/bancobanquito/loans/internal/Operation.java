package com.grupob.bancobanquito.loans.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "operaciones")
class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idOperacion")
    private Integer id;

    @Column(name = "idCliente", nullable = false)
    private Integer clientId;

    @Column(name = "cedulaCliente", nullable = false)
    private String clientIdentification;

    @Column(name = "sistemaAmortizacion", nullable = false)
    private String amortizationSystem;

    @Column(name = "numeroOperacion", nullable = false)
    private String operationNumber;

    @Column(name = "montoAprobado", nullable = false)
    private BigDecimal approvedAmount;

    @Column(name = "teaAsignada", nullable = false)
    private BigDecimal assignedTea;

    @Column(name = "plazoMeses", nullable = false)
    private Integer termMonths;

    @Column(name = "seguroMensual", nullable = false)
    private BigDecimal monthlyInsurance;

    @Column(name = "montoNeto", nullable = false)
    private BigDecimal netAmount;

    @Column(name = "fechaDesembolso", nullable = false)
    private LocalDate disbursementDate;

    @Column(name = "estadoCartera", nullable = false)
    private String portfolioStatus;

    @Column(name = "saldoCapital", nullable = false)
    private BigDecimal principalBalance;

    @Column(name = "diasMora", nullable = false)
    private Integer daysPastDue;

    @OneToMany(mappedBy = "operation", fetch = FetchType.LAZY)
    private List<Installment> installments = new ArrayList<>();

    @OneToMany(mappedBy = "operation", fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();

    Integer getId() { return id; }
    Integer getClientId() { return clientId; }
    String getClientIdentification() { return clientIdentification; }
    String getAmortizationSystem() { return amortizationSystem; }
    String getOperationNumber() { return operationNumber; }
    BigDecimal getApprovedAmount() { return approvedAmount; }
    BigDecimal getAssignedTea() { return assignedTea; }
    Integer getTermMonths() { return termMonths; }
    BigDecimal getMonthlyInsurance() { return monthlyInsurance; }
    BigDecimal getNetAmount() { return netAmount; }
    LocalDate getDisbursementDate() { return disbursementDate; }
    String getPortfolioStatus() { return portfolioStatus; }
    BigDecimal getPrincipalBalance() { return principalBalance; }
    Integer getDaysPastDue() { return daysPastDue; }
    List<Installment> getInstallments() { return installments; }
    List<Payment> getPayments() { return payments; }
}
