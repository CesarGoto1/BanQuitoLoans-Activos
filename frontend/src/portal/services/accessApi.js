const API = import.meta.env.VITE_API ?? '/api'

export async function loginAnalyst(nationalId) {
  const response = await fetch(`${API}/analyst-sessions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ nationalId }),
  }).catch(() => {
    throw new Error('No se pudo conectar con el servidor.')
  })
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(body?.detail ?? 'No se pudo ingresar. Intenta de nuevo.')
  }
  return body
}
