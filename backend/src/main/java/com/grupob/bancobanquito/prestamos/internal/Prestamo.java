package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idOperacion")
    private Integer id;

    @NotNull
    @Column(name = "idSolicitud")
    private Integer idSolicitud;

    @NotNull
    @Column(name = "idCliente")
    private Integer idCliente;

    @NotNull
    @Size(max = 10)
    @Column(name = "cedulaCliente")
    private String cedulaCliente;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "sistemaAmortizacion")
    private MetodoAmortizacion metodoAmortizacion;

    @NotNull
    @Size(max = 20)
    @Column(name = "numeroOperacion")
    private String numeroOperacion;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "montoAprobado")
    private BigDecimal montoAprobado;

    @NotNull
    @Digits(integer = 1, fraction = 4)
    @Column(name = "teaAsignada")
    private BigDecimal teaAsignada;

    @NotNull
    @Column(name = "plazoMeses")
    private Integer plazoMeses;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "seguroMensual")
    private BigDecimal seguroMensual;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "contribucionSolca")
    private BigDecimal contribucionSolca;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "montoNeto")
    private BigDecimal montoNeto;

    @NotNull
    @Size(max = 20)
    @Column(name = "cuentaDestino")
    private String cuentaDestino;

    @NotNull
    @Column(name = "fechaDesembolso")
    private LocalDate fechaDesembolso;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estadoInstruccionDesembolso")
    private EstadoInstruccionDesembolso disbursementInstructionStatus;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estadoCartera")
    private EstadoCartera portfolioStatus;

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

    Prestamo() {
    }

    Prestamo(Integer id) {
        this.id = id;
    }

    Integer getId() {
        return id;
    }

    void setId(Integer id) {
        this.id = id;
    }

    Integer getApplicationId() {
        return idSolicitud;
    }

    void setApplicationId(Integer idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    Integer getCustomerId() {
        return idCliente;
    }

    void setCustomerId(Integer idCliente) {
        this.idCliente = idCliente;
    }

    String getCustomerNationalId() {
        return cedulaCliente;
    }

    void setCustomerNationalId(String cedulaCliente) {
        this.cedulaCliente = cedulaCliente;
    }

    MetodoAmortizacion getAmortizationMethod() {
        return metodoAmortizacion;
    }

    void setAmortizationMethod(MetodoAmortizacion metodoAmortizacion) {
        this.metodoAmortizacion = metodoAmortizacion;
    }

    String getLoanNumber() {
        return numeroOperacion;
    }

    void setLoanNumber(String numeroOperacion) {
        this.numeroOperacion = numeroOperacion;
    }

    BigDecimal getApprovedAmount() {
        return montoAprobado;
    }

    void setApprovedAmount(BigDecimal montoAprobado) {
        this.montoAprobado = montoAprobado;
    }

    BigDecimal getEffectiveAnnualRate() {
        return teaAsignada;
    }

    void setEffectiveAnnualRate(BigDecimal teaAsignada) {
        this.teaAsignada = teaAsignada;
    }

    Integer getTermMonths() {
        return plazoMeses;
    }

    void setTermMonths(Integer plazoMeses) {
        this.plazoMeses = plazoMeses;
    }

    BigDecimal getMonthlyInsurance() {
        return seguroMensual;
    }

    void setMonthlyInsurance(BigDecimal seguroMensual) {
        this.seguroMensual = seguroMensual;
    }

    BigDecimal getSolcaContribution() {
        return contribucionSolca;
    }

    void setSolcaContribution(BigDecimal contribucionSolca) {
        this.contribucionSolca = contribucionSolca;
    }

    BigDecimal getNetAmount() {
        return montoNeto;
    }

    void setNetAmount(BigDecimal montoNeto) {
        this.montoNeto = montoNeto;
    }

    String getDestinationAccount() {
        return cuentaDestino;
    }

    void setDestinationAccount(String cuentaDestino) {
        this.cuentaDestino = cuentaDestino;
    }

    LocalDate getDisbursementDate() {
        return fechaDesembolso;
    }

    void setDisbursementDate(LocalDate fechaDesembolso) {
        this.fechaDesembolso = fechaDesembolso;
    }

    EstadoInstruccionDesembolso getDisbursementInstructionStatus() {
        return disbursementInstructionStatus;
    }

    void setDisbursementInstructionStatus(EstadoInstruccionDesembolso disbursementInstructionStatus) {
        this.disbursementInstructionStatus = disbursementInstructionStatus;
    }

    EstadoCartera getPortfolioStatus() {
        return portfolioStatus;
    }

    void setPortfolioStatus(EstadoCartera portfolioStatus) {
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
