package com.grupob.bancobanquito.otorgamiento.internal;

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
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idSolicitud")
    private Integer idSolicitud;

    @NotNull
    @Column(name = "idCliente")
    private Integer idCliente;

    @NotNull
    @Size(max = 7)
    @Column(name = "sistemaAmortizacion")
    private String sistemaAmortizacion;

    @Column(name = "idAnalista")
    private Integer idAnalista;

    @NotNull
    @Positive
    @Digits(integer = 8, fraction = 2)
    @Column(name = "montoSolicitado")
    private BigDecimal montoSolicitado;

    @NotNull
    @Positive
    @Column(name = "plazoMeses")
    private Integer plazoMeses;

    @NotNull
    @Size(max = 20)
    @Column(name = "destinoCredito")
    private String destinoCredito;

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
    @Positive
    @Digits(integer = 8, fraction = 2)
    @Column(name = "cuotaEvaluacionReferencial")
    private BigDecimal cuotaEvaluacionReferencial;

    @Digits(integer = 1, fraction = 4)
    @Column(name = "teaAsignada")
    private BigDecimal teaAsignada;

    @Digits(integer = 8, fraction = 2)
    @Column(name = "cuotaEvaluacionDefinitiva")
    private BigDecimal cuotaEvaluacionDefinitiva;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estadoSolicitud")
    private EstadoSolicitud estadoSolicitud;

    @Column(name = "fechaResolucion")
    private LocalDate fechaResolucion;

    @Size(max = 200)
    @Column(name = "motivoRechazo")
    private String motivoRechazo;

    @NotNull
    @Size(max = 50)
    @Column(name = "usuarioCreacion", updatable = false)
    private String usuarioCreacion;

    @NotNull
    @Column(name = "fechaCreacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Size(max = 50)
    @Column(name = "usuarioModificacion")
    private String usuarioModificacion;

    @Column(name = "fechaModificacion")
    private LocalDateTime fechaModificacion;

    public Solicitud() {
    }

    public Solicitud(Integer idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    // Getters y Setters explícitos (sin Lombok)

    public Integer getIdSolicitud() {
        return idSolicitud;
    }

    public void setIdSolicitud(Integer idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public String getSistemaAmortizacion() {
        return sistemaAmortizacion;
    }

    public void setSistemaAmortizacion(String sistemaAmortizacion) {
        this.sistemaAmortizacion = sistemaAmortizacion;
    }

    public Integer getIdAnalista() {
        return idAnalista;
    }

    public void setIdAnalista(Integer idAnalista) {
        this.idAnalista = idAnalista;
    }

    public BigDecimal getMontoSolicitado() {
        return montoSolicitado;
    }

    public void setMontoSolicitado(BigDecimal montoSolicitado) {
        this.montoSolicitado = montoSolicitado;
    }

    public Integer getPlazoMeses() {
        return plazoMeses;
    }

    public void setPlazoMeses(Integer plazoMeses) {
        this.plazoMeses = plazoMeses;
    }

    public String getDestinoCredito() {
        return destinoCredito;
    }

    public void setDestinoCredito(String destinoCredito) {
        this.destinoCredito = destinoCredito;
    }

    public BigDecimal getIngresosMensuales() {
        return ingresosMensuales;
    }

    public void setIngresosMensuales(BigDecimal ingresosMensuales) {
        this.ingresosMensuales = ingresosMensuales;
    }

    public BigDecimal getGastosMensuales() {
        return gastosMensuales;
    }

    public void setGastosMensuales(BigDecimal gastosMensuales) {
        this.gastosMensuales = gastosMensuales;
    }

    public LocalDate getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDate fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public LocalDateTime getFechaHoraRegistro() {
        return fechaHoraRegistro;
    }

    public void setFechaHoraRegistro(LocalDateTime fechaHoraRegistro) {
        this.fechaHoraRegistro = fechaHoraRegistro;
    }

    public BigDecimal getCuotaEvaluacionReferencial() {
        return cuotaEvaluacionReferencial;
    }

    public void setCuotaEvaluacionReferencial(BigDecimal cuotaEvaluacionReferencial) {
        this.cuotaEvaluacionReferencial = cuotaEvaluacionReferencial;
    }

    public BigDecimal getTeaAsignada() {
        return teaAsignada;
    }

    public void setTeaAsignada(BigDecimal teaAsignada) {
        this.teaAsignada = teaAsignada;
    }

    public BigDecimal getCuotaEvaluacionDefinitiva() {
        return cuotaEvaluacionDefinitiva;
    }

    public void setCuotaEvaluacionDefinitiva(BigDecimal cuotaEvaluacionDefinitiva) {
        this.cuotaEvaluacionDefinitiva = cuotaEvaluacionDefinitiva;
    }

    public EstadoSolicitud getEstadoSolicitud() {
        return estadoSolicitud;
    }

    public void setEstadoSolicitud(EstadoSolicitud estadoSolicitud) {
        this.estadoSolicitud = estadoSolicitud;
    }

    public LocalDate getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(LocalDate fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public String getUsuarioCreacion() {
        return usuarioCreacion;
    }

    public void setUsuarioCreacion(String usuarioCreacion) {
        this.usuarioCreacion = usuarioCreacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getUsuarioModificacion() {
        return usuarioModificacion;
    }

    public void setUsuarioModificacion(String usuarioModificacion) {
        this.usuarioModificacion = usuarioModificacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(LocalDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }
}
