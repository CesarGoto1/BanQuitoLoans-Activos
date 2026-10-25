package com.grupob.bancobanquito.parameters.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "parametros")
class Parameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idParametro")
    private Integer id;

    @NotNull
    @Size(max = 10)
    @Column(name = "codigoParametro")
    private String code;

    @NotNull
    @Size(max = 100)
    @Column(name = "nombreParametro")
    private String name;

    @NotNull
    @Digits(integer = 6, fraction = 4)
    @Column(name = "valorParametro")
    private BigDecimal value;

    @NotNull
    @Column(name = "fechaVigenciaDesde")
    private LocalDate validFrom;

    @Column(name = "fechaVigenciaHasta")
    private LocalDate validTo;

    @NotNull
    @Size(max = 50)
    @Column(name = "usuarioCreacion", updatable = false)
    private String createdBy;

    @NotNull
    @Column(name = "fechaCreacion", updatable = false)
    private LocalDateTime createdAt;

    @Size(max = 50)
    @Column(name = "usuarioModificacion")
    private String modifiedBy;

    @Column(name = "fechaModificacion")
    private LocalDateTime modifiedAt;

    Parameter() {
    }

    Parameter(Integer id) {
        this.id = id;
    }

    boolean isValidOn(LocalDate date) {
        return !date.isBefore(validFrom) && (validTo == null || date.isBefore(validTo));
    }

    Integer getId() {
        return id;
    }

    String getCode() {
        return code;
    }

    void setCode(String code) {
        this.code = code;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    BigDecimal getValue() {
        return value;
    }

    void setValue(BigDecimal value) {
        this.value = value;
    }

    LocalDate getValidFrom() {
        return validFrom;
    }

    void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    LocalDate getValidTo() {
        return validTo;
    }

    void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    String getCreatedBy() {
        return createdBy;
    }

    void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    LocalDateTime getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    String getModifiedBy() {
        return modifiedBy;
    }

    void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
}
