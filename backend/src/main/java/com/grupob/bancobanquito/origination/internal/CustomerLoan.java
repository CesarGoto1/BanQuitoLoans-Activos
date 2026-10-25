package com.grupob.bancobanquito.origination.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "operaciones")
class CustomerLoan {

    @Id
    @Column(name = "idOperacion")
    private Integer id;

    @Column(name = "idSolicitud")
    private Integer applicationId;

    @Column(name = "idCliente")
    private Integer customerId;

    @Column(name = "diasMora")
    private Integer daysPastDue;

    CustomerLoan() {
    }

    CustomerLoan(Integer id) {
        this.id = id;
    }

    Integer getId() {
        return id;
    }

    Integer getCustomerId() {
        return customerId;
    }

    Integer getDaysPastDue() {
        return daysPastDue;
    }
}
