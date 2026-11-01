package com.grupob.bancobanquito.pagos.internal;

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
import java.time.LocalDateTime;

@Entity
@Table(name = "detallesPago")
class DetallePago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDetallePago")
    private Integer id;

    @NotNull
    @Column(name = "idPago")
    private Integer idPago;

    @Column(name = "idCuota")
    private Integer idCuota;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "aplicadoSeguro")
    private BigDecimal aplicadoSeguro;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "aplicadoMora")
    private BigDecimal aplicadoMora;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "aplicadoInteres")
    private BigDecimal aplicadoInteres;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "aplicadoCapital")
    private BigDecimal aplicadoCapital;

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

    DetallePago() {
    }

    DetallePago(Integer id) {
        this.id = id;
    }

    Integer getId() {
        return id;
    }

    Integer getIdPago() {
        return idPago;
    }

    void setIdPago(Integer idPago) {
        this.idPago = idPago;
    }

    Integer getIdCuota() {
        return idCuota;
    }

    void setIdCuota(Integer idCuota) {
        this.idCuota = idCuota;
    }

    BigDecimal getAplicadoSeguro() {
        return aplicadoSeguro;
    }

    void setAplicadoSeguro(BigDecimal aplicadoSeguro) {
        this.aplicadoSeguro = aplicadoSeguro;
    }

    BigDecimal getAplicadoMora() {
        return aplicadoMora;
    }

    void setAplicadoMora(BigDecimal aplicadoMora) {
        this.aplicadoMora = aplicadoMora;
    }

    BigDecimal getAplicadoInteres() {
        return aplicadoInteres;
    }

    void setAplicadoInteres(BigDecimal aplicadoInteres) {
        this.aplicadoInteres = aplicadoInteres;
    }

    BigDecimal getAplicadoCapital() {
        return aplicadoCapital;
    }

    void setAplicadoCapital(BigDecimal aplicadoCapital) {
        this.aplicadoCapital = aplicadoCapital;
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
