const environment = import.meta.env || {};
const API_ROOT = environment.VITE_API_PRESTAMOS
  ? `${environment.VITE_API_PRESTAMOS.replace(/\/$/, '')}/api`
  : (environment.VITE_API || '/api').replace(/\/$/, '');
const SESSION_KEY = 'banquito.analystIdentification';

export function getAnalystIdentification() {
  return sessionStorage.getItem(SESSION_KEY) || '';
}

export function setAnalystIdentification(identification) {
  sessionStorage.setItem(SESSION_KEY, identification.trim());
}

async function request(path, { method = 'GET', body } = {}) {
  let response;
  try {
    response = await fetch(`${API_ROOT}/analista/solicitudes${path}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
        'X-Rol': 'ANALISTA',
        'X-Identificacion': getAnalystIdentification(),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
      cache: 'no-store',
    });
  } catch {
    throw new Error(method === 'GET'
      ? 'No se pudo conectar con el servidor. Intenta nuevamente.'
      : 'Se perdió la comunicación. Actualiza el detalle para comprobar si la decisión se guardó antes de reintentar.');
  }
  const payload = await response.json().catch(() => null);
  if (!response.ok) {
    const error = new Error(payload?.detail || payload?.title || 'No se pudo completar la operación. Intenta nuevamente.');
    error.status = response.status;
    throw error;
  }
  if (!payload) {
    throw new Error('El servidor devolvió una respuesta incompleta. Actualiza el detalle antes de continuar.');
  }
  return payload;
}

export function obtenerBandejaSolicitudes(estado = 'REGISTRADA') {
  return request(`?estado=${encodeURIComponent(estado)}`);
}

export function obtenerDetalleSolicitud(idSolicitud) {
  return request(`/${encodeURIComponent(idSolicitud)}`);
}

export function approveLoanApplication(applicationId, annualEffectiveRate) {
  return request(`/${encodeURIComponent(applicationId)}/aprobacion`, {
    method: 'POST', body: { teaAsignada: annualEffectiveRate },
  });
}

export function rejectLoanApplication(applicationId, rejectionReason) {
  return request(`/${encodeURIComponent(applicationId)}/rechazo`, {
    method: 'POST', body: { motivoRechazo: rejectionReason },
  });
}

export const prestamosApi = {
  obtenerBandejaSolicitudes,
  obtenerDetalleSolicitud,
  approveLoanApplication,
  rejectLoanApplication,
};

export default prestamosApi;
