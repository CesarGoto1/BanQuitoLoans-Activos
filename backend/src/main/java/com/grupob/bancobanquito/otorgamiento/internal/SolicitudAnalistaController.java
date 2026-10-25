package com.grupob.bancobanquito.otorgamiento.internal;

import com.grupob.bancobanquito.otorgamiento.SolicitudBandejaResponse;
import com.grupob.bancobanquito.otorgamiento.SolicitudDetalleResponse;
import com.grupob.bancobanquito.otorgamiento.SolicitudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/analista/solicitudes")
public class SolicitudAnalistaController {

    private static final Logger log = LoggerFactory.getLogger(SolicitudAnalistaController.class);

    private final SolicitudService solicitudService;

    public SolicitudAnalistaController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping
    public ResponseEntity<List<SolicitudBandejaResponse>> listarBandeja(
            @RequestParam(name = "estado", defaultValue = "REGISTRADA") String estado,
            @RequestHeader(name = "X-Rol", required = false) String rol,
            @RequestHeader(name = "X-Identificacion", required = false) String identificacion) {

        validarCabecerasSeguridad(rol, identificacion);
        String cedulaEnmascarada = enmascararIdentificacion(identificacion);
        log.info("Analista [{}] consulta bandeja de solicitudes (estado: {})", cedulaEnmascarada, estado);

        List<SolicitudBandejaResponse> bandeja = this.solicitudService.listarBandejaAnalista(estado);
        return ResponseEntity.ok(bandeja);
    }

    @GetMapping("/{idSolicitud}")
    public ResponseEntity<SolicitudDetalleResponse> obtenerDetalle(
            @PathVariable("idSolicitud") Integer idSolicitud,
            @RequestHeader(name = "X-Rol", required = false) String rol,
            @RequestHeader(name = "X-Identificacion", required = false) String identificacion) {

        validarCabecerasSeguridad(rol, identificacion);
        String cedulaEnmascarada = enmascararIdentificacion(identificacion);
        log.info("Analista [{}] consulta detalle de la solicitud ID: {}", cedulaEnmascarada, idSolicitud);

        SolicitudDetalleResponse detalle = this.solicitudService.obtenerDetalleParaEvaluacion(idSolicitud);
        return ResponseEntity.ok(detalle);
    }

    private void validarCabecerasSeguridad(String rol, String identificacion) {
        if (rol == null || !rol.trim().equalsIgnoreCase("ANALISTA")) {
            throw new AccesoDenegadoAnalistaException("Acceso denegado: se requiere la cabecera X-Rol con valor ANALISTA.");
        }
        if (identificacion == null || identificacion.trim().isBlank()) {
            throw new CabeceraFaltanteException("La cabecera X-Identificacion es requerida para identificar al analista.");
        }
    }

    private String enmascararIdentificacion(String id) {
        if (id == null) {
            return "******";
        }
        String limpia = id.trim();
        if (limpia.length() <= 4) {
            return "******";
        }
        return limpia.substring(0, 2) + "******" + limpia.substring(limpia.length() - 2);
    }
}
