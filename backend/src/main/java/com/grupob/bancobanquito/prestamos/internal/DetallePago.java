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
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "DetallePagoPrestamo")
@Table(name = "detallesPago")
class DetallePago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDetallePago")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idPago")
    private Pago pago;

    @Column(name = "idCuota")
    private Integer idCuota;

    @Column(name = "aplicadoSeguro")
    private BigDecimal aplicadoSeguro;

    @Column(name = "aplicadoMora")
    private BigDecimal aplicadoMora;

    @Column(name = "aplicadoInteres")
    private BigDecimal aplicadoInteres;

    @Column(name = "aplicadoCapital")
    private BigDecimal aplicadoCapital;

    @Column(name = "usuarioCreacion", updatable = false)
    private String usuarioCreacion;

    @Column(name = "fechaCreacion", updatable = false)
    private LocalDateTime fechaCreacion;

    DetallePago() {
    }

    DetallePago(Pago pago, Integer idCuota, BigDecimal aplicadoSeguro, BigDecimal aplicadoMora,
                BigDecimal aplicadoInteres, BigDecimal aplicadoCapital, String usuarioCreacion,
                LocalDateTime fechaCreacion) {
        this.pago = pago;
        this.idCuota = idCuota;
        this.aplicadoSeguro = aplicadoSeguro;
        this.aplicadoMora = aplicadoMora;
        this.aplicadoInteres = aplicadoInteres;
        this.aplicadoCapital = aplicadoCapital;
        this.usuarioCreacion = usuarioCreacion;
        this.fechaCreacion = fechaCreacion;
    }

    Integer getIdCuota() {
        return idCuota;
    }

    BigDecimal getAplicadoSeguro() {
        return aplicadoSeguro;
    }

    BigDecimal getAplicadoMora() {
        return aplicadoMora;
    }

    BigDecimal getAplicadoInteres() {
        return aplicadoInteres;
    }

    BigDecimal getAplicadoCapital() {
        return aplicadoCapital;
    }
}
