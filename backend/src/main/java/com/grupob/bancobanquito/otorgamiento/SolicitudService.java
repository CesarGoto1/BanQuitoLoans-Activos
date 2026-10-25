package com.grupob.bancobanquito.otorgamiento;

import java.util.List;

public interface SolicitudService {

    /**
     * Consulta la bandeja de solicitudes filtradas por su estado (por defecto REGISTRADA)
     * ordenadas por antigüedad de forma ascendente (RF-04).
     *
     * @param estado Estado de solicitud a listar (ej. REGISTRADA).
     * @return Lista de solicitudes ordenadas por antigüedad.
     */
    List<SolicitudBandejaResponse> listarBandejaAnalista(String estado);

    /**
     * Obtiene el detalle financiero de una solicitud para su evaluación por parte
     * del analista de crédito, calculando el remanente mensual y evaluando la
     * capacidad de pago frente al límite regulatorio del 40% (RF-04, RN-09, RN-10, PAR-09).
     *
     * @param idSolicitud Identificador único de la solicitud.
     * @return Detalle de la solicitud con métricas financieras y capacidad de pago.
     */
    SolicitudDetalleResponse obtenerDetalleParaEvaluacion(Integer idSolicitud);
}
