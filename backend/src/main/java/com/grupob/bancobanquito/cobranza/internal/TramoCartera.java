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
@Table(name = "tramosCartera")
public class TramoCartera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTramo")
    private Integer idTramo;

    @NotNull
    @Column(name = "numeroTramo")
    private Integer numeroTramo;

    @NotNull
    @Column(name = "diasMoraDesde")
    private Integer diasMoraDesde;

    @Column(name = "diasMoraHasta")
    private Integer diasMoraHasta;

    @NotNull
    @Size(max = 10)
    @Column(name = "estadoCartera")
    private String estadoCartera;

    @NotNull
    @Column(name = "devengaIngresos")
    private Boolean devengaIngresos;

    @NotNull
    @Size(max = 20)
    @Column(name = "tratamientoMora")
    private String tratamientoMora;

    @NotNull
    @Digits(integer = 1, fraction = 4)
    @Column(name = "porcentajeProvision")
    private BigDecimal porcentajeProvision;

    @NotNull
    @Column(name = "fechaVigenciaDesde")
    private LocalDate fechaVigenciaDesde;

    @Column(name = "fechaVigenciaHasta")
    private LocalDate fechaVigenciaHasta;

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

    public TramoCartera() {
    }

    public Integer getIdTramo() {
        return idTramo;
    }

    public void setIdTramo(Integer idTramo) {
        this.idTramo = idTramo;
    }

    public Integer getNumeroTramo() {
        return numeroTramo;
    }

    public void setNumeroTramo(Integer numeroTramo) {
        this.numeroTramo = numeroTramo;
    }

    public Integer getDiasMoraDesde() {
        return diasMoraDesde;
    }

    public void setDiasMoraDesde(Integer diasMoraDesde) {
        this.diasMoraDesde = diasMoraDesde;
    }

    public Integer getDiasMoraHasta() {
        return diasMoraHasta;
    }

    public void setDiasMoraHasta(Integer diasMoraHasta) {
        this.diasMoraHasta = diasMoraHasta;
    }

    public String getEstadoCartera() {
        return estadoCartera;
    }

    public void setEstadoCartera(String estadoCartera) {
        this.estadoCartera = estadoCartera;
    }

    public Boolean getDevengaIngresos() {
        return devengaIngresos;
    }

    public void setDevengaIngresos(Boolean devengaIngresos) {
        this.devengaIngresos = devengaIngresos;
    }

    public String getTratamientoMora() {
        return tratamientoMora;
    }

    public void setTratamientoMora(String tratamientoMora) {
        this.tratamientoMora = tratamientoMora;
    }

    public BigDecimal getPorcentajeProvision() {
        return porcentajeProvision;
    }

    public void setPorcentajeProvision(BigDecimal porcentajeProvision) {
        this.porcentajeProvision = porcentajeProvision;
    }

    public LocalDate getFechaVigenciaDesde() {
        return fechaVigenciaDesde;
    }

    public void setFechaVigenciaDesde(LocalDate fechaVigenciaDesde) {
        this.fechaVigenciaDesde = fechaVigenciaDesde;
    }

    public LocalDate getFechaVigenciaHasta() {
        return fechaVigenciaHasta;
    }

    public void setFechaVigenciaHasta(LocalDate fechaVigenciaHasta) {
        this.fechaVigenciaHasta = fechaVigenciaHasta;
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
