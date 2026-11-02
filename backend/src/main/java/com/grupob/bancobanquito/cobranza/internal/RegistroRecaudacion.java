package com.grupob.bancobanquito.cobranza.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "registrosRecaudacion")
class RegistroRecaudacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idRegistro")
    private Integer id;

    @Column(name = "idArchivo", nullable = false)
    private Integer idArchivo;

    @Column(name = "numeroSecuencial", nullable = false)
    private Integer numeroSecuencial;

    @Column(name = "uuidTransaccion")
    private UUID uuidTransaccion;

    @Column(name = "numeroOperacion", length = 20)
    private String numeroOperacion;

    @Column(name = "cedulaCliente", length = 10)
    private String cedulaCliente;

    @Column(name = "montoPagado", precision = 10, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "fechaHoraCobro")
    private LocalDateTime fechaHoraCobro;

    @Column(name = "canalPago", length = 10)
    private String canalPago;

    @Column(name = "referenciaPago", length = 100)
    private String referenciaPago;

    @Column(name = "lineaOriginal", length = 500)
    private String lineaOriginal;

    @Column(name = "estadoRegistro", nullable = false, length = 10)
    private String estadoRegistro;

    @Column(name = "codigoRechazo", length = 10)
    private String codigoRechazo;

    @Column(name = "usuarioCreacion", nullable = false, length = 50, updatable = false)
    private String usuarioCreacion;

    @Column(name = "fechaCreacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    protected RegistroRecaudacion() {
    }

    RegistroRecaudacion(Integer idArchivo, RegistroRecaudacionLeido entrada, String estado, String codigo) {
        this.idArchivo = idArchivo;
        numeroSecuencial = entrada.posicion();
        uuidTransaccion = entrada.uuidTransaccion();
        numeroOperacion = entrada.numeroOperacion();
        cedulaCliente = entrada.identificacion();
        BigDecimal monto = entrada.monto();
        montoPagado = monto != null && monto.scale() <= 2
                && monto.abs().compareTo(new BigDecimal("99999999.99")) <= 0 ? monto : null;
        fechaHoraCobro = entrada.fechaCobro();
        canalPago = entrada.canal();
        referenciaPago = entrada.referencia();
        lineaOriginal = entrada.lineaOriginal().length() <= 500 ? entrada.lineaOriginal() : null;
        finalizar(estado, codigo);
        usuarioCreacion = "system";
        fechaCreacion = LocalDateTime.now();
    }

    void finalizar(String estado, String codigo) {
        estadoRegistro = estado;
        codigoRechazo = codigo;
    }

    Integer id() {
        return id;
    }
}
