import { getCustomerSession } from './customerSession.js'

const API = import.meta.env.VITE_API ?? '/api'

const headers = { 'Content-Type': 'application/json' }

function customerHeaders() {
  return {
    ...headers,
    'X-Rol': 'CLIENTE',
    'X-Identificacion': getCustomerSession()?.nationalId ?? '',
  }
}

async function readResponse(response) {
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(body?.detail ?? 'No se pudo completar la operación. Intenta de nuevo.')
  }
  return body
}

export async function registerApplication(application) {
  const response = await fetch(`${API}/loan-applications`, {
    method: 'POST',
    headers: customerHeaders(),
    body: JSON.stringify(application),
  })
  return readResponse(response)
}

export async function findApplications(status, page) {
  const params = new URLSearchParams({ page })
  if (status) {
    params.set('status', status)
  }
  const response = await fetch(`${API}/loan-applications?${params}`, { headers: customerHeaders() })
  return readResponse(response)
}

export async function simulateLoan(simulation) {
  const response = await fetch(`${API}/loan-simulations`, {
    method: 'POST',
    headers,
    body: JSON.stringify(simulation),
  })
  return readResponse(response)
}

export async function findCreditLimits() {
  const response = await fetch(`${API}/credit-limits`, { headers })
  return readResponse(response)
}
