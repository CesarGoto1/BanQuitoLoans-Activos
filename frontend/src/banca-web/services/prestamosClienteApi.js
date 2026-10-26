import { getCustomerSession } from './sesionCliente.js'

const API = import.meta.env.VITE_API ?? '/api'

function headers() {
  return {
    'Content-Type': 'application/json',
    'X-Rol': 'CLIENTE',
    'X-Identificacion': getCustomerSession()?.cedulaCliente ?? '',
  }
}

async function readResponse(response) {
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(body?.detail ?? 'No se pudo completar la consulta. Intenta de nuevo.')
  }
  return body
}

export async function findCustomerLoans(page) {
  const params = new URLSearchParams({ page })
  const response = await fetch(`${API}/prestamos?${params}`, { headers: headers() })
  return readResponse(response)
}

export async function findLoanInstallments(numeroOperacion, page) {
  const params = new URLSearchParams({ page })
  const response = await fetch(`${API}/prestamos/${encodeURIComponent(numeroOperacion)}/cuotas?${params}`, { headers: headers() })
  return readResponse(response)
}
