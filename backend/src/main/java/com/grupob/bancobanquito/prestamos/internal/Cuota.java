package com.grupob.bancobanquito.prestamos.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "cuotas")
class Cuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCuota")
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idOperacion")
    private Prestamo loan;

    @NotNull
    @Column(name = "numeroCuota")
    private Integer numeroCuota;

    @NotNull
    @Column(name = "fechaVencimiento")
    private LocalDate fechaVencimiento;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "valorCapital")
    private BigDecimal principalAmount;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "valorInteres")
    private BigDecimal interestAmount;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "valorSeguro")
    private BigDecimal insuranceAmount;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Digits(integer = 8, fraction = 2)
    @Column(name = "totalCuota", insertable = false, updatable = false)
    private BigDecimal totalAmount;

    @Digits(integer = 8, fraction = 2)
    @Column(name = "ajusteDevengoInteres")
    private BigDecimal interestAccrualAdjustment;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "saldoCapitalPosterior")
    private BigDecimal remainingPrincipalBalance;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "seguroPagado")
    private BigDecimal insurancePaid;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "moraPagada")
    private BigDecimal defaultInterestPaid;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "interesPagado")
    private BigDecimal interestPaid;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "capitalPagado")
    private BigDecimal principalPaid;

    @NotNull
    @Digits(integer = 6, fraction = 4)
    @Column(name = "moraAcumulada")
    private BigDecimal accruedDefaultInterest;

    @NotNull
    @Convert(converter = ConversorEstadoCuota.class)
    @Column(name = "estadoCuota")
    private EstadoCuota status;

    @Column(name = "fechaPago")
    private LocalDate paymentDate;

    @NotNull
    @Size(max = 50)
    @Column(name = "usuarioCreacion", updatable = false)
    private String createdBy;

    @NotNull
    @Column(name = "fechaCreacion", updatable = false)
    private LocalDateTime createdAt;

    @Size(max = 50)
    @Column(name = "usuarioModificacion")
    private String lastModifiedBy;

    @Column(name = "fechaModificacion")
    private LocalDateTime lastModifiedAt;

    Cuota() {
    }

    Cuota(Integer id) {
        this.id = id;
    }

    Integer getId() {
        return id;
    }

    void setId(Integer id) {
        this.id = id;
    }

    Prestamo getLoan() {
        return loan;
    }

    void setLoan(Prestamo loan) {
        this.loan = loan;
    }

    Integer getInstallmentNumber() {
        return numeroCuota;
    }

    void setInstallmentNumber(Integer numeroCuota) {
        this.numeroCuota = numeroCuota;
    }

    LocalDate getDueDate() {
        return fechaVencimiento;
    }

    void setDueDate(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    BigDecimal getInterestAmount() {
        return interestAmount;
    }

    void setInterestAmount(BigDecimal interestAmount) {
        this.interestAmount = interestAmount;
    }

    BigDecimal getInsuranceAmount() {
        return insuranceAmount;
    }

    void setInsuranceAmount(BigDecimal insuranceAmount) {
        this.insuranceAmount = insuranceAmount;
    }

    BigDecimal getTotalAmount() {
        return totalAmount;
    }

    BigDecimal getInterestAccrualAdjustment() {
        return interestAccrualAdjustment;
    }

    void setInterestAccrualAdjustment(BigDecimal interestAccrualAdjustment) {
        this.interestAccrualAdjustment = interestAccrualAdjustment;
    }

    BigDecimal getRemainingPrincipalBalance() {
        return remainingPrincipalBalance;
    }

    void setRemainingPrincipalBalance(BigDecimal remainingPrincipalBalance) {
        this.remainingPrincipalBalance = remainingPrincipalBalance;
    }

    BigDecimal getInsurancePaid() {
        return insurancePaid;
    }

    void setInsurancePaid(BigDecimal insurancePaid) {
        this.insurancePaid = insurancePaid;
    }

    BigDecimal getDefaultInterestPaid() {
        return defaultInterestPaid;
    }

    void setDefaultInterestPaid(BigDecimal defaultInterestPaid) {
        this.defaultInterestPaid = defaultInterestPaid;
    }

    BigDecimal getInterestPaid() {
        return interestPaid;
    }

    void setInterestPaid(BigDecimal interestPaid) {
        this.interestPaid = interestPaid;
    }

    BigDecimal getPrincipalPaid() {
        return principalPaid;
    }

    void setPrincipalPaid(BigDecimal principalPaid) {
        this.principalPaid = principalPaid;
    }

    BigDecimal getAccruedDefaultInterest() {
        return accruedDefaultInterest;
    }

    void setAccruedDefaultInterest(BigDecimal accruedDefaultInterest) {
        this.accruedDefaultInterest = accruedDefaultInterest;
    }

    EstadoCuota getStatus() {
        return status;
    }

    void setStatus(EstadoCuota status) {
        this.status = status;
    }

    LocalDate getPaymentDate() {
        return paymentDate;
    }

    void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    String getCreatedBy() {
        return createdBy;
    }

    void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    LocalDateTime getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    String getLastModifiedBy() {
        return lastModifiedBy;
    }

    void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
}
