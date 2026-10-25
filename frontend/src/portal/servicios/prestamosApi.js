const BASE_URL = import.meta.env.VITE_API_PRESTAMOS || 'http://localhost:8080';

// Cabeceras simuladas obligatorias según estándares de desarrollo de BanQuito (QA-12)
const CABECERAS_ANALISTA = {
  'Content-Type': 'application/json',
  'X-Rol': 'ANALISTA',
  'X-Identificacion': '1710000001'
};

/**
 * Procesa la respuesta HTTP y extrae el mensaje de ProblemDetail (RFC 7807/9457)
 * en caso de error.
 */
async function procesarRespuesta(respuesta) {
  if (!respuesta.ok) {
    let detalleError = 'Error al comunicarse con el servidor bancario.';
    try {
      const problem = await respuesta.json();
      detalleError = problem.detail || problem.title || detalleError;
    } catch (_) {
      detalleError = respuesta.statusText || detalleError;
    }
    throw new Error(detalleError);
  }
  return respuesta.json();
}

/**
 * Consulta la bandeja de solicitudes de crédito filtradas por estado (RF-04).
 * Por defecto recupera las que están en estado REGISTRADA ordenadas por antigüedad.
 *
 * @param {string} estado Estado a filtrar ('REGISTRADA', 'APROBADA', 'RECHAZADA')
 * @returns {Promise<Array>} Lista de SolicitudBandejaResponse
 */
export async function obtenerBandejaSolicitudes(estado = 'REGISTRADA') {
  const url = `${BASE_URL}/api/analista/solicitudes?estado=${encodeURIComponent(estado)}`;
  const respuesta = await fetch(url, {
    method: 'GET',
    headers: CABECERAS_ANALISTA
  });
  return procesarRespuesta(respuesta);
}

/**
 * Consulta el detalle financiero y cálculo de capacidad de pago de una solicitud (RF-04).
 *
 * @param {number|string} idSolicitud Identificador único de la solicitud
 * @returns {Promise<Object>} SolicitudDetalleResponse
 */
export async function obtenerDetalleSolicitud(idSolicitud) {
  const url = `${BASE_URL}/api/analista/solicitudes/${idSolicitud}`;
  const respuesta = await fetch(url, {
    method: 'GET',
    headers: CABECERAS_ANALISTA
  });
  return procesarRespuesta(respuesta);
}

export const prestamosApi = {
  obtenerBandejaSolicitudes,
  obtenerDetalleSolicitud
};

export default prestamosApi;
