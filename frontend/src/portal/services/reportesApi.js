import { getAnalystSession } from './sesionAnalista.js'

const API = import.meta.env?.VITE_API ?? '/api'
const tipos = ['morosidad', 'composicion', 'provisiones', 'salud', 'roll-rate']

function validarFecha(fecha) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(fecha ?? '')
      || !Number.isFinite(Date.parse(fecha)) || new Date(fecha).toISOString().slice(0, 10) !== fecha) {
    throw new Error('Indica una fecha válida con formato AAAA-MM-DD.')
  }
}

export async function consultarReporte(tipo, fecha, fechaFinal) {
  if (!tipos.includes(tipo)) throw new Error('El reporte solicitado no existe.')
  validarFecha(fecha)
  const parametros = new URLSearchParams()
  if (tipo === 'roll-rate') {
    validarFecha(fechaFinal)
    if (fecha >= fechaFinal) throw new Error('La fecha inicial debe ser anterior a la fecha final.')
    parametros.set('fechaInicial', fecha)
    parametros.set('fechaFinal', fechaFinal)
  } else {
    parametros.set('fechaCorte', fecha)
  }
  let respuesta
  try {
    respuesta = await fetch(`${API}/analista/reportes/${tipo}?${parametros}`, {
      headers: { 'X-Rol': 'ANALISTA', 'X-Identificacion': getAnalystSession()?.cedula ?? '' },
      cache: 'no-store',
    })
  } catch {
    throw new Error('No se pudo conectar con el servidor. Intenta nuevamente.')
  }
  const datos = await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    const error = new Error(datos?.detail ?? 'No se pudo consultar el reporte.')
    error.status = respuesta.status
    throw error
  }
  const corte = tipo === 'salud' ? datos?.morosidad?.fechaCorte : datos?.fechaCorte
  if (!datos || (tipo === 'roll-rate'
    ? datos.fechaInicial !== fecha || datos.fechaFinal !== fechaFinal || !Array.isArray(datos.filas)
    : corte !== fecha)
    || (['composicion', 'provisiones'].includes(tipo) && !Array.isArray(datos.tramos))
    || (tipo === 'salud' && (!datos.concentracion || !Array.isArray(datos.concentracion.mayoresClientes)
      || !['porMontoOriginal', 'porPlazo', 'porSistemaAmortizacion'].every(campo => Array.isArray(datos[campo]))))) {
    throw new Error('El servidor devolvió un reporte incompleto o inválido.')
  }
  return datos
}
