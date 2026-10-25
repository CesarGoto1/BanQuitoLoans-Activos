package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes")
class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idSolicitud")
    private Integer id;

    @NotNull
    @Column(name = "idCliente")
    private Integer customerId;

    @NotNull
    @Convert(converter = AmortizationMethodConverter.class)
    @Column(name = "sistemaAmortizacion")
    private AmortizationMethod amortizationMethod;

    @Column(name = "idAnalista")
    private Integer analystId;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "montoSolicitado")
    private BigDecimal requestedAmount;

    @NotNull
    @Column(name = "plazoMeses")
    private Integer termMonths;

    @NotNull
    @Convert(converter = CreditPurposeConverter.class)
    @Column(name = "destinoCredito")
    private CreditPurpose purpose;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "ingresosMensuales")
    private BigDecimal monthlyIncome;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "gastosMensuales")
    private BigDecimal monthlyExpenses;

    @NotNull
    @Column(name = "fechaSolicitud")
    private LocalDate applicationDate;

    @NotNull
    @Column(name = "fechaHoraRegistro")
    private LocalDateTime registeredAt;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "cuotaEvaluacionReferencial")
    private BigDecimal referenceInstallment;

    @Digits(integer = 1, fraction = 4)
    @Column(name = "teaAsignada")
    private BigDecimal assignedRate;

    @Digits(integer = 8, fraction = 2)
    @Column(name = "cuotaEvaluacionDefinitiva")
    private BigDecimal finalInstallment;

    @NotNull
    @Convert(converter = LoanApplicationStatusConverter.class)
    @Column(name = "estadoSolicitud")
    private LoanApplicationStatus status;

    @Column(name = "fechaResolucion")
    private LocalDate resolutionDate;

    @Size(max = 200)
    @Column(name = "motivoRechazo")
    private String rejectionReason;

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

    LoanApplication() {
    }

    LoanApplication(Integer id) {
        this.id = id;
    }

    Integer getId() {
        return id;
    }

    void setId(Integer id) {
        this.id = id;
    }

    Integer getCustomerId() {
        return customerId;
    }

    void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    AmortizationMethod getAmortizationMethod() {
        return amortizationMethod;
    }

    void setAmortizationMethod(AmortizationMethod amortizationMethod) {
        this.amortizationMethod = amortizationMethod;
    }

    Integer getAnalystId() {
        return analystId;
    }

    void setAnalystId(Integer analystId) {
        this.analystId = analystId;
    }

    BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    Integer getTermMonths() {
        return termMonths;
    }

    void setTermMonths(Integer termMonths) {
        this.termMonths = termMonths;
    }

    CreditPurpose getPurpose() {
        return purpose;
    }

    void setPurpose(CreditPurpose purpose) {
        this.purpose = purpose;
    }

    BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    BigDecimal getMonthlyExpenses() {
        return monthlyExpenses;
    }

    void setMonthlyExpenses(BigDecimal monthlyExpenses) {
        this.monthlyExpenses = monthlyExpenses;
    }

    LocalDate getApplicationDate() {
        return applicationDate;
    }

    void setApplicationDate(LocalDate applicationDate) {
        this.applicationDate = applicationDate;
    }

    LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    BigDecimal getReferenceInstallment() {
        return referenceInstallment;
    }

    void setReferenceInstallment(BigDecimal referenceInstallment) {
        this.referenceInstallment = referenceInstallment;
    }

    BigDecimal getAssignedRate() {
        return assignedRate;
    }

    void setAssignedRate(BigDecimal assignedRate) {
        this.assignedRate = assignedRate;
    }

    BigDecimal getFinalInstallment() {
        return finalInstallment;
    }

    void setFinalInstallment(BigDecimal finalInstallment) {
        this.finalInstallment = finalInstallment;
    }

    LoanApplicationStatus getStatus() {
        return status;
    }

    void setStatus(LoanApplicationStatus status) {
        this.status = status;
    }

    LocalDate getResolutionDate() {
        return resolutionDate;
    }

    void setResolutionDate(LocalDate resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    String getRejectionReason() {
        return rejectionReason;
    }

    void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
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
