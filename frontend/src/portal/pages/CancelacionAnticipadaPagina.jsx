import { useState } from 'react'
import { consultarCancelacion, registrarCancelacion } from '../services/abonoCancelacionApi.js'

const dolares = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })

function formatearFecha(iso) {
  const [anio, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${anio}`
}

const rubros = [
  { campo: 'capitalInsoluto', texto: 'Capital pendiente' },
  { campo: 'interesCuotaEnCurso', texto: 'Interés de la cuota en curso hasta hoy' },
  { campo: 'interesVencido', texto: 'Interés de cuotas vencidas' },
  { campo: 'seguroVencido', texto: 'Seguro de cuotas vencidas' },
  { campo: 'moraVencida', texto: 'Interés de mora' },
]

export default function CancelacionAnticipadaPagina() {
  const [numeroOperacion, setNumeroOperacion] = useState('')
  const [liquidacion, setLiquidacion] = useState(null)
  const [cancelada, setCancelada] = useState(null)
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

  function consultar(evento) {
    evento.preventDefault()
    setLiquidacion(null)
    setCancelada(null)
    ejecutar(async () => setLiquidacion(await consultarCancelacion(numeroOperacion.trim())))
  }

  function registrar() {
    ejecutar(async () => {
      setCancelada(await registrarCancelacion(numeroOperacion.trim(), liquidacion.valorCancelacion))
      setLiquidacion(null)
    })
  }

  return (
    <main className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div>
          <h1>Cancelación anticipada</h1>
          <p className="bq-pequeno bq-texto-2">
            Consulta el valor para cancelar el préstamo a la fecha de negocio. No tiene penalización.
          </p>
        </div>
      </div>

      <form className="bq-tarjeta bq-formulario" onSubmit={consultar}>
        <div className="bq-campo">
          <label className="bq-label" htmlFor="numeroOperacionCancelacion">
            Número de operación <span className="bq-obligatorio">*</span>
          </label>
          <input
            className="bq-input bq-mono"
            id="numeroOperacionCancelacion"
            value={numeroOperacion}
            onChange={(e) => {
              setNumeroOperacion(e.target.value)
              setLiquidacion(null)
              setCancelada(null)
            }}
            placeholder="OP-20261031-000001"
            required
          />
        </div>

        {error ? (
          <div className="bq-alerta bq-alerta-peligro" role="alert">
            <strong>No se pudo procesar la cancelación</strong>
            <p>{error}</p>
          </div>
        ) : null}

        <div className="bq-acciones">
          <button className="bq-btn bq-btn-primario" type="submit" disabled={procesando || numeroOperacion.trim() === ''}>
            {procesando && !liquidacion ? 'Consultando…' : 'Consultar valor'}
          </button>
        </div>
      </form>

      {liquidacion ? (
        <div className="bq-tarjeta">
          <span className="bq-etiqueta-seccion">
            Valor de cancelación al {formatearFecha(liquidacion.fechaCancelacion)}
          </span>
          <div className="bq-tabla-contenedor">
            <table className="bq-tabla">
              <tbody>
                {rubros.map(({ campo, texto }) => (
                  <tr key={campo}>
                    <td>{texto}</td>
                    <td className="bq-num">{dolares.format(liquidacion[campo])}</td>
                  </tr>
                ))}
                <tr>
                  <td><strong>Total a pagar</strong></td>
                  <td className="bq-num"><strong>{dolares.format(liquidacion.valorCancelacion)}</strong></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div className="bq-alerta bq-alerta-aviso" role="note">
            <strong>Antes de confirmar</strong>
            <p>
              Confirma solo cuando el cliente haya pagado {dolares.format(liquidacion.valorCancelacion)}. La operación pasará
              a estado Cancelada y no se puede deshacer.
            </p>
          </div>
          <div className="bq-acciones">
            <button className="bq-btn bq-btn-secundario" type="button" onClick={() => setLiquidacion(null)} disabled={procesando}>
              Volver
            </button>
            <button className="bq-btn bq-btn-peligro" type="button" onClick={registrar} disabled={procesando}>
              {procesando ? 'Registrando…' : 'Confirmar pago y cancelar'}
            </button>
          </div>
        </div>
      ) : null}

      {cancelada ? (
        <div className="bq-alerta bq-alerta-exito" role="status">
          <strong>Operación cancelada</strong>
          <p>
            La operación <span className="bq-mono">{cancelada.numeroOperacion}</span> quedó cancelada con un pago de{' '}
            {dolares.format(cancelada.valorCancelacion)}. Ya no genera intereses.
          </p>
        </div>
      ) : null}
    </main>
  )
}
