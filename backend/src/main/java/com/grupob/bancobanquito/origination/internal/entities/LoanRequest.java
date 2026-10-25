package com.grupob.bancobanquito.origination.internal.entities;
import com.grupob.bancobanquito.origination.internal.entities.*;
import com.grupob.bancobanquito.origination.internal.repositories.*;
import com.grupob.bancobanquito.origination.internal.services.*;
import com.grupob.bancobanquito.origination.internal.controllers.*;
import com.grupob.bancobanquito.origination.internal.exceptions.*;
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
@Table(name = "solicitudes")
@EntityListeners(AuditingEntityListener.class)
public class LoanRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idSolicitud;

    @NotNull
    private Integer idCliente;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AmortizationSystem sistemaAmortizacion;

    private Integer idAnalista;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal montoSolicitado;

    @NotNull
    private Integer plazoMeses;

    @NotNull
    @Enumerated(EnumType.STRING)
    private CreditPurpose destinoCredito;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal ingresosMensuales;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal gastosMensuales;

    @NotNull
    private LocalDate fechaSolicitud;

    @NotNull
    private LocalDateTime fechaHoraRegistro;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal cuotaEvaluacionReferencial;

    @Digits(integer = 1, fraction = 4)
    private BigDecimal teaAsignada;

    @Digits(integer = 8, fraction = 2)
    private BigDecimal cuotaEvaluacionDefinitiva;

    @NotNull
    @Enumerated(EnumType.STRING)
    private LoanRequestStatus estadoSolicitud;

    private LocalDate fechaResolucion;

    @Size(max = 200)
    private String motivoRechazo;

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

    protected LoanRequest() {
    }

    public Integer getIdLoanRequest() {
        return idSolicitud;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public AmortizationSystem getAmortizationSystem() {
        return sistemaAmortizacion;
    }

    public Integer getIdAnalista() {
        return idAnalista;
    }

    public BigDecimal getMontoSolicitado() {
        return montoSolicitado;
    }

    public Integer getPlazoMeses() {
        return plazoMeses;
    }

    public CreditPurpose getCreditPurpose() {
        return destinoCredito;
    }

    public BigDecimal getIngresosMensuales() {
        return ingresosMensuales;
    }

    public BigDecimal getGastosMensuales() {
        return gastosMensuales;
    }

    public LocalDate getFechaLoanRequest() {
        return fechaSolicitud;
    }

    public LocalDateTime getFechaHoraRegistro() {
        return fechaHoraRegistro;
    }

    public BigDecimal getInstallmentEvaluacionReferencial() {
        return cuotaEvaluacionReferencial;
    }

    public BigDecimal getTeaAsignada() {
        return teaAsignada;
    }

    public BigDecimal getInstallmentEvaluacionDefinitiva() {
        return cuotaEvaluacionDefinitiva;
    }

    public LoanRequestStatus getLoanRequestStatus() {
        return estadoSolicitud;
    }

    public LocalDate getFechaResolucion() {
        return fechaResolucion;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
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
















