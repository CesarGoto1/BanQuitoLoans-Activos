package com.grupob.bancobanquito.contabilidad.internal;

import com.grupob.bancobanquito.contabilidad.TipoAsiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "asientosContables")
class AsientoContable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idAsiento")
    private Integer id;

    @Column(name = "idOperacion")
    private Integer idOperacion;

    @NotNull
    @Column(name = "fechaAsiento")
    private LocalDate fechaAsiento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipoAsiento")
    private TipoAsiento tipoAsiento;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCuentaDebito")
    private CuentaContable cuentaDebito;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCuentaCredito")
    private CuentaContable cuentaCredito;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "montoAsiento")
    private BigDecimal montoAsiento;

    @NotNull
    @Size(max = 60)
    @Column(name = "claveIdempotencia")
    private String claveIdempotencia;

    @NotNull
    @Size(max = 50)
    @Column(name = "usuarioCreacion", updatable = false)
    private String usuarioCreacion;

    @NotNull
    @Column(name = "fechaCreacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Size(max = 50)
    @Column(name = "usuarioModificacion")
    private String usuarioModificacion;

    @Column(name = "fechaModificacion")
    private LocalDateTime fechaModificacion;

    AsientoContable() {
    }

    AsientoContable(Integer id) {
        this.id = id;
    }

    Integer getId() {
        return id;
    }

    Integer getIdOperacion() {
        return idOperacion;
    }

    void setIdOperacion(Integer idOperacion) {
        this.idOperacion = idOperacion;
    }

    LocalDate getFechaAsiento() {
        return fechaAsiento;
    }

    void setFechaAsiento(LocalDate fechaAsiento) {
        this.fechaAsiento = fechaAsiento;
    }

    TipoAsiento getTipoAsiento() {
        return tipoAsiento;
    }

    void setTipoAsiento(TipoAsiento tipoAsiento) {
        this.tipoAsiento = tipoAsiento;
    }

    CuentaContable getCuentaDebito() {
        return cuentaDebito;
    }

    void setCuentaDebito(CuentaContable cuentaDebito) {
        this.cuentaDebito = cuentaDebito;
    }

    CuentaContable getCuentaCredito() {
        return cuentaCredito;
    }

    void setCuentaCredito(CuentaContable cuentaCredito) {
        this.cuentaCredito = cuentaCredito;
    }

    BigDecimal getMontoAsiento() {
        return montoAsiento;
    }

    void setMontoAsiento(BigDecimal montoAsiento) {
        this.montoAsiento = montoAsiento;
    }

    String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    void setClaveIdempotencia(String claveIdempotencia) {
        this.claveIdempotencia = claveIdempotencia;
    }

    String getUsuarioCreacion() {
        return usuarioCreacion;
    }

    void setUsuarioCreacion(String usuarioCreacion) {
        this.usuarioCreacion = usuarioCreacion;
    }

    LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    String getUsuarioModificacion() {
        return usuarioModificacion;
    }

    void setUsuarioModificacion(String usuarioModificacion) {
        this.usuarioModificacion = usuarioModificacion;
    }

    LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    void setFechaModificacion(LocalDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }
}
