package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.prestamos.EstadoCartera;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "CambioEstadoPrestamo")
@Table(name = "cambiosEstadoOperacion")
class CambioEstadoOperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCambioEstado")
    private Integer id;

    @Column(name = "idOperacion")
    private Integer idOperacion;

    @Column(name = "fechaCambio")
    private LocalDate fechaCambio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadoAnterior")
    private EstadoCartera estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadoNuevo")
    private EstadoCartera estadoNuevo;

    @Column(name = "diasMora")
    private Integer diasMora;

    @Column(name = "usuarioCreacion", updatable = false)
    private String usuarioCreacion;

    @Column(name = "fechaCreacion", updatable = false)
    private LocalDateTime fechaCreacion;

    CambioEstadoOperacion() {
    }

    CambioEstadoOperacion(Integer idOperacion, LocalDate fechaCambio, EstadoCartera estadoAnterior,
                          EstadoCartera estadoNuevo, Integer diasMora, String usuarioCreacion) {
        this.idOperacion = idOperacion;
        this.fechaCambio = fechaCambio;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.diasMora = diasMora;
        this.usuarioCreacion = usuarioCreacion;
        this.fechaCreacion = LocalDateTime.now();
    }

    EstadoCartera getEstadoAnterior() {
        return estadoAnterior;
    }

    EstadoCartera getEstadoNuevo() {
        return estadoNuevo;
    }

    LocalDate getFechaCambio() {
        return fechaCambio;
    }
}
