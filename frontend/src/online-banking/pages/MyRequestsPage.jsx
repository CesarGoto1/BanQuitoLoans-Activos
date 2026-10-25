import { useEffect, useState } from 'react'
import { clientApi } from '../services/clientApi'

const estados = {
  REGISTRADA: ['Registrada', 'bq-info'],
  APROBADA: ['Aprobada', 'bq-exito'],
  RECHAZADA: ['Rechazada', 'bq-peligro'],
}

const formatCurrency = (valor) =>
  valor == null
    ? '—'
    : new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' }).format(valor)

const formatDate = (valor) => (valor ? new Date(`${valor}T00:00:00`).toLocaleDateString('es-EC') : '—')

export default function MyRequestsPage() {
  const [solicitudes, setSolicitudes] = useState([])
  const [estado, setEstado] = useState('')
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let vigente = true
    setCargando(true)
    setError('')
    clientApi.fetchRequests(estado)
      .then((datos) => vigente && setSolicitudes(datos))
      .catch((e) => vigente && setError(e.message))
      .finally(() => vigente && setCargando(false))
    return () => { vigente = false }
  }, [estado])

  return (
    <section className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div><h1>Mis solicitudes</h1><p>Consulta el estado de tus solicitudes de préstamo.</p></div>
        <label className="bq-campo">
          <span>Filtrar por estado</span>
          <select value={estado} onChange={(e) => setEstado(e.target.value)}>
            <option value="">Todos</option>
            <option value="REGISTRADA">Registradas</option>
            <option value="APROBADA">Aprobadas</option>
            <option value="RECHAZADA">Rechazadas</option>
          </select>
        </label>
      </div>
      {error && <div className="bq-alerta bq-alerta-peligro" role="alert"><strong>No se pudo cargar la información.</strong><span>{error}</span></div>}
      <div className="bq-tarjeta">
        {cargando ? <p className="bq-texto-2">Cargando tus solicitudes…</p> : solicitudes.length === 0 ? (
          <div className="bq-vacio"><h2>No tienes solicitudes</h2><p className="bq-texto-2">Cuando registres una solicitud aparecerá aquí.</p></div>
        ) : (
          <div className="bq-tabla-contenedor">
            <table className="bq-tabla">
              <thead><tr><th>Solicitud</th><th>Fecha</th><th>Destino</th><th className="bq-num">Monto</th><th>Plazo</th><th>Estado</th><th>Motivo</th></tr></thead>
              <tbody>{solicitudes.map((s) => {
                const [texto, clase] = estados[s.estadoSolicitud] || [s.estadoSolicitud, 'bq-info']
                return <tr key={s.idSolicitud}>
                  <td className="bq-mono">#{s.idSolicitud}</td><td>{formatDate(s.formatDateSolicitud)}</td>
                  <td>{s.destinoCredito.replaceAll('_', ' ')}</td><td className="bq-num">{formatCurrency(s.montoSolicitado)}</td>
                  <td>{s.plazoMeses} meses</td><td><span className={`bq-etiqueta ${clase}`}>{texto}</span></td>
                  <td>{s.motivoRechazo || '—'}</td>
                </tr>
              })}</tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  )
}






