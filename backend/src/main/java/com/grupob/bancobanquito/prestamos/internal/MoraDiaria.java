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
@Table(name = "morasDiarias")
class MoraDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idMora")
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCuota")
    private Cuota cuota;

    @NotNull
    @Column(name = "fechaMora")
    private LocalDate fechaMora;

    @NotNull
    @Digits(integer = 6, fraction = 4)
    @Column(name = "interesMora")
    private BigDecimal interesMora;

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

    MoraDiaria() {
    }

    MoraDiaria(Cuota cuota, LocalDate fechaMora, BigDecimal interesMora, String usuarioCreacion) {
        this.cuota = cuota;
        this.fechaMora = fechaMora;
        this.interesMora = interesMora;
        this.usuarioCreacion = usuarioCreacion;
        this.fechaCreacion = LocalDateTime.now();
    }

    Integer getId() {
        return id;
    }

    Cuota getCuota() {
        return cuota;
    }

    LocalDate getFechaMora() {
        return fechaMora;
    }

    BigDecimal getInteresMora() {
        return interesMora;
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
