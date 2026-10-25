const BASE_URL = import.meta.env.VITE_API_PRESTAMOS || 'http://localhost:8080';

const CABECERAS_ANALISTA = {
  'Content-Type': 'application/json',
  'X-Rol': 'ANALISTA',
  'X-Identificacion': '1710000001'
};

const SOLICITUDES_MOCK = [
  {
    idSolicitud: 1,
    idCliente: 101,
    montoSolicitado: 5000.00,
    plazoMeses: 12,
    sistemaAmortizacion: 'FRANCES',
    destinoCredito: 'EDUCACION',
    cuotaEvaluacionReferencial: 450.00,
    fechaSolicitud: '2026-10-24',
    fechaHoraRegistro: '2026-10-24T09:15:00',
    estadoSolicitud: 'REGISTRADA'
  },
  {
    idSolicitud: 2,
    idCliente: 102,
    montoSolicitado: 10000.00,
    plazoMeses: 24,
    sistemaAmortizacion: 'ALEMAN',
    destinoCredito: 'BIENES_DURADEROS',
    cuotaEvaluacionReferencial: 550.00,
    fechaSolicitud: '2026-10-24',
    fechaHoraRegistro: '2026-10-24T10:30:00',
    estadoSolicitud: 'REGISTRADA'
  },
  {
    idSolicitud: 3,
    idCliente: 103,
    montoSolicitado: 3000.00,
    plazoMeses: 6,
    sistemaAmortizacion: 'FRANCES',
    destinoCredito: 'SALUD',
    cuotaEvaluacionReferencial: 530.00,
    fechaSolicitud: '2026-10-24',
    fechaHoraRegistro: '2026-10-24T11:45:00',
    estadoSolicitud: 'REGISTRADA'
  }
];

const DETALLES_MOCK = {
  1: {
    idSolicitud: 1,
    idCliente: 101,
    estadoSolicitud: 'REGISTRADA',
    fechaSolicitud: '2026-10-24',
    fechaHoraRegistro: '2026-10-24 09:15:00',
    montoSolicitado: 5000.00,
    plazoMeses: 12,
    sistemaAmortizacion: 'FRANCES',
    destinoCredito: 'EDUCACION',
    cuotaEvaluacionReferencial: 450.00,
    ingresosMensuales: 2000.00,
    gastosMensuales: 800.00,
    ingresoRemanente: 1200.00,
    porcentajeImpactoCuota: 37.50,
    limiteImpactoMaximo: 40.00,
    cuotaMaximaAdmisible: 480.00,
    capacidadPagoValida: true,
    mensajeCapacidadPago: 'La cuota referencial representa el 37.50% del remanente (dentro del límite regulatorio del 40.00%).',
    teaAsignada: null,
    cuotaEvaluacionDefinitiva: null,
    fechaResolucion: null,
    motivoRechazo: null,
    idAnalista: null
  },
  2: {
    idSolicitud: 2,
    idCliente: 102,
    estadoSolicitud: 'REGISTRADA',
    fechaSolicitud: '2026-10-24',
    fechaHoraRegistro: '2026-10-24 10:30:00',
    montoSolicitado: 10000.00,
    plazoMeses: 24,
    sistemaAmortizacion: 'ALEMAN',
    destinoCredito: 'BIENES_DURADEROS',
    cuotaEvaluacionReferencial: 550.00,
    ingresosMensuales: 1500.00,
    gastosMensuales: 500.00,
    ingresoRemanente: 1000.00,
    porcentajeImpactoCuota: 55.00,
    limiteImpactoMaximo: 40.00,
    cuotaMaximaAdmisible: 400.00,
    capacidadPagoValida: false,
    mensajeCapacidadPago: 'ALERTA: La cuota referencial (55.00%) supera el límite regulatorio del 40.00% del remanente mensual (RN-09). Cuota máxima admisible: $400.00.',
    teaAsignada: null,
    cuotaEvaluacionDefinitiva: null,
    fechaResolucion: null,
    motivoRechazo: null,
    idAnalista: null
  },
  3: {
    idSolicitud: 3,
    idCliente: 103,
    estadoSolicitud: 'REGISTRADA',
    fechaSolicitud: '2026-10-24',
    fechaHoraRegistro: '2026-10-24 11:45:00',
    montoSolicitado: 3000.00,
    plazoMeses: 6,
    sistemaAmortizacion: 'FRANCES',
    destinoCredito: 'SALUD',
    cuotaEvaluacionReferencial: 530.00,
    ingresosMensuales: 1100.00,
    gastosMensuales: 1150.00,
    ingresoRemanente: -50.00,
    porcentajeImpactoCuota: 0.00,
    limiteImpactoMaximo: 40.00,
    cuotaMaximaAdmisible: 0.00,
    capacidadPagoValida: false,
    mensajeCapacidadPago: 'El solicitante no dispone de remanente mensual positivo (RN-10).',
    teaAsignada: null,
    cuotaEvaluacionDefinitiva: null,
    fechaResolucion: null,
    motivoRechazo: null,
    idAnalista: null
  }
};

export async function obtenerBandejaSolicitudes(estado = 'REGISTRADA') {
  const url = `${BASE_URL}/api/analista/solicitudes?estado=${encodeURIComponent(estado)}`;
  try {
    const respuesta = await fetch(url, {
      method: 'GET',
      headers: CABECERAS_ANALISTA
    });
    if (!respuesta.ok) {
      const problem = await respuesta.json().catch(() => ({}));
      throw new Error(problem.detail || problem.title || `Error ${respuesta.status}: ${respuesta.statusText}`);
    }
    return await respuesta.json();
  } catch (error) {
    if (error.name === 'TypeError' || error.message.includes('fetch')) {
      console.warn('Backend Spring Boot no detectado en ' + BASE_URL + '. Utilizando datos simulados para desarrollo.');
      return SOLICITUDES_MOCK.filter(s => !estado || s.estadoSolicitud === estado);
    }
    throw error;
  }
}

export async function obtenerDetalleSolicitud(idSolicitud) {
  const url = `${BASE_URL}/api/analista/solicitudes/${idSolicitud}`;
  try {
    const respuesta = await fetch(url, {
      method: 'GET',
      headers: CABECERAS_ANALISTA
    });
    if (!respuesta.ok) {
      const problem = await respuesta.json().catch(() => ({}));
      throw new Error(problem.detail || problem.title || `Error ${respuesta.status}: ${respuesta.statusText}`);
    }
    return await respuesta.json();
  } catch (error) {
    if (error.name === 'TypeError' || error.message.includes('fetch')) {
      const mock = DETALLES_MOCK[Number(idSolicitud)] || DETALLES_MOCK[1];
      return mock;
    }
    throw error;
  }
}

export const prestamosApi = {
  obtenerBandejaSolicitudes,
  obtenerDetalleSolicitud
};

export default prestamosApi;
