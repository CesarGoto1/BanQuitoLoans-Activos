const API = import.meta.env.VITE_API

// Pendiente P4: tomar la cédula de la sesión del login simulado.
// Mientras tanto se usa VITE_DEV_IDENTIFICATION del .env local (no se sube).
function currentIdentification() {
  return import.meta.env.VITE_DEV_IDENTIFICATION
}

async function post(path, body) {
  let response
  try {
    response = await fetch(`${API}${path}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Identificacion': currentIdentification(),
        'X-Rol': 'CLIENTE',
      },
      body: JSON.stringify(body),
    })
  } catch {
    throw new Error('No se pudo conectar con el servidor; intente nuevamente más tarde')
  }

  const data = await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(data?.detail ?? 'No se pudo completar la operación')
  }
  return data
}

export function registerLoanApplication(application) {
  return post('/customer/loan-applications', application)
}
