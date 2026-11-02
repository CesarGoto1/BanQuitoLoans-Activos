import { getAnalystSession } from './sesionAnalista.js'

const API = import.meta.env?.VITE_API ?? '/api'

export async function consultarCuadre(fechaNegocio) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(fechaNegocio ?? '')) {
    throw new Error('Indica una fecha de negocio con formato AAAA-MM-DD.')
  }
  let response
  try {
    response = await fetch(`${API}/analista/cuadres-contables?fechaNegocio=${encodeURIComponent(fechaNegocio)}`, {
      headers: {
        'X-Rol': 'ANALISTA',
        'X-Identificacion': getAnalystSession()?.cedula ?? '',
      },
      cache: 'no-store',
    })
  } catch {
    throw new Error('No se pudo conectar con el servidor. Intenta nuevamente.')
  }
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    const error = new Error(body?.detail ?? 'No se pudo consultar el cuadre contable.')
    error.status = response.status
    throw error
  }
  if (!body || body.fechaNegocio !== fechaNegocio
      || typeof body.cuadrado !== 'boolean'
      || !Number.isSafeInteger(body.cantidadAsientos) || body.cantidadAsientos < 0
      || !['totalDebitos', 'totalCreditos', 'diferencia'].every((campo) => typeof body[campo] === 'number' && Number.isFinite(body[campo]))
      || typeof body.observacion !== 'string' || !body.observacion.trim()) {
    throw new Error('El servidor devolvió una respuesta incompleta o inválida. Intenta nuevamente.')
  }
  return body
}
