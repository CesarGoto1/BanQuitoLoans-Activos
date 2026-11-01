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
    throw new Error(cuerpo?.detail ?? 'No se pudo completar la operación. Intenta de nuevo.')
  }
  return cuerpo
}

function rutaOperacion(numeroOperacion) {
  return `${API}/analista/operaciones/${encodeURIComponent(numeroOperacion)}`
}

export async function simularAbono(numeroOperacion, monto, modalidad) {
  const respuesta = await fetch(`${rutaOperacion(numeroOperacion)}/simulaciones-abono`, {
    method: 'POST',
    headers: cabeceras(),
    body: JSON.stringify({ monto, modalidad }),
  })
  return leerRespuesta(respuesta)
}

export async function registrarAbono(numeroOperacion, monto, modalidad) {
  const respuesta = await fetch(`${rutaOperacion(numeroOperacion)}/abonos`, {
    method: 'POST',
    headers: cabeceras(),
    body: JSON.stringify({ monto, modalidad }),
  })
  return leerRespuesta(respuesta)
}

export async function consultarCancelacion(numeroOperacion) {
  const respuesta = await fetch(`${rutaOperacion(numeroOperacion)}/valor-cancelacion`, { headers: cabeceras() })
  return leerRespuesta(respuesta)
}

export async function registrarCancelacion(numeroOperacion, montoRecibido) {
  const respuesta = await fetch(`${rutaOperacion(numeroOperacion)}/cancelaciones`, {
    method: 'POST',
    headers: cabeceras(),
    body: JSON.stringify({ montoRecibido }),
  })
  return leerRespuesta(respuesta)
}
