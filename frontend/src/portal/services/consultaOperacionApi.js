import { getAnalystSession } from './sesionAnalista.js'

const API = import.meta.env.VITE_API ?? '/api'

function cabeceras() {
  return {
    'Content-Type': 'application/json',
    'X-Rol': 'ANALISTA',
    'X-Identificacion': getAnalystSession()?.cedula ?? '',
  }
}
async function leerRespuesta(respuesta) {
  const cuerpo = await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    throw new Error(cuerpo?.detail ?? 'No se pudo completar la consulta. Intenta de nuevo.')
  }
  return cuerpo
}

export async function buscarPrestamo(numeroOperacion) {
  const respuesta = await fetch(`${API}/prestamos/${encodeURIComponent(numeroOperacion)}`, { headers: cabeceras() })
  return leerRespuesta(respuesta)
}

export async function buscarPrestamosDelCliente(cedulaCliente, estadoCartera, pagina) {
  const parametros = new URLSearchParams({ cedulaCliente, page: pagina })
  if (estadoCartera) {
    parametros.set('estadoCartera', estadoCartera)
  }
  const respuesta = await fetch(`${API}/prestamos?${parametros}`, { headers: cabeceras() })
  return leerRespuesta(respuesta)
}

export async function buscarTablaAmortizacion(numeroOperacion, pagina) {
  const parametros = new URLSearchParams({ page: pagina })
  const respuesta = await fetch(`${API}/prestamos/${encodeURIComponent(numeroOperacion)}/cuotas?${parametros}`, {
    headers: cabeceras(),
  })
  return leerRespuesta(respuesta)
}
