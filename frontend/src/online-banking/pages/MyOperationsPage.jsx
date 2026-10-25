import { useEffect, useState } from 'react'
import { clientApi } from '../services/clientApi'
import OperationDetailsPage from './OperationDetailsPage'

const formatCurrency = (valor) => valor == null ? '—' : new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' }).format(valor)

export default function MyOperationsPage() {
  const [operaciones, setOperaciones] = useState([])
  const [seleccionada, setSeleccionada] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    clientApi.fetchOperations().then(setOperaciones).catch((e) => setError(e.message)).finally(() => setCargando(false))
  }, [])

  if (seleccionada) return <OperationDetailsPage operacion={seleccionada} goBack={() => setSeleccionada(null)} />

  return <section className="bq-pagina">
    <div className="bq-titulo-pagina"><div><h1>Mis operaciones</h1><p>Revisa tus préstamos, saldos y estado de cartera.</p></div></div>
    {error && <div className="bq-alerta bq-alerta-peligro" role="alert"><strong>No se pudo cargar la información.</strong><span>{error}</span></div>}
    {cargando ? <div className="bq-tarjeta"><p className="bq-texto-2">Cargando tus operaciones…</p></div> : operaciones.length === 0 ? (
      <div className="bq-tarjeta bq-vacio"><h2>No tienes operaciones vigentes</h2><p className="bq-texto-2">Las operaciones desembolsadas aparecerán aquí.</p></div>
    ) : <div className="bq-rejilla">{operaciones.map((o) => <article className="bq-tarjeta" key={o.numeroOperacion}>
      <div className="bq-encabezado-tarjeta"><div><span className="bq-etiqueta-seccion">Operación</span><h2>{o.numeroOperacion}</h2></div><span className={`bq-etiqueta ${o.estadoCartera === 'POR_VENCER' ? 'bq-exito' : 'bq-aviso'}`}>{o.estadoCartera.replaceAll('_', ' ')}</span></div>
      <div className="bq-cifras"><div className="bq-cifra"><span>Saldo de capital</span><strong>{formatCurrency(o.saldoCapital)}</strong></div><div className="bq-cifra"><span>Monto aprobado</span><strong>{formatCurrency(o.montoAprobado)}</strong></div></div>
      <div className="bq-datos"><div className="bq-dato"><span>Plazo</span><span>{o.plazoMeses} meses</span></div><div className="bq-dato"><span>Días de mora</span><span>{o.diasMora}</span></div></div>
      <button className="bq-btn bq-btn-secundario" onClick={() => setSeleccionada(o)}>Ver tabla y pagos</button>
    </article>)}</div>}
  </section>
}





