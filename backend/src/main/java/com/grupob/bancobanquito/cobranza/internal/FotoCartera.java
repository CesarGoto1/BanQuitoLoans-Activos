package com.grupob.bancobanquito.cobranza.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fotosCartera")
@IdClass(FotoCarteraId.class)
public class FotoCartera {

    @Id
    @Column(name = "idOperacion")
    private Integer idOperacion;

    @Id
    @Column(name = "fechaFoto")
    private LocalDate fechaFoto;

    @NotNull
    @Column(name = "idTramo")
    private Integer idTramo;

    @NotNull
    @Size(max = 10)
    @Column(name = "estadoCartera")
    private String estadoCartera;

    @NotNull
    @Column(name = "diasMora")
    private Integer diasMora;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "saldoCapital")
    private BigDecimal saldoCapital;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "valorProvision")
    private BigDecimal valorProvision;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "interesDevengadoAcumulado")
    private BigDecimal interesDevengadoAcumulado;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "interesMoraAcumulado")
    private BigDecimal interesMoraAcumulado;

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

    public FotoCartera() {
    }

    public Integer getIdOperacion() {
        return idOperacion;
    }

    public void setIdOperacion(Integer idOperacion) {
        this.idOperacion = idOperacion;
    }

    public LocalDate getFechaFoto() {
        return fechaFoto;
    }

    public void setFechaFoto(LocalDate fechaFoto) {
        this.fechaFoto = fechaFoto;
    }

    public Integer getIdTramo() {
        return idTramo;
    }

    public void setIdTramo(Integer idTramo) {
        this.idTramo = idTramo;
    }

    public String getEstadoCartera() {
        return estadoCartera;
    }

    public void setEstadoCartera(String estadoCartera) {
        this.estadoCartera = estadoCartera;
    }

    public Integer getDiasMora() {
        return diasMora;
    }

    public void setDiasMora(Integer diasMora) {
        this.diasMora = diasMora;
    }

    public BigDecimal getSaldoCapital() {
        return saldoCapital;
    }

    public void setSaldoCapital(BigDecimal saldoCapital) {
        this.saldoCapital = saldoCapital;
    }

    public BigDecimal getValorProvision() {
        return valorProvision;
    }

    public void setValorProvision(BigDecimal valorProvision) {
        this.valorProvision = valorProvision;
    }

    public BigDecimal getInteresDevengadoAcumulado() {
        return interesDevengadoAcumulado;
    }

    public void setInteresDevengadoAcumulado(BigDecimal interesDevengadoAcumulado) {
        this.interesDevengadoAcumulado = interesDevengadoAcumulado;
    }

    public BigDecimal getInteresMoraAcumulado() {
        return interesMoraAcumulado;
    }

    public void setInteresMoraAcumulado(BigDecimal interesMoraAcumulado) {
        this.interesMoraAcumulado = interesMoraAcumulado;
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
