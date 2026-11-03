import { getAnalystSession } from './sesionAnalista.js'

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

export async function listDayClosings(page) {
  let response
  try {
    response = await fetch(`${API}/analista/cierres-dia?page=${page}`, { headers: headers(), cache: 'no-store' })
  } catch {
    throw new Error('No se pudo conectar con el servidor. Intenta nuevamente.')
  }
  return readResponse(response)
}

export async function runDayClosing(fechaNegocio) {
  let response
  try {
    response = await fetch(`${API}/analista/cierres-dia`, {
      method: 'POST',
      headers: headers(),
      body: JSON.stringify({ fechaNegocio }),
    })
  } catch {
    throw new Error('Se perdió la comunicación con el servidor. Revisa el historial para saber si el cierre se ejecutó antes de reintentar.')
  }
  return readResponse(response)
}

export async function solicitarArchivoRecaudacion() {
  let respuesta
  try {
    respuesta = await fetch(`${API}/analista/archivos-recaudacion`, { method: 'POST', headers: headers() })
  } catch {
    throw new Error('No se pudo conectar con el servidor. Intenta nuevamente.')
  }
  return readResponse(respuesta)
}
