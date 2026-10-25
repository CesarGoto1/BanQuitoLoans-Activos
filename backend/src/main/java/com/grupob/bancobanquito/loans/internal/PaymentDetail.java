package com.grupob.bancobanquito.loans.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "detallesPago")
class PaymentDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDetallePago")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idPago", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCuota")
    private Installment installment;

    @Column(name = "aplicadoSeguro", nullable = false)
    private BigDecimal appliedInsurance;

    @Column(name = "aplicadoMora", nullable = false)
    private BigDecimal appliedPastDueInterest;

    @Column(name = "aplicadoInteres", nullable = false)
    private BigDecimal appliedInterest;

    @Column(name = "aplicadoCapital", nullable = false)
    private BigDecimal appliedPrincipal;

    Integer installmentNumber() { return installment == null ? null : installment.getNumber(); }
    BigDecimal getAppliedInsurance() { return appliedInsurance; }
    BigDecimal getAppliedPastDueInterest() { return appliedPastDueInterest; }
    BigDecimal getAppliedInterest() { return appliedInterest; }
    BigDecimal getAppliedPrincipal() { return appliedPrincipal; }
}
