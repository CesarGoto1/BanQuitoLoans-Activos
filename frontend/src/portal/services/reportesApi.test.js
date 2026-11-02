import { test, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import { consultarReporte } from './reportesApi.js'

const originalFetch = globalThis.fetch
const originalSession = globalThis.sessionStorage
afterEach(() => { globalThis.fetch = originalFetch; globalThis.sessionStorage = originalSession })
const fecha = '2026-10-31'

for (const tipo of ['morosidad', 'composicion', 'provisiones', 'salud', 'roll-rate']) {
  test(`consulta ${tipo} con fechas e identificación del analista`, async () => {
    globalThis.sessionStorage = { getItem: () => JSON.stringify({ cedula: '1712345675' }) }
    const cuerpo = tipo === 'roll-rate' ? { fechaInicial: fecha, fechaFinal: '2026-11-01', filas: [] }
      : tipo === 'salud' ? { morosidad: { fechaCorte: fecha }, concentracion: { mayoresClientes: [] }, porMontoOriginal: [], porPlazo: [], porSistemaAmortizacion: [] }
        : { fechaCorte: fecha, tramos: [] }
    globalThis.fetch = async (url, opciones) => {
      assert.ok(url.startsWith(`/api/analista/reportes/${tipo}?`))
      assert.ok(url.includes(tipo === 'roll-rate' ? 'fechaInicial=2026-10-31&fechaFinal=2026-11-01' : 'fechaCorte=2026-10-31'))
      assert.equal(opciones.headers['X-Identificacion'], '1712345675')
      assert.equal(opciones.headers['X-Rol'], 'ANALISTA')
      assert.equal(opciones.cache, 'no-store')
      return { ok: true, json: async () => cuerpo }
    }
    assert.deepEqual(await consultarReporte(tipo, fecha, '2026-11-01'), cuerpo)
  })
}

test('rechaza fechas inválidas y rangos invertidos antes de HTTP', async () => {
  globalThis.fetch = () => { throw new Error('No debe consultar') }
  for (const valor of ['', '2026-02-30', '2026-13-01']) {
    await assert.rejects(consultarReporte('morosidad', valor), /fecha válida/)
  }
  await assert.rejects(consultarReporte('roll-rate', fecha, fecha), /anterior/)
  await assert.rejects(consultarReporte('otro', fecha), /no existe/)
})

test('presenta el motivo de un corte no confirmado', async () => {
  globalThis.sessionStorage = { getItem: () => null }
  globalThis.fetch = async () => ({ ok: false, status: 409, json: async () => ({ detail: 'Fotografía no confirmada' }) })
  await assert.rejects(consultarReporte('roll-rate', fecha, '2026-11-01'), e => e.status === 409 && e.message === 'Fotografía no confirmada')
})

test('rechaza respuesta correspondiente a otra fecha', async () => {
  globalThis.sessionStorage = { getItem: () => null }
  globalThis.fetch = async () => ({ ok: true, json: async () => ({ fechaCorte: '2026-10-30' }) })
  await assert.rejects(consultarReporte('morosidad', fecha), /incompleto o inválido/)
})

test('presenta errores de conexión', async () => {
  globalThis.sessionStorage = { getItem: () => null }
  globalThis.fetch = async () => { throw new Error('red') }
  await assert.rejects(consultarReporte('salud', fecha), /conectar/)
})
