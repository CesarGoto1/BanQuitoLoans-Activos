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
@Table(name = "cuotas")
public class CuotaCobranza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCuota")
    private Integer idCuota;

    @NotNull
    @Column(name = "idOperacion")
    private Integer idOperacion;

    @NotNull
    @Column(name = "numeroCuota")
    private Integer numeroCuota;

    @NotNull
    @Column(name = "fechaVencimiento")
    private LocalDate fechaVencimiento;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "valorCapital")
    private BigDecimal valorCapital;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "valorInteres")
    private BigDecimal valorInteres;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "valorSeguro")
    private BigDecimal valorSeguro;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "saldoCapitalPosterior")
    private BigDecimal saldoCapitalPosterior;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "seguroPagado")
    private BigDecimal seguroPagado;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "interesPagado")
    private BigDecimal interesPagado;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "capitalPagado")
    private BigDecimal capitalPagado;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "moraPagada")
    private BigDecimal moraPagada;

    @NotNull
    @Digits(integer = 8, fraction = 4)
    @Column(name = "moraAcumulada")
    private BigDecimal moraAcumulada;

    @NotNull
    @Size(max = 15)
    @Column(name = "estadoCuota")
    private String estadoCuota;

    @Column(name = "fechaPago")
    private LocalDate fechaPago;

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

    public CuotaCobranza() {
    }

    public Integer getIdCuota() {
        return idCuota;
    }

    public void setIdCuota(Integer idCuota) {
        this.idCuota = idCuota;
    }

    public Integer getIdOperacion() {
        return idOperacion;
    }

    public void setIdOperacion(Integer idOperacion) {
        this.idOperacion = idOperacion;
    }

    public Integer getNumeroCuota() {
        return numeroCuota;
    }

    public void setNumeroCuota(Integer numeroCuota) {
        this.numeroCuota = numeroCuota;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public BigDecimal getValorCapital() {
        return valorCapital;
    }

    public void setValorCapital(BigDecimal valorCapital) {
        this.valorCapital = valorCapital;
    }

    public BigDecimal getValorInteres() {
        return valorInteres;
    }

    public void setValorInteres(BigDecimal valorInteres) {
        this.valorInteres = valorInteres;
    }

    public BigDecimal getValorSeguro() {
        return valorSeguro;
    }

    public void setValorSeguro(BigDecimal valorSeguro) {
        this.valorSeguro = valorSeguro;
    }

    public BigDecimal getSaldoCapitalPosterior() {
        return saldoCapitalPosterior;
    }

    public void setSaldoCapitalPosterior(BigDecimal saldoCapitalPosterior) {
        this.saldoCapitalPosterior = saldoCapitalPosterior;
    }

    public BigDecimal getSeguroPagado() {
        return seguroPagado;
    }

    public void setSeguroPagado(BigDecimal seguroPagado) {
        this.seguroPagado = seguroPagado;
    }

    public BigDecimal getInteresPagado() {
        return interesPagado;
    }

    public void setInteresPagado(BigDecimal interesPagado) {
        this.interesPagado = interesPagado;
    }

    public BigDecimal getCapitalPagado() {
        return capitalPagado;
    }

    public void setCapitalPagado(BigDecimal capitalPagado) {
        this.capitalPagado = capitalPagado;
    }

    public BigDecimal getMoraPagada() {
        return moraPagada;
    }

    public void setMoraPagada(BigDecimal moraPagada) {
        this.moraPagada = moraPagada;
    }

    public BigDecimal getMoraAcumulada() {
        return moraAcumulada;
    }

    public void setMoraAcumulada(BigDecimal moraAcumulada) {
        this.moraAcumulada = moraAcumulada;
    }

    public String getEstadoCuota() {
        return estadoCuota;
    }

    public void setEstadoCuota(String estadoCuota) {
        this.estadoCuota = estadoCuota;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
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
