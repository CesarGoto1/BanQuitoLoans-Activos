package com.grupob.bancobanquito.cobranza.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "devengosDiarios")
public class DevengoCobranza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDevengo")
    private Integer idDevengo;

    @NotNull
    @Column(name = "idOperacion")
    private Integer idOperacion;

    @NotNull
    @Column(name = "fechaDevengo")
    private LocalDate fechaDevengo;

    @NotNull
    @Digits(integer = 8, fraction = 4)
    @Column(name = "interesDevengado")
    private BigDecimal interesDevengado;

    @NotNull
    @Column(name = "usuarioCreacion", updatable = false)
    private String usuarioCreacion;

    @NotNull
    @Column(name = "fechaCreacion", updatable = false)
    private LocalDateTime fechaCreacion;

    public DevengoCobranza() {
    }

    public Integer getIdDevengo() {
        return idDevengo;
    }

    public void setIdDevengo(Integer idDevengo) {
        this.idDevengo = idDevengo;
    }

    public Integer getIdOperacion() {
        return idOperacion;
    }

    public void setIdOperacion(Integer idOperacion) {
        this.idOperacion = idOperacion;
    }

    public LocalDate getFechaDevengo() {
        return fechaDevengo;
    }

    public void setFechaDevengo(LocalDate fechaDevengo) {
        this.fechaDevengo = fechaDevengo;
    }

    public BigDecimal getInteresDevengado() {
        return interesDevengado;
    }

    public void setInteresDevengado(BigDecimal interesDevengado) {
        this.interesDevengado = interesDevengado;
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
}
