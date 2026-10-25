import { afterEach, beforeEach, test } from 'node:test';
import assert from 'node:assert/strict';
import { approveLoanApplication, rejectLoanApplication, obtenerBandejaSolicitudes,
  obtenerDetalleSolicitud, setAnalystIdentification } from './prestamosApi.js';

const originalFetch = globalThis.fetch;

beforeEach(() => {
  const values = new Map();
  globalThis.sessionStorage = {
    getItem: (key) => values.get(key) || null,
    setItem: (key, value) => values.set(key, value),
  };
  setAnalystIdentification('1799999901');
});

afterEach(() => {
  globalThis.fetch = originalFetch;
  delete globalThis.sessionStorage;
});

test('approval sends the established body and session headers', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/analista/solicitudes/7/aprobacion');
    assert.equal(options.method, 'POST');
    assert.deepEqual(JSON.parse(options.body), { teaAsignada: 0.165 });
    assert.equal(options.headers['X-Rol'], 'ANALISTA');
    assert.equal(options.headers['X-Identificacion'], '1799999901');
    return Response.json({ idSolicitud: 7, estadoSolicitud: 'APROBADA' });
  };
  assert.equal((await approveLoanApplication(7, 0.165)).estadoSolicitud, 'APROBADA');
});

test('rejection sends a reason without an analyst id in the body', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/analista/solicitudes/7/rechazo');
    assert.deepEqual(JSON.parse(options.body), { motivoRechazo: 'Capacidad insuficiente' });
    return Response.json({ estadoSolicitud: 'RECHAZADA' });
  };
  assert.equal((await rejectLoanApplication(7, 'Capacidad insuficiente')).estadoSolicitud, 'RECHAZADA');
});

test('conflicts retain status and Spanish ProblemDetail message', async () => {
  globalThis.fetch = async () => Response.json({ status: 409, title: 'Conflicto', detail: 'La solicitud ya fue resuelta.' }, { status: 409 });
  await assert.rejects(approveLoanApplication(7, 0.165), { status: 409, message: 'La solicitud ya fue resuelta.' });
});

test('network failures never return fictitious applications', async () => {
  globalThis.fetch = async () => { throw new TypeError('Failed to fetch'); };
  await assert.rejects(obtenerBandejaSolicitudes(), /No se pudo conectar/);
  await assert.rejects(obtenerDetalleSolicitud(7), /No se pudo conectar/);
  await assert.rejects(approveLoanApplication(7, 0.165), /comprobar si la decisión se guardó/);
});

test('invalid success payload cannot be mistaken for a saved decision', async () => {
  globalThis.fetch = async () => new Response('not json', { status: 200 });
  await assert.rejects(rejectLoanApplication(7, 'Motivo'), /respuesta incompleta/);
});

test('inbox queries registered applications without caching', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/analista/solicitudes?estado=REGISTRADA');
    assert.equal(options.cache, 'no-store');
    return Response.json([]);
  };
  assert.deepEqual(await obtenerBandejaSolicitudes(), []);
});
