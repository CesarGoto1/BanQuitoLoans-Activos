import { useEffect, useState } from 'react'
import { clientApi } from '../services/clientApi'

const formatCurrency = (valor) => valor == null ? '—' : new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' }).format(valor)
const formatDate = (valor) => valor ? new Date(`${valor}T00:00:00`).toLocaleDateString('es-EC') : '—'

export default function OperationDetailsPage({ operacion, goBack }) {
  const [cuotas, setCuotas] = useState([])
  const [error, setError] = useState('')
  useEffect(() => { clientApi.fetchInstallments(operacion.numeroOperacion).then(setCuotas).catch((e) => setError(e.message)) }, [operacion.numeroOperacion])
  const proxima = cuotas.find((c) => c.estadoCuota !== 'PAGADA')

  return <section className="bq-pagina">
    <button className="bq-btn bq-btn-texto" onClick={goBack}>← Volver a mis operaciones</button>
    <div className="bq-titulo-pagina"><div><h1>Detalle de operación</h1><p className="bq-mono">{operacion.numeroOperacion}</p></div></div>
    {error && <div className="bq-alerta bq-alerta-peligro" role="alert"><strong>No se pudo cargar la tabla.</strong><span>{error}</span></div>}
    <div className="bq-rejilla"><div className="bq-tarjeta bq-tarjeta-destacada"><span className="bq-etiqueta-seccion">Saldo de capital</span><strong className="bq-monto-grande">{formatCurrency(operacion.saldoCapital)}</strong><p>{operacion.estadoCartera.replaceAll('_', ' ')}</p></div><div className="bq-tarjeta"><span className="bq-etiqueta-seccion">Próximo pago</span><strong className="bq-monto-grande">{proxima ? formatCurrency(proxima.totalCuota) : 'Al día'}</strong><p className="bq-texto-2">{proxima ? `Cuota ${proxima.numeroCuota} · vence el ${formatDate(proxima.fechaVencimiento)}` : 'No tienes cuotas pendientes.'}</p></div></div>
    <div className="bq-tarjeta"><div className="bq-encabezado-tarjeta"><h2>Tabla de amortización y pagos</h2><span className="bq-texto-2">{cuotas.length} cuotas</span></div><div className="bq-tabla-contenedor"><table className="bq-tabla"><thead><tr><th>Cuota</th><th>Vencimiento</th><th className="bq-num">Capital</th><th className="bq-num">Interés</th><th className="bq-num">Seguro</th><th className="bq-num">Total</th><th>Estado</th><th>Pago</th></tr></thead><tbody>{cuotas.map((c) => <tr key={c.numeroCuota}><td>{c.numeroCuota}</td><td>{formatDate(c.fechaVencimiento)}</td><td className="bq-num">{formatCurrency(c.valorCapital)}</td><td className="bq-num">{formatCurrency(c.valorInteres)}</td><td className="bq-num">{formatCurrency(c.valorSeguro)}</td><td className="bq-num"><strong>{formatCurrency(c.totalCuota)}</strong></td><td>{c.estadoCuota.replaceAll('_', ' ')}</td><td>{c.fechaPago ? formatDate(c.fechaPago) : '—'}</td></tr>)}</tbody></table></div></div>
  </section>
}





