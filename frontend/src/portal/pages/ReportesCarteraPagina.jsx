import { useState } from 'react'
import { consultarReporte } from '../services/reportesApi.js'

const dolares = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })
const numero = new Intl.NumberFormat('es-EC', { maximumFractionDigits: 2 })
const dinero = valor => valor == null ? 'No disponible' : dolares.format(valor)
const porcentaje = valor => valor == null ? 'No calculable' : `${numero.format(valor)} %`
const estado = valor => valor.replaceAll('_', ' ')

function Tabla({ titulo, columnas, filas }) {
  return <section className="bq-tarjeta">
    <h2>{titulo}</h2>
    {filas.length === 0 ? <p>Sin registros para la fecha consultada.</p> : <div className="bq-tabla-contenedor">
      <table className="bq-tabla"><caption>{titulo}</caption>
        <thead><tr>{columnas.map(columna => <th key={columna} scope="col">{columna}</th>)}</tr></thead>
        <tbody>{filas.map((fila, i) => <tr key={i}>{fila.map((valor, j) => <td key={j}>{valor}</td>)}</tr>)}</tbody>
      </table>
    </div>}
  </section>
}

function Morosidad({ datos }) {
  return <Tabla titulo="Morosidad de cartera" columnas={['Concepto', 'Valor']} filas={[
    ['Cartera por vencer', dinero(datos.saldoPorVencer)], ['Cartera vencida', dinero(datos.saldoVencido)],
    ['Cartera en litigio', dinero(datos.saldoEnLitigio)], ['Cartera bruta', dinero(datos.carteraBruta)],
    ['Cartera castigada', dinero(datos.saldoCastigado)], ['Índice de morosidad', porcentaje(datos.indiceMorosidad)],
  ]} />
}

function Composicion({ datos }) {
  return <>
    {datos.cantidadOperaciones === 0 ? <p className="bq-alerta bq-alerta-aviso">No hay operaciones en las fotografías consultadas. Esto no confirma que se haya generado un corte vacío.</p> : null}
    <Tabla titulo="Saldos y provisiones por tramo" columnas={['Tramo', 'Estado', 'Operaciones', 'Capital', 'Participación', 'Provisión constituida']}
      filas={datos.tramos.map(d => [d.tramo.numeroTramo, estado(d.tramo.estadoCartera), d.cantidadOperaciones,
        dinero(d.saldoCapital), porcentaje(d.participacion), dinero(d.provisionConstituida)])} />
    <Tabla titulo="Totales" columnas={['Operaciones', 'Capital', 'Provisión constituida']}
      filas={[[datos.cantidadOperaciones, dinero(datos.saldoCapital), dinero(datos.provisionConstituida)]]} />
  </>
}

function Salud({ datos }) {
  return <>
    <Morosidad datos={datos.morosidad} />
    <Tabla titulo="Salud y concentración" columnas={['Indicador', 'Valor']} filas={[
      ['Provisiones de cartera bruta', dinero(datos.provisionesCarteraBruta)],
      ['Cobertura de provisiones', porcentaje(datos.coberturaProvisiones)],
      ['Clientes de cartera bruta', datos.concentracion.cantidadClientes],
      ['Exposición de los diez mayores', dinero(datos.concentracion.saldoMayoresClientes)],
      ['Concentración de los diez mayores', porcentaje(datos.concentracion.concentracionDiezMayores)],
    ]} />
    <Tabla titulo="Clientes con mayor exposición" columnas={['Identificador de cliente', 'Operaciones', 'Capital', 'Participación']}
      filas={datos.concentracion.mayoresClientes.map(c => [c.idCliente, c.cantidadOperaciones, dinero(c.saldoCapital), porcentaje(c.participacionCarteraBruta)])} />
    {[['Monto original aprobado', datos.porMontoOriginal], ['Plazo original', datos.porPlazo], ['Sistema de amortización', datos.porSistemaAmortizacion]].map(([titulo, categorias]) =>
      <Tabla key={titulo} titulo={titulo} columnas={['Categoría', 'Operaciones', 'Capital', 'Participación sobre cartera bruta']}
        filas={categorias.map(c => [c.categoria, c.cantidadOperaciones, dinero(c.saldoCapital), porcentaje(c.participacion)])} />)}
  </>
}

function RollRate({ datos }) {
  const destinos = datos.filas[0]?.transiciones ?? []
  return <>
    <p>Las transiciones se expresan con el saldo inicial. Las operaciones nuevas del corte final no forman parte de esta matriz.</p>
    <Tabla titulo="Matriz Roll-Rate" columnas={['Tramo inicial', 'Operaciones iniciales', 'Capital inicial', ...destinos.map(d => estado(d.destino))]}
      filas={datos.filas.map(f => [`Tramo ${f.tramoInicial.numeroTramo}`, f.cantidadOperaciones, dinero(f.saldoInicial),
        ...f.transiciones.map(t => <span>{dinero(t.saldoInicial)} · {porcentaje(t.porcentaje)} · {t.cantidadOperaciones} operaciones</span>)])} />
    <Tabla titulo="Totales iniciales" columnas={['Operaciones', 'Capital']} filas={[[datos.cantidadOperacionesIniciales, dinero(datos.saldoInicial)]]} />
  </>
}

function ReportesCarteraPagina({ tipo, titulo }) {
  const [fecha, setFecha] = useState('')
  const [fin, setFin] = useState('')
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)
  const rollRate = tipo === 'roll-rate'

  function cambiar(valor, guardar) {
    guardar(valor)
    setResultado(null)
    setError('')
  }

  async function consultar(event) {
    event.preventDefault()
    if (cargando) return
    setResultado(null)
    setError('')
    setCargando(true)
    try {
      setResultado(await consultarReporte(tipo, fecha, fin))
    } catch (e) {
      setError(e.message)
    } finally {
      setCargando(false)
    }
  }

  return <main className="bq-pagina">
    <div className="bq-titulo-pagina"><div><h1>{titulo}</h1><p className="bq-pequeno bq-texto-2">Consulta la cartera registrada en las fotografías oficiales.</p></div></div>
    <form className="bq-tarjeta bq-formulario" onSubmit={consultar}>
      <div className="bq-fila">
        <div className="bq-campo"><label className="bq-label" htmlFor="fechaReporte">{rollRate ? 'Fecha inicial' : 'Fecha de corte'}</label>
          <input className="bq-input" id="fechaReporte" type="date" required value={fecha} disabled={cargando} onChange={e => cambiar(e.target.value, setFecha)} /></div>
        {rollRate ? <div className="bq-campo"><label className="bq-label" htmlFor="fechaFinalReporte">Fecha final</label>
          <input className="bq-input" id="fechaFinalReporte" type="date" required value={fin} disabled={cargando} onChange={e => cambiar(e.target.value, setFin)} /></div> : null}
      </div>
      <div className="bq-acciones"><button className="bq-btn bq-btn-primario" disabled={cargando || !fecha || (rollRate && !fin)}>{cargando ? 'Consultando…' : 'Consultar reporte'}</button></div>
    </form>
    {error ? <div className="bq-alerta bq-alerta-peligro" role="alert"><strong>No se pudo consultar el reporte</strong><p>{error}</p></div> : null}
    {cargando ? <p role="status">Consultando la cartera…</p> : null}
    {resultado ? <div aria-live="polite"><p>Fecha consultada: {fecha}{rollRate ? ` a ${fin}` : ''}</p>
      {tipo === 'morosidad' ? <Morosidad datos={resultado} /> : tipo === 'salud' ? <Salud datos={resultado} /> : rollRate ? <RollRate datos={resultado} /> : <Composicion datos={resultado} />}
    </div> : null}
  </main>
}

export function MorosidadPagina() { return <ReportesCarteraPagina tipo="morosidad" titulo="Morosidad de cartera" /> }
export function ComposicionCarteraPagina() { return <ReportesCarteraPagina tipo="composicion" titulo="Composición de cartera" /> }
export function ProvisionesPagina() { return <ReportesCarteraPagina tipo="provisiones" titulo="Provisiones constituidas" /> }
export function SaludCarteraPagina() { return <ReportesCarteraPagina tipo="salud" titulo="Salud de cartera" /> }
export function RollRatePagina() { return <ReportesCarteraPagina tipo="roll-rate" titulo="Roll-Rate" /> }
