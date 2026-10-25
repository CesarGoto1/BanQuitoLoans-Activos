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
import java.time.LocalDate;

@Entity
@Table(name = "cuotas")
class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCuota")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idOperacion", nullable = false)
    private Operation operation;

    @Column(name = "numeroCuota", nullable = false)
    private Integer number;

    @Column(name = "fechaVencimiento", nullable = false)
    private LocalDate dueDate;

    @Column(name = "valorCapital", nullable = false)
    private BigDecimal principal;

    @Column(name = "valorInteres", nullable = false)
    private BigDecimal interest;

    @Column(name = "valorSeguro", nullable = false)
    private BigDecimal insurance;

    @Column(name = "totalCuota", insertable = false, updatable = false)
    private BigDecimal total;

    @Column(name = "seguroPagado", nullable = false)
    private BigDecimal paidInsurance;

    @Column(name = "moraPagada", nullable = false)
    private BigDecimal paidPastDueInterest;

    @Column(name = "interesPagado", nullable = false)
    private BigDecimal paidInterest;

    @Column(name = "capitalPagado", nullable = false)
    private BigDecimal paidPrincipal;

    @Column(name = "moraAcumulada", nullable = false)
    private BigDecimal accruedPastDueInterest;

    @Column(name = "estadoCuota", nullable = false)
    private String status;

    @Column(name = "fechaPago")
    private LocalDate paymentDate;

    Integer getNumber() { return number; }
    LocalDate getDueDate() { return dueDate; }
    BigDecimal getPrincipal() { return principal; }
    BigDecimal getInterest() { return interest; }
    BigDecimal getInsurance() { return insurance; }
    BigDecimal getTotal() { return total; }
    BigDecimal getPaidInsurance() { return paidInsurance; }
    BigDecimal getPaidPastDueInterest() { return paidPastDueInterest; }
    BigDecimal getPaidInterest() { return paidInterest; }
    BigDecimal getPaidPrincipal() { return paidPrincipal; }
    BigDecimal getAccruedPastDueInterest() { return accruedPastDueInterest; }
    String getStatus() { return status; }
    LocalDate getPaymentDate() { return paymentDate; }
}
