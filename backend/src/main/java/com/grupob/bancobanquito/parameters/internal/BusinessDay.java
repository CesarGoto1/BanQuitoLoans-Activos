package com.grupob.bancobanquito.parameters.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "cierresDia")
class BusinessDay {

    @Id
    @Column(name = "idCierre")
    private Integer id;

    @Column(name = "fechaNegocio")
    private LocalDate businessDate;

    @Convert(converter = ClosingStatusConverter.class)
    @Column(name = "estadoCierre")
    private ClosingStatus status;

    BusinessDay() {
    }

    BusinessDay(Integer id) {
        this.id = id;
    }

    Integer getId() {
        return id;
    }

    LocalDate getBusinessDate() {
        return businessDate;
    }

    ClosingStatus getStatus() {
        return status;
    }
}
