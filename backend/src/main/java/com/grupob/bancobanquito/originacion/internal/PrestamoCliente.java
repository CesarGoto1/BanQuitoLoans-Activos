package com.grupob.bancobanquito.originacion.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "operaciones")
class PrestamoCliente {

    @Id
    @Column(name = "idOperacion")
    private Integer id;

    @Column(name = "idSolicitud")
    private Integer idSolicitud;

    @Column(name = "idCliente")
    private Integer idCliente;

    @Column(name = "diasMora")
    private Integer daysPastDue;

    PrestamoCliente() {
    }

    PrestamoCliente(Integer id) {
        this.id = id;
    }

    Integer getId() {
        return id;
    }

    Integer getCustomerId() {
        return idCliente;
    }

    Integer getDaysPastDue() {
        return daysPastDue;
    }
}
