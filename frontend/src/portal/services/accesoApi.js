const API = import.meta.env.VITE_API ?? '/api'

export async function loginAnalyst(cedula) {
  const response = await fetch(`${API}/sesiones-analista`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ cedula }),
  }).catch(() => {
    throw new Error('No se pudo conectar con el servidor.')
  })
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(body?.detail ?? 'No se pudo ingresar. Intenta de nuevo.')
  }
  return body
}
