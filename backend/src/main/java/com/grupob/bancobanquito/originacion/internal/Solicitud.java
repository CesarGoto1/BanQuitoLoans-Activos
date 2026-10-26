package com.grupob.bancobanquito.originacion.internal;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
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
@Table(name = "solicitudes")
class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idSolicitud")
    private Integer id;

    @NotNull
    @Column(name = "idCliente")
    private Integer idCliente;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "sistemaAmortizacion")
    private MetodoAmortizacion metodoAmortizacion;

    @Column(name = "idAnalista")
    private Integer idAnalista;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "montoSolicitado")
    private BigDecimal montoSolicitado;

    @NotNull
    @Column(name = "plazoMeses")
    private Integer plazoMeses;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "destinoCredito")
    private DestinoCredito destinoCredito;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "ingresosMensuales")
    private BigDecimal ingresosMensuales;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "gastosMensuales")
    private BigDecimal gastosMensuales;

    @NotNull
    @Column(name = "fechaSolicitud")
    private LocalDate fechaSolicitud;

    @NotNull
    @Column(name = "fechaHoraRegistro")
    private LocalDateTime fechaHoraRegistro;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "cuotaEvaluacionReferencial")
    private BigDecimal cuotaEvaluacionReferencial;

    @Digits(integer = 1, fraction = 4)
    @Column(name = "teaAsignada")
    private BigDecimal tasaAsignada;

    @Digits(integer = 8, fraction = 2)
    @Column(name = "cuotaEvaluacionDefinitiva")
    private BigDecimal finalInstallment;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estadoSolicitud")
    private EstadoSolicitud status;

    @Column(name = "fechaResolucion")
    private LocalDate fechaResolucion;

    @Size(max = 200)
    @Column(name = "motivoRechazo")
    private String motivoRechazo;

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

    Solicitud() {
    }

    Solicitud(Integer id) {
        this.id = id;
    }

    Integer getId() {
        return id;
    }

    void setId(Integer id) {
        this.id = id;
    }

    Integer getCustomerId() {
        return idCliente;
    }

    void setCustomerId(Integer idCliente) {
        this.idCliente = idCliente;
    }

    MetodoAmortizacion getAmortizationMethod() {
        return metodoAmortizacion;
    }

    void setAmortizationMethod(MetodoAmortizacion metodoAmortizacion) {
        this.metodoAmortizacion = metodoAmortizacion;
    }

    Integer getAnalystId() {
        return idAnalista;
    }

    void setAnalystId(Integer idAnalista) {
        this.idAnalista = idAnalista;
    }

    BigDecimal getRequestedAmount() {
        return montoSolicitado;
    }

    void setRequestedAmount(BigDecimal montoSolicitado) {
        this.montoSolicitado = montoSolicitado;
    }

    Integer getTermMonths() {
        return plazoMeses;
    }

    void setTermMonths(Integer plazoMeses) {
        this.plazoMeses = plazoMeses;
    }

    DestinoCredito getPurpose() {
        return destinoCredito;
    }

    void setPurpose(DestinoCredito destinoCredito) {
        this.destinoCredito = destinoCredito;
    }

    BigDecimal getMonthlyIncome() {
        return ingresosMensuales;
    }

    void setMonthlyIncome(BigDecimal ingresosMensuales) {
        this.ingresosMensuales = ingresosMensuales;
    }

    BigDecimal getMonthlyExpenses() {
        return gastosMensuales;
    }

    void setMonthlyExpenses(BigDecimal gastosMensuales) {
        this.gastosMensuales = gastosMensuales;
    }

    LocalDate getApplicationDate() {
        return fechaSolicitud;
    }

    void setApplicationDate(LocalDate fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    LocalDateTime getRegisteredAt() {
        return fechaHoraRegistro;
    }

    void setRegisteredAt(LocalDateTime fechaHoraRegistro) {
        this.fechaHoraRegistro = fechaHoraRegistro;
    }

    BigDecimal getReferenceInstallment() {
        return cuotaEvaluacionReferencial;
    }

    void setReferenceInstallment(BigDecimal cuotaEvaluacionReferencial) {
        this.cuotaEvaluacionReferencial = cuotaEvaluacionReferencial;
    }

    BigDecimal getAssignedRate() {
        return tasaAsignada;
    }

    void setAssignedRate(BigDecimal tasaAsignada) {
        this.tasaAsignada = tasaAsignada;
    }

    BigDecimal getFinalInstallment() {
        return finalInstallment;
    }

    void setFinalInstallment(BigDecimal finalInstallment) {
        this.finalInstallment = finalInstallment;
    }

    EstadoSolicitud getStatus() {
        return status;
    }

    void setStatus(EstadoSolicitud status) {
        this.status = status;
    }

    LocalDate getResolutionDate() {
        return fechaResolucion;
    }

    void setResolutionDate(LocalDate fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    String getRejectionReason() {
        return motivoRechazo;
    }

    void setRejectionReason(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
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
