package com.grupob.bancobanquito.loans.internal;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import com.grupob.bancobanquito.loans.PortfolioStatus;
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
@Table(name = "operaciones")
class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idOperacion")
    private Integer id;

    @NotNull
    @Column(name = "idSolicitud")
    private Integer applicationId;

    @NotNull
    @Column(name = "idCliente")
    private Integer customerId;

    @NotNull
    @Size(max = 10)
    @Column(name = "cedulaCliente")
    private String customerNationalId;

    @NotNull
    @Convert(converter = AmortizationMethodConverter.class)
    @Column(name = "sistemaAmortizacion")
    private AmortizationMethod amortizationMethod;

    @NotNull
    @Size(max = 20)
    @Column(name = "numeroOperacion")
    private String loanNumber;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "montoAprobado")
    private BigDecimal approvedAmount;

    @NotNull
    @Digits(integer = 1, fraction = 4)
    @Column(name = "teaAsignada")
    private BigDecimal effectiveAnnualRate;

    @NotNull
    @Column(name = "plazoMeses")
    private Integer termMonths;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "seguroMensual")
    private BigDecimal monthlyInsurance;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "contribucionSolca")
    private BigDecimal solcaContribution;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "montoNeto")
    private BigDecimal netAmount;

    @NotNull
    @Size(max = 20)
    @Column(name = "cuentaDestino")
    private String destinationAccount;

    @NotNull
    @Column(name = "fechaDesembolso")
    private LocalDate disbursementDate;

    @NotNull
    @Convert(converter = DisbursementInstructionStatusConverter.class)
    @Column(name = "estadoInstruccionDesembolso")
    private DisbursementInstructionStatus disbursementInstructionStatus;

    @NotNull
    @Convert(converter = PortfolioStatusConverter.class)
    @Column(name = "estadoCartera")
    private PortfolioStatus portfolioStatus;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "saldoCapital")
    private BigDecimal principalBalance;

    @NotNull
    @Column(name = "diasMora")
    private Integer daysPastDue;

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

    Loan() {
    }

    Loan(Integer id) {
        this.id = id;
    }

    Integer getId() {
        return id;
    }

    void setId(Integer id) {
        this.id = id;
    }

    Integer getApplicationId() {
        return applicationId;
    }

    void setApplicationId(Integer applicationId) {
        this.applicationId = applicationId;
    }

    Integer getCustomerId() {
        return customerId;
    }

    void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    String getCustomerNationalId() {
        return customerNationalId;
    }

    void setCustomerNationalId(String customerNationalId) {
        this.customerNationalId = customerNationalId;
    }

    AmortizationMethod getAmortizationMethod() {
        return amortizationMethod;
    }

    void setAmortizationMethod(AmortizationMethod amortizationMethod) {
        this.amortizationMethod = amortizationMethod;
    }

    String getLoanNumber() {
        return loanNumber;
    }

    void setLoanNumber(String loanNumber) {
        this.loanNumber = loanNumber;
    }

    BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    BigDecimal getEffectiveAnnualRate() {
        return effectiveAnnualRate;
    }

    void setEffectiveAnnualRate(BigDecimal effectiveAnnualRate) {
        this.effectiveAnnualRate = effectiveAnnualRate;
    }

    Integer getTermMonths() {
        return termMonths;
    }

    void setTermMonths(Integer termMonths) {
        this.termMonths = termMonths;
    }

    BigDecimal getMonthlyInsurance() {
        return monthlyInsurance;
    }

    void setMonthlyInsurance(BigDecimal monthlyInsurance) {
        this.monthlyInsurance = monthlyInsurance;
    }

    BigDecimal getSolcaContribution() {
        return solcaContribution;
    }

    void setSolcaContribution(BigDecimal solcaContribution) {
        this.solcaContribution = solcaContribution;
    }

    BigDecimal getNetAmount() {
        return netAmount;
    }

    void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    String getDestinationAccount() {
        return destinationAccount;
    }

    void setDestinationAccount(String destinationAccount) {
        this.destinationAccount = destinationAccount;
    }

    LocalDate getDisbursementDate() {
        return disbursementDate;
    }

    void setDisbursementDate(LocalDate disbursementDate) {
        this.disbursementDate = disbursementDate;
    }

    DisbursementInstructionStatus getDisbursementInstructionStatus() {
        return disbursementInstructionStatus;
    }

    void setDisbursementInstructionStatus(DisbursementInstructionStatus disbursementInstructionStatus) {
        this.disbursementInstructionStatus = disbursementInstructionStatus;
    }

    PortfolioStatus getPortfolioStatus() {
        return portfolioStatus;
    }

    void setPortfolioStatus(PortfolioStatus portfolioStatus) {
        this.portfolioStatus = portfolioStatus;
    }

    BigDecimal getPrincipalBalance() {
        return principalBalance;
    }

    void setPrincipalBalance(BigDecimal principalBalance) {
        this.principalBalance = principalBalance;
    }

    Integer getDaysPastDue() {
        return daysPastDue;
    }

    void setDaysPastDue(Integer daysPastDue) {
        this.daysPastDue = daysPastDue;
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
