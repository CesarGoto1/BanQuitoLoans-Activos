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
@Table(name = "devengosDiarios")
class DevengoDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDevengo")
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idOperacion")
    private Prestamo loan;

    @NotNull
    @Column(name = "fechaDevengo")
    private LocalDate accrualDate;

    @NotNull
    @Digits(integer = 6, fraction = 4)
    @Column(name = "interesDevengado")
    private BigDecimal accruedInterest;

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

    DevengoDiario() {
    }

    DevengoDiario(Prestamo loan, LocalDate accrualDate, BigDecimal accruedInterest, String createdBy) {
        this.loan = loan;
        this.accrualDate = accrualDate;
        this.accruedInterest = accruedInterest;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    Integer getId() {
        return id;
    }

    Prestamo getLoan() {
        return loan;
    }

    LocalDate getAccrualDate() {
        return accrualDate;
    }

    BigDecimal getAccruedInterest() {
        return accruedInterest;
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
