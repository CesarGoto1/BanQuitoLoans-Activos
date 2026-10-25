package com.grupob.bancobanquito.origination.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "analistas")
class Analyst {

    @Id
    @Column(name = "idAnalista")
    private Integer id;

    @NotNull
    @Size(max = 10)
    @Column(name = "cedulaAnalista", nullable = false, length = 10)
    private String identification;

    protected Analyst() {
    }

    Integer getId() {
        return id;
    }
}
