package com.grupob.bancobanquito.cobranza.internal;

import com.grupob.bancobanquito.cobranza.ResumenFotoCartera;
import com.grupob.bancobanquito.cobranza.ServicioClasificacionCartera;
import com.grupob.bancobanquito.cobranza.ServicioFotografiaCartera;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/cobranza")
public class CobranzaController {

    private final ServicioClasificacionCartera servicioClasificacionCartera;
    private final ServicioFotografiaCartera servicioFotografiaCartera;
    private final CambioEstadoOperacionRepository cambioEstadoRepository;

    public CobranzaController(
            ServicioClasificacionCartera servicioClasificacionCartera,
            ServicioFotografiaCartera servicioFotografiaCartera,
            CambioEstadoOperacionRepository cambioEstadoRepository) {
        this.servicioClasificacionCartera = servicioClasificacionCartera;
        this.servicioFotografiaCartera = servicioFotografiaCartera;
        this.cambioEstadoRepository = cambioEstadoRepository;
    }

    @PostMapping("/clasificacion")
    public ResponseEntity<String> ejecutarClasificacion(
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestHeader(name = "X-Rol", required = false) String rol,
            @RequestHeader(name = "X-Identificacion", required = false) String identificacion) {

        validarCabeceras(rol, identificacion);
        int total = this.servicioClasificacionCartera.clasificarCartera(fecha);
        return ResponseEntity.ok("Clasificacion de cartera ejecutada exitosamente. Operaciones procesadas: " + total);
    }

    @PostMapping("/fotografia")
    public ResponseEntity<String> ejecutarFotografia(
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestHeader(name = "X-Rol", required = false) String rol,
            @RequestHeader(name = "X-Identificacion", required = false) String identificacion) {

        validarCabeceras(rol, identificacion);
        int total = this.servicioFotografiaCartera.tomarFotografiaDiaria(fecha);
        return ResponseEntity.ok("Fotografia diaria tomada exitosamente. Operaciones registradas: " + total);
    }

    @GetMapping("/cambios-estado/{idOperacion}")
    public ResponseEntity<List<CambioEstadoOperacion>> listarCambiosEstado(
            @PathVariable("idOperacion") Integer idOperacion,
            @RequestHeader(name = "X-Rol", required = false) String rol,
            @RequestHeader(name = "X-Identificacion", required = false) String identificacion) {

        validarCabeceras(rol, identificacion);
        List<CambioEstadoOperacion> cambios = this.cambioEstadoRepository
                .findByIdOperacionOrderByFechaCambioDesc(idOperacion);
        return ResponseEntity.ok(cambios);
    }

    @GetMapping("/fotos")
    public ResponseEntity<List<ResumenFotoCartera>> listarFotosPorFecha(
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestHeader(name = "X-Rol", required = false) String rol,
            @RequestHeader(name = "X-Identificacion", required = false) String identificacion) {

        validarCabeceras(rol, identificacion);
        List<ResumenFotoCartera> fotos = this.servicioFotografiaCartera.consultarFotosPorFecha(fecha);
        return ResponseEntity.ok(fotos);
    }

    private void validarCabeceras(String rol, String identificacion) {
        if (rol == null || !"ANALISTA".equalsIgnoreCase(rol.trim())) {
            throw new IllegalArgumentException("Acceso no autorizado: rol ANALISTA requerido.");
        }
        if (identificacion == null || identificacion.trim().isBlank()) {
            throw new IllegalArgumentException("Cabecera X-Identificacion requerida.");
        }
    }
}
