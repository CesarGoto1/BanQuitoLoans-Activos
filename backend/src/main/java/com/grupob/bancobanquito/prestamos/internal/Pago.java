package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.amortizacion.ModalidadAbono;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "PagoPrestamo")
@Table(name = "pagos")
class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPago")
    private Integer id;

    @Column(name = "idOperacion")
    private Integer idOperacion;

    @Column(name = "idAnalista")
    private Integer idAnalista;

    @Enumerated(EnumType.STRING)
    @Column(name = "origenPago")
    private OrigenPago origen;

    @Column(name = "fechaPago")
    private LocalDate fechaPago;

    @Column(name = "montoRecibido")
    private BigDecimal montoRecibido;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidadAbono")
    private ModalidadAbono modalidadAbono;

    @Column(name = "excedenteDevuelto")
    private BigDecimal excedenteDevuelto;

    @Column(name = "usuarioCreacion", updatable = false)
    private String usuarioCreacion;

    @Column(name = "fechaCreacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "pago", cascade = CascadeType.PERSIST)
    private List<DetallePago> detalles = new ArrayList<>();

    Pago() {
    }

    Pago(Integer idOperacion, Integer idAnalista, OrigenPago origen, LocalDate fechaPago, BigDecimal montoRecibido,
         ModalidadAbono modalidadAbono, String usuarioCreacion) {
        this.idOperacion = idOperacion;
        this.idAnalista = idAnalista;
        this.origen = origen;
        this.fechaPago = fechaPago;
        this.montoRecibido = montoRecibido;
        this.modalidadAbono = modalidadAbono;
        this.excedenteDevuelto = BigDecimal.ZERO;
        this.usuarioCreacion = usuarioCreacion;
        this.fechaCreacion = LocalDateTime.now();
    }

    void agregarDetalle(Integer idCuota, BigDecimal seguro, BigDecimal mora, BigDecimal interes, BigDecimal capital) {
        detalles.add(new DetallePago(this, idCuota, seguro, mora, interes, capital, usuarioCreacion, fechaCreacion));
    }

    Integer getId() {
        return id;
    }

    Integer getIdOperacion() {
        return idOperacion;
    }

    OrigenPago getOrigen() {
        return origen;
    }

    LocalDate getFechaPago() {
        return fechaPago;
    }

    BigDecimal getMontoRecibido() {
        return montoRecibido;
    }

    ModalidadAbono getModalidadAbono() {
        return modalidadAbono;
    }

    List<DetallePago> getDetalles() {
        return detalles;
    }
}
