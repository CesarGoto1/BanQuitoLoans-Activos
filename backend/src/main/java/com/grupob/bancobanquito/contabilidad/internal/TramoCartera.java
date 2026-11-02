package com.grupob.bancobanquito.contabilidad.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tramosCartera")
class TramoCartera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTramo")
    private Integer id;

    @Column(name = "diasMoraDesde")
    private Integer daysFrom;

    @Column(name = "diasMoraHasta")
    private Integer daysTo;

    @Column(name = "porcentajeProvision")
    private BigDecimal provisionRate;

    @Column(name = "fechaVigenciaDesde")
    private LocalDate validFrom;

    @Column(name = "fechaVigenciaHasta")
    private LocalDate validTo;

    Integer getId() {
        return id;
    }

    BigDecimal getProvisionRate() {
        return provisionRate;
    }

    boolean appliesTo(Integer days, LocalDate date) {
        return !date.isBefore(validFrom)
                && (validTo == null || date.isBefore(validTo))
                && days >= daysFrom
                && (daysTo == null || days <= daysTo);
    }
}
