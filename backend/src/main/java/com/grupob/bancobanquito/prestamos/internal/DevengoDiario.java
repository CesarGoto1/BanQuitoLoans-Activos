package com.grupob.bancobanquito.prestamos.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "devengosDiarios")
class DevengoDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDevengo")
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idOperacion")
    private Prestamo prestamo;

    @NotNull
    @Column(name = "fechaDevengo")
    private LocalDate fechaDevengo;

    @NotNull
    @Digits(integer = 6, fraction = 4)
    @Column(name = "interesDevengado")
    private BigDecimal interesDevengado;

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

    DevengoDiario() {
    }

    DevengoDiario(Prestamo prestamo, LocalDate fechaDevengo, BigDecimal interesDevengado, String usuarioCreacion) {
        this.prestamo = prestamo;
        this.fechaDevengo = fechaDevengo;
        this.interesDevengado = interesDevengado;
        this.usuarioCreacion = usuarioCreacion;
        this.fechaCreacion = LocalDateTime.now();
    }

    Integer getId() {
        return id;
    }

    Prestamo getPrestamo() {
        return prestamo;
    }

    LocalDate getFechaDevengo() {
        return fechaDevengo;
    }

    BigDecimal getInteresDevengado() {
        return interesDevengado;
    }

    String getUsuarioCreacion() {
        return usuarioCreacion;
    }

    LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    String getUsuarioModificacion() {
        return usuarioModificacion;
    }

    LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }
}
