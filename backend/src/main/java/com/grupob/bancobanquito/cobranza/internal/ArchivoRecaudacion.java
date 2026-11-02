package com.grupob.bancobanquito.cobranza.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "archivosRecaudacion")
class ArchivoRecaudacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idArchivo")
    private Integer id;

    @Column(name = "fechaCarga", nullable = false)
    private LocalDate fechaCarga;

    @Column(name = "nombreArchivo", nullable = false, length = 30)
    private String nombreArchivo;

    @Column(name = "hashSha256", length = 64)
    private String hashSha256;

    @Column(name = "fechaProcesoArchivo")
    private LocalDate fechaProcesoArchivo;

    @Column(name = "fechaHoraGeneracion")
    private LocalDateTime fechaHoraGeneracion;

    @Column(name = "totalRegistros")
    private Integer totalRegistros;

    @Column(name = "montoTotalArchivo", precision = 12, scale = 2)
    private BigDecimal montoTotalArchivo;

    @Column(name = "estadoArchivo", nullable = false, length = 10)
    private String estadoArchivo;

    @Column(name = "codigoRechazo", length = 10)
    private String codigoRechazo;

    @Column(name = "usuarioCreacion", nullable = false, length = 50, updatable = false)
    private String usuarioCreacion;

    @Column(name = "fechaCreacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    protected ArchivoRecaudacion() {
    }

    static ArchivoRecaudacion procesado(ArchivoRecaudacionLeido entrada, LocalDate fecha) {
        ArchivoRecaudacion archivo = nuevo(entrada.nombre(), fecha);
        archivo.hashSha256 = entrada.hashSha256();
        archivo.fechaProcesoArchivo = entrada.fechaProceso();
        archivo.fechaHoraGeneracion = entrada.fechaGeneracion();
        archivo.totalRegistros = entrada.registros().size();
        archivo.montoTotalArchivo = entrada.montoTotal();
        archivo.estadoArchivo = "PROCESADO";
        return archivo;
    }

    static ArchivoRecaudacion rechazado(String nombre, LocalDate fecha, String codigo) {
        ArchivoRecaudacion archivo = nuevo(nombre, fecha);
        archivo.estadoArchivo = "RECHAZADO";
        archivo.codigoRechazo = codigo;
        return archivo;
    }

    private static ArchivoRecaudacion nuevo(String nombre, LocalDate fecha) {
        ArchivoRecaudacion archivo = new ArchivoRecaudacion();
        archivo.nombreArchivo = nombre;
        archivo.fechaCarga = fecha;
        archivo.usuarioCreacion = "system";
        archivo.fechaCreacion = LocalDateTime.now();
        return archivo;
    }

    Integer id() {
        return id;
    }
}
