import { afterEach, beforeEach, test } from 'node:test'
import assert from 'node:assert/strict'
import { consultarCuadre } from './cuadreContableApi.js'
import { saveAnalystSession } from './sesionAnalista.js'

const originalFetch = globalThis.fetch
const resultado = {
  fechaNegocio: '2026-10-31',
  totalDebitos: 10000,
  totalCreditos: 10000,
  diferencia: 0,
  cantidadAsientos: 2,
  cuadrado: true,
  observacion: 'La igualdad no verifica que todos los eventos estén contabilizados.',
}

beforeEach(() => {
  const valores = new Map()
  globalThis.sessionStorage = {
    getItem: (clave) => valores.get(clave) ?? null,
    setItem: (clave, valor) => valores.set(clave, valor),
  }
  saveAnalystSession({ cedula: '1799999901', nombre: 'Analista de prueba' })
})

afterEach(() => {
  globalThis.fetch = originalFetch
  delete globalThis.sessionStorage
})

test('consulta por fecha con la sesión del analista y sin caché', async () => {
  globalThis.fetch = async (url, opciones) => {
    assert.equal(url, '/api/analista/cuadres-contables?fechaNegocio=2026-10-31')
    assert.equal(opciones.headers['X-Rol'], 'ANALISTA')
    assert.equal(opciones.headers['X-Identificacion'], '1799999901')
    assert.equal(opciones.cache, 'no-store')
    return Response.json(resultado)
  }
  assert.deepEqual(await consultarCuadre('2026-10-31'), resultado)
})

test('conserva el estado y detalle de ProblemDetail', async () => {
  globalThis.fetch = async () => Response.json({ detail: 'Fecha inválida.' }, { status: 400 })
  await assert.rejects(consultarCuadre('2026-02-30'), { status: 400, message: 'Fecha inválida.' })
})

test('un fallo de red no se convierte en un cuadre vacío', async () => {
  globalThis.fetch = async () => { throw new TypeError('Failed to fetch') }
  await assert.rejects(consultarCuadre('2026-10-31'), /No se pudo conectar/)
})

test('rechaza respuestas incompletas y resultados de otra fecha', async () => {
  for (const respuesta of [null, {}, { ...resultado, fechaNegocio: '2026-10-30' },
    { ...resultado, totalDebitos: null }, { ...resultado, cuadrado: 'true' }]) {
    globalThis.fetch = async () => Response.json(respuesta)
    await assert.rejects(consultarCuadre('2026-10-31'), /respuesta incompleta o inválida/)
  }
})

test('rechaza un formato inválido antes de realizar la petición', async () => {
  globalThis.fetch = async () => assert.fail('No debe realizar la petición')
  await assert.rejects(consultarCuadre('31-10-2026'), /formato AAAA-MM-DD/)
  await assert.rejects(consultarCuadre(''), /formato AAAA-MM-DD/)
})

test('acepta la ausencia de asientos comunicada por el servidor', async () => {
  const vacio = { ...resultado, totalDebitos: 0, totalCreditos: 0, cantidadAsientos: 0,
    observacion: 'No hay asientos registrados para la fecha consultada.' }
  globalThis.fetch = async () => Response.json(vacio)
  assert.deepEqual(await consultarCuadre('2026-10-31'), vacio)
})
