package com.grupob.bancobanquito.cierre.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cierresDia")
class CierreDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCierre")
    private Integer id;

    @NotNull
    @Column(name = "fechaNegocio")
    private LocalDate fechaNegocio;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estadoCierre")
    private EstadoCierre estadoCierre;

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

    CierreDia() {
    }

    static CierreDia abrir(LocalDate fechaNegocio, String usuario) {
        CierreDia cierre = new CierreDia();
        cierre.fechaNegocio = fechaNegocio;
        cierre.estadoCierre = EstadoCierre.PENDIENTE;
        cierre.usuarioCreacion = usuario;
        cierre.fechaCreacion = LocalDateTime.now();
        return cierre;
    }

    void cambiarEstado(EstadoCierre estado, String usuario) {
        this.estadoCierre = estado;
        this.usuarioModificacion = usuario;
        this.fechaModificacion = LocalDateTime.now();
    }

    Integer getId() {
        return id;
    }

    LocalDate getFechaNegocio() {
        return fechaNegocio;
    }

    EstadoCierre getEstadoCierre() {
        return estadoCierre;
    }
}
