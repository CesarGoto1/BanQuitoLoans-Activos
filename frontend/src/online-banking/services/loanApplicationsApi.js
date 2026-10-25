const API = import.meta.env.VITE_API ?? '/api'

const headers = { 'Content-Type': 'application/json' }

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
    headers,
    body: JSON.stringify(application),
  })
  return readResponse(response)
}

export async function findApplications(customerId, status, page) {
  const params = new URLSearchParams({ customerId, page })
  if (status) {
    params.set('status', status)
  }
  const response = await fetch(`${API}/loan-applications?${params}`, { headers })
  return readResponse(response)
}
