import { getAnalystSession } from './sesionAnalista.js'

const USE_SAMPLE_DATA = false

const API = import.meta.env.VITE_API ?? '/api'

function headers() {
  return {
    'Content-Type': 'application/json',
    'X-Rol': 'ANALISTA',
    'X-Identificacion': getAnalystSession()?.cedula ?? '',
  }
}

async function readResponse(response) {
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(body?.detail ?? 'No se pudo completar la operación. Intenta de nuevo.')
  }
  return body
}

const samplePreview = {
  idSolicitud: 9001,
  cedulaCliente: '1710034065',
  nombreCliente: 'María Fernanda Andrade Lema',
  montoAprobado: 10000.0,
  teaAsignada: 0.155,
  plazoMeses: 2,
  metodoAmortizacion: 'FRENCH',
  seguroMensual: 6.0,
  contribucionSolca: 50.0,
  montoNeto: 9950.0,
  fechaDesembolso: '2026-10-31',
  cuentas: [
    { numeroCuenta: '2200000001', tipoCuenta: 'AHORROS' },
    { numeroCuenta: '3100000001', tipoCuenta: 'CORRIENTE' },
  ],
}

export async function findPendingDisbursements(page) {
  if (USE_SAMPLE_DATA) {
    return {
      content: [
        {
          idSolicitud: samplePreview.idSolicitud,
          idCliente: 1,
          cedulaCliente: samplePreview.cedulaCliente,
          nombreCliente: samplePreview.nombreCliente,
          montoAprobado: samplePreview.montoAprobado,
          teaAsignada: samplePreview.teaAsignada,
          plazoMeses: samplePreview.plazoMeses,
          metodoAmortizacion: samplePreview.metodoAmortizacion,
          fechaResolucion: '2026-10-30',
        },
      ],
      page: { size: 10, number: 0, totalElements: 1, totalPages: 1 },
    }
  }
  const response = await fetch(`${API}/desembolsos-pendientes?page=${page}`, { headers: headers() })
  return readResponse(response)
}

export async function getDisbursementPreview(idSolicitud) {
  if (USE_SAMPLE_DATA) {
    return { ...samplePreview, idSolicitud }
  }
  const response = await fetch(`${API}/vistas-previas-desembolso/${idSolicitud}`, { headers: headers() })
  return readResponse(response)
}

export async function disburse(idSolicitud, cuentaDestino) {
  if (USE_SAMPLE_DATA) {
    return {
      idSolicitud,
      numeroOperacion: 'OP-20261031-009001',
      montoNeto: samplePreview.montoNeto,
      cuentaDestino,
      fechaDesembolso: samplePreview.fechaDesembolso,
    }
  }
  const response = await fetch(`${API}/desembolsos`, {
    method: 'POST',
    headers: headers(),
    body: JSON.stringify({ idSolicitud, cuentaDestino }),
  })
  return readResponse(response)
}
