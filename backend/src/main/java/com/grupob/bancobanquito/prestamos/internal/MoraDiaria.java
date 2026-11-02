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
    private Cuota installment;

    @NotNull
    @Column(name = "fechaMora")
    private LocalDate defaultDate;

    @NotNull
    @Digits(integer = 6, fraction = 4)
    @Column(name = "interesMora")
    private BigDecimal defaultInterest;

    @NotNull
    @Size(max = 50)
    @Column(name = "usuarioCreacion", updatable = false)
    private String createdBy;

    @NotNull
    @Column(name = "fechaCreacion", updatable = false)
    private LocalDateTime createdAt;

    @Size(max = 50)
    @Column(name = "usuarioModificacion")
    private String lastModifiedBy;

    @Column(name = "fechaModificacion")
    private LocalDateTime lastModifiedAt;

    MoraDiaria() {
    }

    MoraDiaria(Cuota installment, LocalDate defaultDate, BigDecimal defaultInterest, String createdBy) {
        this.installment = installment;
        this.defaultDate = defaultDate;
        this.defaultInterest = defaultInterest;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    Integer getId() {
        return id;
    }

    Cuota getInstallment() {
        return installment;
    }

    LocalDate getDefaultDate() {
        return defaultDate;
    }

    BigDecimal getDefaultInterest() {
        return defaultInterest;
    }

    String getCreatedBy() {
        return createdBy;
    }

    LocalDateTime getCreatedAt() {
        return createdAt;
    }

    String getLastModifiedBy() {
        return lastModifiedBy;
    }

    LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }
}
