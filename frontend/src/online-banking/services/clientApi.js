const API = import.meta.env.VITE_API || '/api'

export const initialIdentification = import.meta.env.VITE_IDENTIFICATION || '1712345678'

async function request(ruta) {
  const respuesta = await fetch(`${API}${ruta}`, {
    headers: {
      'X-Identification': initialIdentification,
      'X-Role': 'CLIENT',
      Accept: 'application/json',
    },
  })

  if (!respuesta.ok) {
    let mensaje = 'No fue posible completar la consulta.'
    try {
      const problema = await respuesta.json()
      mensaje = problema.detail || problema.title || mensaje
    } catch {
    }
    throw new Error(mensaje)
  }

  return respuesta.json()
}

export const clientApi = {
  fetchRequests: (estado = '') =>
    request(`/client/requests${estado ? `?estado=${encodeURIComponent(estado)}` : ''}`),
  fetchOperations: () => request('/client/operations'),
  fetchInstallments: (numeroOperacion) =>
    request(`/client/operations/${encodeURIComponent(numeroOperacion)}/installments`),
}








