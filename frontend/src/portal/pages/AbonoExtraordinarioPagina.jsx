import { useState } from 'react'
import { registrarAbono, simularAbono } from '../services/abonoCancelacionApi.js'

const dolares = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })
const numero = new Intl.NumberFormat('es-EC', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const modalidades = {
  DISMINUIR_CUOTA: { texto: 'Disminuir la cuota', ayuda: 'Mantiene la fecha final y baja el valor de las cuotas.' },
  DISMINUIR_PLAZO: { texto: 'Disminuir el plazo', ayuda: 'Mantiene la cuota y elimina cuotas del final.' },
}

function formatearFecha(iso) {
  const [anio, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${anio}`
}

export default function AbonoExtraordinarioPagina() {
  const [numeroOperacion, setNumeroOperacion] = useState('')
  const [monto, setMonto] = useState('')
  const [modalidad, setModalidad] = useState('DISMINUIR_CUOTA')
  const [simulacion, setSimulacion] = useState(null)
  const [registrado, setRegistrado] = useState(null)
  const [error, setError] = useState('')
  const [procesando, setProcesando] = useState(false)

  async function ejecutar(accion) {
    setProcesando(true)
    setError('')
    try {
      await accion()
    } catch (e) {
      setError(e.message)
    } finally {
      setProcesando(false)
    }
  }

  function limpiarResultado() {
    setSimulacion(null)
    setRegistrado(null)
    setError('')
  }

  function calcular(evento) {
    evento.preventDefault()
    limpiarResultado()
    if (!/^\d+(\.\d{1,2})?$/.test(monto.trim())) {
      setError('Escribe el valor con máximo dos decimales, por ejemplo 2000.00.')
      return
    }
    ejecutar(async () => setSimulacion(await simularAbono(numeroOperacion.trim(), monto.trim(), modalidad)))
  }

  function registrar() {
    ejecutar(async () => {
      setRegistrado(await registrarAbono(numeroOperacion.trim(), monto.trim(), modalidad))
      setSimulacion(null)
    })
  }

  const resultado = registrado ?? simulacion

  return (
    <main className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div>
          <h1>Abono extraordinario</h1>
          <p className="bq-pequeno bq-texto-2">
            Registra un pago adicional a capital. Primero calcula el cuadro y, si el cliente está de acuerdo, regístralo.
          </p>
        </div>
      </div>

      <form className="bq-tarjeta bq-formulario" onSubmit={calcular}>
        <div className="bq-fila">
          <div className="bq-campo">
            <label className="bq-label" htmlFor="numeroOperacion">
              Número de operación <span className="bq-obligatorio">*</span>
            </label>
            <input
              className="bq-input bq-mono"
              id="numeroOperacion"
              value={numeroOperacion}
              onChange={(e) => {
                setNumeroOperacion(e.target.value)
                limpiarResultado()
              }}
              placeholder="OP-20261031-000001"
              required
            />
          </div>
          <div className="bq-campo">
            <label className="bq-label" htmlFor="monto">
              Valor del abono (USD) <span className="bq-obligatorio">*</span>
            </label>
            <input
              className="bq-input bq-num"
              id="monto"
              value={monto}
              onChange={(e) => {
                setMonto(e.target.value)
                limpiarResultado()
              }}
              inputMode="decimal"
              placeholder="2000.00"
              required
            />
          </div>
        </div>

        <div className="bq-campo">
          <span className="bq-label">Modalidad elegida por el cliente</span>
          <div className="bq-segmentado" role="group" aria-label="Modalidad del abono">
            {Object.entries(modalidades).map(([clave, { texto }]) => (
              <button
                key={clave}
                type="button"
                aria-pressed={modalidad === clave}
                onClick={() => {
                  setModalidad(clave)
                  limpiarResultado()
                }}
              >
                {texto}
              </button>
            ))}
          </div>
          <span className="bq-ayuda">{modalidades[modalidad].ayuda}</span>
        </div>

        {error ? (
          <div className="bq-alerta bq-alerta-peligro" role="alert">
            <strong>No se pudo procesar el abono</strong>
            <p>{error}</p>
          </div>
        ) : null}

        <div className="bq-acciones">
          <button
            className="bq-btn bq-btn-primario"
            type="submit"
            disabled={procesando || numeroOperacion.trim() === '' || monto.trim() === ''}
          >
            {procesando && !simulacion ? 'Calculando…' : 'Calcular cuadro'}
          </button>
        </div>
      </form>

      {registrado ? (
        <div className="bq-alerta bq-alerta-exito" role="status">
          <strong>Abono registrado</strong>
          <p>
            Se aplicaron {dolares.format(registrado.capitalAbonado)} a capital de la operación{' '}
            <span className="bq-mono">{registrado.numeroOperacion}</span>. La tabla de amortización ya está actualizada.
          </p>
        </div>
      ) : null}

      {resultado ? (
        <>
          <div className="bq-tarjeta">
            <span className="bq-etiqueta-seccion">
              Liquidación al {formatearFecha(resultado.fechaAbono)} · {modalidades[resultado.modalidad].texto}
            </span>
            <div className="bq-fila">
              <Dato etiqueta="Valor del abono" valor={dolares.format(resultado.montoAbono)} />
              <Dato etiqueta="Interés devengado liquidado" valor={dolares.format(resultado.interesLiquidado)} />
              <Dato etiqueta="Aplicado a capital" valor={dolares.format(resultado.capitalAbonado)} />
              <Dato etiqueta="Saldo anterior" valor={dolares.format(resultado.saldoAnterior)} />
              <Dato etiqueta="Saldo nuevo" valor={dolares.format(resultado.saldoNuevo)} destacado />
            </div>
          </div>

          <ComparacionCuadros antes={resultado.antes} despues={resultado.despues} />

          {simulacion ? (
            <div className="bq-acciones">
              <button className="bq-btn bq-btn-secundario" type="button" onClick={limpiarResultado} disabled={procesando}>
                Cancelar
              </button>
              <button className="bq-btn bq-btn-primario" type="button" onClick={registrar} disabled={procesando}>
                {procesando ? 'Registrando…' : 'Registrar abono'}
              </button>
            </div>
          ) : null}
        </>
      ) : null}
    </main>
  )
}

function ComparacionCuadros({ antes, despues }) {
  return (
    <div className="bq-tarjeta">
      <span className="bq-etiqueta-seccion">Cuadro comparativo de las cuotas pendientes</span>
      <div className="bq-tabla-contenedor">
        <table className="bq-tabla">
          <thead>
            <tr>
              <th></th>
              <th className="bq-num">Antes</th>
              <th className="bq-num">Después</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Cuotas pendientes</td>
              <td className="bq-num">{antes.numeroCuotas}</td>
              <td className="bq-num">{despues.numeroCuotas}</td>
            </tr>
            <tr>
              <td>Valor de la cuota</td>
              <td className="bq-num">{numero.format(antes.valorCuota)}</td>
              <td className="bq-num">{numero.format(despues.valorCuota)}</td>
            </tr>
            <tr>
              <td>Intereses por pagar</td>
              <td className="bq-num">{numero.format(antes.totalInteres)}</td>
              <td className="bq-num">{numero.format(despues.totalInteres)}</td>
            </tr>
            <tr>
              <td>Fecha final</td>
              <td className="bq-num">{formatearFecha(antes.fechaFinal)}</td>
              <td className="bq-num">{formatearFecha(despues.fechaFinal)}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <span className="bq-etiqueta-seccion">Tabla antes del abono</span>
      <TablaCuotas cuotas={antes.cuotas} />
      <span className="bq-etiqueta-seccion">Tabla después del abono</span>
      <TablaCuotas cuotas={despues.cuotas} />
    </div>
  )
}

function TablaCuotas({ cuotas }) {
  return (
    <div className="bq-tabla-contenedor">
      <table className="bq-tabla">
        <thead>
          <tr>
            <th className="bq-num">N.º</th>
            <th>Vencimiento</th>
            <th className="bq-num">Capital</th>
            <th className="bq-num">Interés</th>
            <th className="bq-num">Seguro</th>
            <th className="bq-num">Cuota total</th>
            <th className="bq-num">Saldo</th>
          </tr>
        </thead>
        <tbody>
          {cuotas.map((cuota) => (
            <tr key={cuota.numeroCuota}>
              <td className="bq-num">{cuota.numeroCuota}</td>
              <td>{formatearFecha(cuota.fechaVencimiento)}</td>
              <td className="bq-num">{numero.format(cuota.valorCapital)}</td>
              <td className="bq-num">{numero.format(cuota.valorInteres)}</td>
              <td className="bq-num">{numero.format(cuota.valorSeguro)}</td>
              <td className="bq-num">{numero.format(cuota.valorCuota)}</td>
              <td className="bq-num">{numero.format(cuota.saldoCapitalPendiente)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function Dato({ etiqueta, valor, destacado = false }) {
  return (
    <div className="bq-campo">
      <span className="bq-label">{etiqueta}</span>
      {destacado ? <strong className="bq-monto">{valor}</strong> : <span className="bq-monto">{valor}</span>}
    </div>
  )
}
