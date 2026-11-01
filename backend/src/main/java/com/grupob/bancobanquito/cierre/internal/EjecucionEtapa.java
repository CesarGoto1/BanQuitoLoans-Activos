package com.grupob.bancobanquito.cierre.internal;

import com.grupob.bancobanquito.cierre.PasoCierre;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "ejecucionesEtapa")
class EjecucionEtapa {

    private static final int LONGITUD_MAXIMA_ERROR = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idEjecucion")
    private Integer id;

    @NotNull
    @Column(name = "idCierre")
    private Integer idCierre;

    @NotNull
    @Column(name = "numeroEtapa")
    private Integer numeroEtapa;

    @NotNull
    @Column(name = "numeroIntento")
    private Integer numeroIntento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estadoEtapa")
    private EstadoEtapa estadoEtapa;

    @NotNull
    @Column(name = "fechaHoraInicio")
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fechaHoraFin")
    private LocalDateTime fechaHoraFin;

    @NotNull
    @Column(name = "registrosProcesados")
    private Integer registrosProcesados;

    @Size(max = LONGITUD_MAXIMA_ERROR)
    @Column(name = "mensajeError")
    private String mensajeError;

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

    EjecucionEtapa() {
    }

    static EjecucionEtapa iniciar(Integer idCierre, PasoCierre paso, int numeroIntento, String usuario) {
        EjecucionEtapa ejecucion = new EjecucionEtapa();
        ejecucion.idCierre = idCierre;
        ejecucion.numeroEtapa = paso.numeroEtapa();
        ejecucion.numeroIntento = numeroIntento;
        ejecucion.estadoEtapa = EstadoEtapa.EN_PROCESO;
        ejecucion.fechaHoraInicio = LocalDateTime.now();
        ejecucion.registrosProcesados = 0;
        ejecucion.usuarioCreacion = usuario;
        ejecucion.fechaCreacion = ejecucion.fechaHoraInicio;
        return ejecucion;
    }

    void terminar(int registrosProcesados, String usuario) {
        this.registrosProcesados = registrosProcesados;
        cerrar(EstadoEtapa.EXITOSA, usuario);
    }

    void fallar(String mensajeError, String usuario) {
        this.mensajeError = mensajeError.length() > LONGITUD_MAXIMA_ERROR
                ? mensajeError.substring(0, LONGITUD_MAXIMA_ERROR)
                : mensajeError;
        cerrar(EstadoEtapa.FALLIDA, usuario);
    }

    private void cerrar(EstadoEtapa estado, String usuario) {
        this.estadoEtapa = estado;
        this.fechaHoraFin = LocalDateTime.now();
        this.usuarioModificacion = usuario;
        this.fechaModificacion = this.fechaHoraFin;
    }

    Integer getId() {
        return id;
    }

    Integer getIdCierre() {
        return idCierre;
    }

    Integer getNumeroEtapa() {
        return numeroEtapa;
    }

    Integer getNumeroIntento() {
        return numeroIntento;
    }

    EstadoEtapa getEstadoEtapa() {
        return estadoEtapa;
    }

    LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    Integer getRegistrosProcesados() {
        return registrosProcesados;
    }

    String getMensajeError() {
        return mensajeError;
    }
}
