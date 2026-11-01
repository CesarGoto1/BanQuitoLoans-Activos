package com.grupob.bancobanquito.pagos.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "pagos")
class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPago")
    private Integer id;

    @NotNull
    @Column(name = "idOperacion")
    private Integer idOperacion;

    @Column(name = "idRegistro")
    private Integer idRegistro;

    @Column(name = "idAnalista")
    private Integer idAnalista;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "origenPago")
    private OrigenPago origenPago;

    @NotNull
    @Column(name = "fechaPago")
    private LocalDate fechaPago;

    @Digits(integer = 8, fraction = 2)
    @Column(name = "montoRecibido")
    private BigDecimal montoRecibido;

    @Size(max = 15)
    @Column(name = "modalidadAbono")
    private String modalidadAbono;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Column(name = "excedenteDevuelto")
    private BigDecimal excedenteDevuelto;

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

    Pago() {
    }

    Pago(Integer id) {
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

    Integer getIdRegistro() {
        return idRegistro;
    }

    void setIdRegistro(Integer idRegistro) {
        this.idRegistro = idRegistro;
    }

    Integer getIdAnalista() {
        return idAnalista;
    }

    void setIdAnalista(Integer idAnalista) {
        this.idAnalista = idAnalista;
    }

    OrigenPago getOrigenPago() {
        return origenPago;
    }

    void setOrigenPago(OrigenPago origenPago) {
        this.origenPago = origenPago;
    }

    LocalDate getFechaPago() {
        return fechaPago;
    }

    void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    BigDecimal getMontoRecibido() {
        return montoRecibido;
    }

    void setMontoRecibido(BigDecimal montoRecibido) {
        this.montoRecibido = montoRecibido;
    }

    String getModalidadAbono() {
        return modalidadAbono;
    }

    void setModalidadAbono(String modalidadAbono) {
        this.modalidadAbono = modalidadAbono;
    }

    BigDecimal getExcedenteDevuelto() {
        return excedenteDevuelto;
    }

    void setExcedenteDevuelto(BigDecimal excedenteDevuelto) {
        this.excedenteDevuelto = excedenteDevuelto;
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
