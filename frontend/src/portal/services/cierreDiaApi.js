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
  const response = await fetch(`${API}/analista/cierres-dia?page=${page}`, { headers: headers() })
  return readResponse(response)
}

export async function runDayClosing(fechaNegocio) {
  const response = await fetch(`${API}/analista/cierres-dia`, {
    method: 'POST',
    headers: headers(),
    body: JSON.stringify({ fechaNegocio }),
  })
  return readResponse(response)
}
