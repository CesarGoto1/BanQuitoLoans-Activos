package com.grupob.bancobanquito.loans.internal.entities;
import com.grupob.bancobanquito.loans.internal.entities.*;
import com.grupob.bancobanquito.loans.internal.repositories.*;
import com.grupob.bancobanquito.loans.internal.services.*;
import com.grupob.bancobanquito.loans.internal.controllers.*;
import com.grupob.bancobanquito.loans.internal.exceptions.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "cuotas")
@EntityListeners(AuditingEntityListener.class)
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCuota;

    @NotNull
    private Integer idOperacion;

    @NotNull
    private Integer numeroCuota;

    @NotNull
    private LocalDate fechaVencimiento;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal valorCapital;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal valorInteres;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal valorSeguro;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(insertable = false, updatable = false)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal totalCuota;

    @Digits(integer = 8, fraction = 2)
    private BigDecimal ajusteDevengoInteres;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal saldoCapitalPosterior;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal seguroPagado;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal moraPagada;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal interesPagado;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal capitalPagado;

    @NotNull
    @Digits(integer = 6, fraction = 4)
    private BigDecimal moraAcumulada;

    @NotNull
    @Enumerated(EnumType.STRING)
    private InstallmentStatus estadoCuota;

    private LocalDate fechaPago;

    @CreatedBy
    @NotNull
    @Size(max = 50)
    private String usuarioCreacion;

    @CreatedDate
    @NotNull
    private LocalDateTime fechaCreacion;

    @LastModifiedBy
    @Size(max = 50)
    private String usuarioModificacion;

    @LastModifiedDate
    private LocalDateTime fechaModificacion;

    protected Installment() {
    }

    public Integer getIdInstallment() {
        return idCuota;
    }

    public Integer getIdLoanOperation() {
        return idOperacion;
    }

    public Integer getNumeroInstallment() {
        return numeroCuota;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public BigDecimal getValorCapital() {
        return valorCapital;
    }

    public BigDecimal getValorInteres() {
        return valorInteres;
    }

    public BigDecimal getValorSeguro() {
        return valorSeguro;
    }

    public BigDecimal getTotalInstallment() {
        return totalCuota;
    }

    public BigDecimal getAjusteDevengoInteres() {
        return ajusteDevengoInteres;
    }

    public BigDecimal getSaldoCapitalPosterior() {
        return saldoCapitalPosterior;
    }

    public BigDecimal getSeguroPagado() {
        return seguroPagado;
    }

    public BigDecimal getMoraPagada() {
        return moraPagada;
    }

    public BigDecimal getInteresPagado() {
        return interesPagado;
    }

    public BigDecimal getCapitalPagado() {
        return capitalPagado;
    }

    public BigDecimal getMoraAcumulada() {
        return moraAcumulada;
    }

    public InstallmentStatus getInstallmentStatus() {
        return estadoCuota;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public String getUsuarioCreacion() {
        return usuarioCreacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public String getUsuarioModificacion() {
        return usuarioModificacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }
}
















