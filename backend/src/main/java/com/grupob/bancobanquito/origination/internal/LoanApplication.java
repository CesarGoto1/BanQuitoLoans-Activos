package com.grupob.bancobanquito.origination.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "solicitudes")
@EntityListeners(AuditingEntityListener.class)
class LoanApplication {

    enum ApplicationStatus {
        REGISTERED,
        REJECTED,
        APPROVED
    }

    enum CreditPurpose {
        DURABLE_GOODS,
        HEALTH,
        EDUCATION,
        TRAVEL,
        OTHER
    }

    // Pasa al enum AmortizationSystem del módulo amortization cuando P1 lo publique.
    enum AmortizationSystem {
        FRENCH,
        GERMAN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idSolicitud")
    private Integer id;

    @NotNull
    @Column(name = "idCliente")
    private Integer customerId;

    @NotNull
    @Convert(converter = AmortizationSystemConverter.class)
    @Column(name = "sistemaAmortizacion")
    private AmortizationSystem amortizationSystem;

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
    private CreditPurpose creditPurpose;

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
    private BigDecimal assignedEffectiveAnnualRate;

    @Digits(integer = 8, fraction = 2)
    @Column(name = "cuotaEvaluacionDefinitiva")
    private BigDecimal finalInstallment;

    @NotNull
    @Convert(converter = ApplicationStatusConverter.class)
    @Column(name = "estadoSolicitud")
    private ApplicationStatus status;

    @Column(name = "fechaResolucion")
    private LocalDate resolutionDate;

    @Size(max = 200)
    @Column(name = "motivoRechazo")
    private String rejectionReason;

    @NotNull
    @Size(max = 50)
    @CreatedBy
    @Column(name = "usuarioCreacion", updatable = false)
    private String createdBy;

    @NotNull
    @CreatedDate
    @Column(name = "fechaCreacion", updatable = false)
    private LocalDateTime createdAt;

    @Size(max = 50)
    @LastModifiedBy
    @Column(name = "usuarioModificacion")
    private String modifiedBy;

    @LastModifiedDate
    @Column(name = "fechaModificacion")
    private LocalDateTime modifiedAt;

    protected LoanApplication() {
    }

    LoanApplication(Integer customerId, AmortizationSystem amortizationSystem, BigDecimal requestedAmount,
                    Integer termMonths, CreditPurpose creditPurpose, BigDecimal monthlyIncome,
                    BigDecimal monthlyExpenses, LocalDate applicationDate, LocalDateTime registeredAt,
                    BigDecimal referenceInstallment) {
        this.customerId = customerId;
        this.amortizationSystem = amortizationSystem;
        this.requestedAmount = requestedAmount;
        this.termMonths = termMonths;
        this.creditPurpose = creditPurpose;
        this.monthlyIncome = monthlyIncome;
        this.monthlyExpenses = monthlyExpenses;
        this.applicationDate = applicationDate;
        this.registeredAt = registeredAt;
        this.referenceInstallment = referenceInstallment;
        this.status = ApplicationStatus.REGISTERED;
    }

    void reject(String rejectionReason, LocalDate resolutionDate) {
        this.status = ApplicationStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.resolutionDate = resolutionDate;
    }

    Integer getId() {
        return id;
    }

    Integer getCustomerId() {
        return customerId;
    }

    AmortizationSystem getAmortizationSystem() {
        return amortizationSystem;
    }

    Integer getAnalystId() {
        return analystId;
    }

    BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    Integer getTermMonths() {
        return termMonths;
    }

    CreditPurpose getCreditPurpose() {
        return creditPurpose;
    }

    BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    BigDecimal getMonthlyExpenses() {
        return monthlyExpenses;
    }

    LocalDate getApplicationDate() {
        return applicationDate;
    }

    LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    BigDecimal getReferenceInstallment() {
        return referenceInstallment;
    }

    BigDecimal getAssignedEffectiveAnnualRate() {
        return assignedEffectiveAnnualRate;
    }

    BigDecimal getFinalInstallment() {
        return finalInstallment;
    }

    ApplicationStatus getStatus() {
        return status;
    }

    LocalDate getResolutionDate() {
        return resolutionDate;
    }

    String getRejectionReason() {
        return rejectionReason;
    }

    @Converter
    static class ApplicationStatusConverter implements AttributeConverter<ApplicationStatus, String> {

        @Override
        public String convertToDatabaseColumn(ApplicationStatus status) {
            if (status == null) {
                return null;
            }
            return switch (status) {
                case REGISTERED -> "REGISTRADA";
                case REJECTED -> "RECHAZADA";
                case APPROVED -> "APROBADA";
            };
        }

        @Override
        public ApplicationStatus convertToEntityAttribute(String value) {
            if (value == null) {
                return null;
            }
            return switch (value) {
                case "REGISTRADA" -> ApplicationStatus.REGISTERED;
                case "RECHAZADA" -> ApplicationStatus.REJECTED;
                case "APROBADA" -> ApplicationStatus.APPROVED;
                default -> throw new IllegalArgumentException("Unknown application status: " + value);
            };
        }
    }

    @Converter
    static class CreditPurposeConverter implements AttributeConverter<CreditPurpose, String> {

        @Override
        public String convertToDatabaseColumn(CreditPurpose purpose) {
            if (purpose == null) {
                return null;
            }
            return switch (purpose) {
                case DURABLE_GOODS -> "BIENES_DURADEROS";
                case HEALTH -> "SALUD";
                case EDUCATION -> "EDUCACION";
                case TRAVEL -> "VIAJE";
                case OTHER -> "OTRO";
            };
        }

        @Override
        public CreditPurpose convertToEntityAttribute(String value) {
            if (value == null) {
                return null;
            }
            return switch (value) {
                case "BIENES_DURADEROS" -> CreditPurpose.DURABLE_GOODS;
                case "SALUD" -> CreditPurpose.HEALTH;
                case "EDUCACION" -> CreditPurpose.EDUCATION;
                case "VIAJE" -> CreditPurpose.TRAVEL;
                case "OTRO" -> CreditPurpose.OTHER;
                default -> throw new IllegalArgumentException("Unknown credit purpose: " + value);
            };
        }
    }

    @Converter
    static class AmortizationSystemConverter implements AttributeConverter<AmortizationSystem, String> {

        @Override
        public String convertToDatabaseColumn(AmortizationSystem system) {
            if (system == null) {
                return null;
            }
            return switch (system) {
                case FRENCH -> "FRANCES";
                case GERMAN -> "ALEMAN";
            };
        }

        @Override
        public AmortizationSystem convertToEntityAttribute(String value) {
            if (value == null) {
                return null;
            }
            return switch (value) {
                case "FRANCES" -> AmortizationSystem.FRENCH;
                case "ALEMAN" -> AmortizationSystem.GERMAN;
                default -> throw new IllegalArgumentException("Unknown amortization system: " + value);
            };
        }
    }
}
