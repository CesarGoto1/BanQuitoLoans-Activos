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
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@Table(name = "operaciones")
@EntityListeners(AuditingEntityListener.class)
public class LoanOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idOperacion;

    @NotNull
    private Integer idSolicitud;

    @NotNull
    private Integer idCliente;

    @NotNull
    @Size(max = 10)
    private String cedulaCliente;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AmortizationSystem sistemaAmortizacion;

    @NotNull
    @Size(max = 20)
    private String numeroOperacion;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal montoAprobado;

    @NotNull
    @Digits(integer = 1, fraction = 4)
    private BigDecimal teaAsignada;

    @NotNull
    private Integer plazoMeses;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal seguroMensual;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal contribucionSolca;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal montoNeto;

    @NotNull
    @Size(max = 20)
    private String cuentaDestino;

    @NotNull
    private LocalDate fechaDesembolso;

    @NotNull
    @Enumerated(EnumType.STRING)
    private DisbursementInstructionStatus estadoInstruccionDesembolso;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PortfolioStatus estadoCartera;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal saldoCapital;

    @NotNull
    private Integer diasMora;

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

    protected LoanOperation() {
    }

    public Integer getIdLoanOperation() {
        return idOperacion;
    }

    public Integer getIdLoanRequest() {
        return idSolicitud;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public String getCedulaCliente() {
        return cedulaCliente;
    }

    public AmortizationSystem getAmortizationSystem() {
        return sistemaAmortizacion;
    }

    public String getNumeroLoanOperation() {
        return numeroOperacion;
    }

    public BigDecimal getMontoAprobado() {
        return montoAprobado;
    }

    public BigDecimal getTeaAsignada() {
        return teaAsignada;
    }

    public Integer getPlazoMeses() {
        return plazoMeses;
    }

    public BigDecimal getSeguroMensual() {
        return seguroMensual;
    }

    public BigDecimal getContribucionSolca() {
        return contribucionSolca;
    }

    public BigDecimal getMontoNeto() {
        return montoNeto;
    }

    public String getCuentaDestino() {
        return cuentaDestino;
    }

    public LocalDate getFechaDesembolso() {
        return fechaDesembolso;
    }

    public DisbursementInstructionStatus getDisbursementInstructionStatus() {
        return estadoInstruccionDesembolso;
    }

    public PortfolioStatus getPortfolioStatus() {
        return estadoCartera;
    }

    public BigDecimal getSaldoCapital() {
        return saldoCapital;
    }

    public Integer getDiasMora() {
        return diasMora;
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
















