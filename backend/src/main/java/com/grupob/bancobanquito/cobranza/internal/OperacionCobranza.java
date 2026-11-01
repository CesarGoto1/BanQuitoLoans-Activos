package com.grupob.bancobanquito.cobranza.internal;

import jakarta.persistence.Column;
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
public class OperacionCobranza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idOperacion")
    private Integer idOperacion;

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
    @Size(max = 10)
    @Column(name = "sistemaAmortizacion")
    private String sistemaAmortizacion;

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
    @Size(max = 10)
    @Column(name = "estadoInstruccionDesembolso")
    private String estadoInstruccionDesembolso;

    @NotNull
    @Size(max = 10)
    @Column(name = "estadoCartera")
    private String estadoCartera;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "saldoCapital")
    private BigDecimal saldoCapital;

    @NotNull
    @Column(name = "diasMora")
    private Integer diasMora;

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

    public OperacionCobranza() {
    }

    public Integer getIdOperacion() {
        return idOperacion;
    }

    public void setIdOperacion(Integer idOperacion) {
        this.idOperacion = idOperacion;
    }

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

    public String getCedulaCliente() {
        return cedulaCliente;
    }

    public void setCedulaCliente(String cedulaCliente) {
        this.cedulaCliente = cedulaCliente;
    }

    public String getSistemaAmortizacion() {
        return sistemaAmortizacion;
    }

    public void setSistemaAmortizacion(String sistemaAmortizacion) {
        this.sistemaAmortizacion = sistemaAmortizacion;
    }

    public String getNumeroOperacion() {
        return numeroOperacion;
    }

    public void setNumeroOperacion(String numeroOperacion) {
        this.numeroOperacion = numeroOperacion;
    }

    public BigDecimal getMontoAprobado() {
        return montoAprobado;
    }

    public void setMontoAprobado(BigDecimal montoAprobado) {
        this.montoAprobado = montoAprobado;
    }

    public BigDecimal getTeaAsignada() {
        return teaAsignada;
    }

    public void setTeaAsignada(BigDecimal teaAsignada) {
        this.teaAsignada = teaAsignada;
    }

    public Integer getPlazoMeses() {
        return plazoMeses;
    }

    public void setPlazoMeses(Integer plazoMeses) {
        this.plazoMeses = plazoMeses;
    }

    public BigDecimal getSeguroMensual() {
        return seguroMensual;
    }

    public void setSeguroMensual(BigDecimal seguroMensual) {
        this.seguroMensual = seguroMensual;
    }

    public BigDecimal getContribucionSolca() {
        return contribucionSolca;
    }

    public void setContribucionSolca(BigDecimal contribucionSolca) {
        this.contribucionSolca = contribucionSolca;
    }

    public BigDecimal getMontoNeto() {
        return montoNeto;
    }

    public void setMontoNeto(BigDecimal montoNeto) {
        this.montoNeto = montoNeto;
    }

    public String getCuentaDestino() {
        return cuentaDestino;
    }

    public void setCuentaDestino(String cuentaDestino) {
        this.cuentaDestino = cuentaDestino;
    }

    public LocalDate getFechaDesembolso() {
        return fechaDesembolso;
    }

    public void setFechaDesembolso(LocalDate fechaDesembolso) {
        this.fechaDesembolso = fechaDesembolso;
    }

    public String getEstadoInstruccionDesembolso() {
        return estadoInstruccionDesembolso;
    }

    public void setEstadoInstruccionDesembolso(String estadoInstruccionDesembolso) {
        this.estadoInstruccionDesembolso = estadoInstruccionDesembolso;
    }

    public String getEstadoCartera() {
        return estadoCartera;
    }

    public void setEstadoCartera(String estadoCartera) {
        this.estadoCartera = estadoCartera;
    }

    public BigDecimal getSaldoCapital() {
        return saldoCapital;
    }

    public void setSaldoCapital(BigDecimal saldoCapital) {
        this.saldoCapital = saldoCapital;
    }

    public Integer getDiasMora() {
        return diasMora;
    }

    public void setDiasMora(Integer diasMora) {
        this.diasMora = diasMora;
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
