package com.grupob.bancobanquito.parametros.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "cierresDia")
class DiaNegocio {

    @Id
    @Column(name = "idCierre")
    private Integer id;

    @Column(name = "fechaNegocio")
    private LocalDate fechaNegocio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadoCierre")
    private EstadoCierre status;

    DiaNegocio() {
    }

    DiaNegocio(Integer id) {
        this.id = id;
    }

    Integer getId() {
        return id;
    }

    LocalDate getFechaNegocio() {
        return fechaNegocio;
    }

    EstadoCierre getStatus() {
        return status;
    }
}
