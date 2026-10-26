const API = import.meta.env.VITE_API ?? '/api'

const cabeceras = { 'Content-Type': 'application/json' }

async function leerRespuesta(respuesta) {
  const cuerpo = await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    throw new Error(cuerpo?.detail ?? 'No se pudo completar la consulta. Intenta de nuevo.')
  }
  return cuerpo
}

export async function buscarPrestamo(numeroOperacion) {
  const respuesta = await fetch(`${API}/loans/${encodeURIComponent(numeroOperacion)}`, { headers: cabeceras })
  return leerRespuesta(respuesta)
}

export async function buscarPrestamosDelCliente(cedulaCliente, estadoCartera, pagina) {
  const parametros = new URLSearchParams({ customerNationalId: cedulaCliente, page: pagina })
  if (estadoCartera) {
    parametros.set('portfolioStatus', estadoCartera)
  }
  const respuesta = await fetch(`${API}/loans?${parametros}`, { headers: cabeceras })
  return leerRespuesta(respuesta)
}

export async function buscarTablaAmortizacion(numeroOperacion, pagina) {
  const parametros = new URLSearchParams({ page: pagina })
  const respuesta = await fetch(`${API}/loans/${encodeURIComponent(numeroOperacion)}/installments?${parametros}`, {
    headers: cabeceras,
  })
  return leerRespuesta(respuesta)
}
