import { useState } from 'react'
import { consultarCuadre } from '../services/cuadreContableApi.js'

const dolares = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })

function formatearFecha(fecha) {
  const [anio, mes, dia] = fecha.split('-')
  return `${dia}/${mes}/${anio}`
}

export default function CuadreContablePagina() {
  const [fecha, setFecha] = useState('')
  const [resultado, setResultado] = useState(null)
  const [consultando, setConsultando] = useState(false)
  const [error, setError] = useState('')

  async function consultar(event) {
    event.preventDefault()
    if (consultando) return
    setResultado(null)
    setError('')
    setConsultando(true)
    try {
      setResultado(await consultarCuadre(fecha))
    } catch (e) {
      setError(e.message)
    } finally {
      setConsultando(false)
    }
  }

  function cambiarFecha(event) {
    setFecha(event.target.value)
    setResultado(null)
    setError('')
  }

  return (
    <main className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div>
          <h1>Cuadre contable</h1>
          <p className="bq-pequeno bq-texto-2">
            Consulta los débitos, créditos y la diferencia de una fecha de negocio.
          </p>
        </div>
      </div>
      <form className="bq-tarjeta bq-formulario" onSubmit={consultar}>
        <div className="bq-fila">
          <div className="bq-campo">
            <label className="bq-label" htmlFor="fechaCuadre">Fecha de negocio</label>
            <input
              id="fechaCuadre"
              className="bq-input"
              type="date"
              required
              value={fecha}
              disabled={consultando}
              onChange={cambiarFecha}
            />
          </div>
        </div>
        <div className="bq-acciones">
          <button className="bq-btn bq-btn-primario" type="submit" disabled={consultando || !fecha}>
            {consultando ? 'Consultando…' : 'Consultar cuadre'}
          </button>
        </div>
      </form>
      {error ? (
        <div className="bq-alerta bq-alerta-peligro" role="alert">
          <strong>No se pudo consultar el cuadre</strong>
          <p>{error}</p>
        </div>
      ) : null}
      {consultando ? <p className="bq-texto-2" role="status">Consultando los asientos contables…</p> : null}
      {resultado ? (
        <section className="bq-tarjeta" aria-labelledby="resultadoCuadre" aria-live="polite">
          <h2 id="resultadoCuadre">Resultado del {formatearFecha(resultado.fechaNegocio)}</h2>
          <div className={`bq-alerta ${resultado.cantidadAsientos === 0 ? 'bq-alerta-aviso' : resultado.cuadrado ? 'bq-alerta-info' : 'bq-alerta-peligro'}`}>
            <strong>
              {resultado.cantidadAsientos === 0 ? 'Sin asientos registrados' : resultado.cuadrado ? 'Cuadrado aritméticamente' : 'Descuadre contable'}
            </strong>
            <p>{resultado.observacion}</p>
          </div>
          <div className="bq-tabla-contenedor">
            <table className="bq-tabla">
              <thead>
                <tr><th>Concepto</th><th className="bq-num">Valor</th></tr>
              </thead>
              <tbody>
                <tr><th scope="row">Total de débitos</th><td className="bq-num">{dolares.format(resultado.totalDebitos)}</td></tr>
                <tr><th scope="row">Total de créditos</th><td className="bq-num">{dolares.format(resultado.totalCreditos)}</td></tr>
                <tr><th scope="row">Diferencia</th><td className="bq-num">{dolares.format(resultado.diferencia)}</td></tr>
                <tr><th scope="row">Cantidad de asientos</th><td className="bq-num">{resultado.cantidadAsientos}</td></tr>
              </tbody>
            </table>
          </div>
        </section>
      ) : null}
    </main>
  )
}
