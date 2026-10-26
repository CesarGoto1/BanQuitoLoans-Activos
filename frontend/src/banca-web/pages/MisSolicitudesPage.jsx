import { useEffect, useState } from 'react'
import { findApplications } from '../services/solicitudesCreditoApi.js'

const dollars = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })

const statuses = {
  REGISTERED: { label: 'Registrada', className: 'bq-estado-info' },
  APPROVED: { label: 'Aprobada', className: 'bq-estado-exito' },
  REJECTED: { label: 'Rechazada', className: 'bq-estado-peligro' },
}

const purposes = {
  DURABLE_GOODS: 'Bienes duraderos',
  HEALTH: 'Salud',
  EDUCATION: 'Educación',
  TRAVEL: 'Viaje',
  OTHER: 'Otro',
}

const amortizationMethods = { FRENCH: 'Francés', GERMAN: 'Alemán' }

function formatDate(iso) {
  const [year, month, day] = iso.split('-')
  return `${day}/${month}/${year}`
}

export default function MisSolicitudesPage({ customer }) {
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [applications, setApplications] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let current = true
    setError('')
    findApplications(status, page)
      .then((data) => current && setApplications(data))
      .catch((e) => current && setError(e.message))
    return () => {
      current = false
    }
  }, [customer.idCliente, status, page])

  function changeStatus(value) {
    setStatus(value)
    setPage(0)
  }

  return (
    <main className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div>
          <h1>Mis solicitudes</h1>
          <p className="bq-pequeno bq-texto-2">Consulta el estado de tus solicitudes de crédito y el motivo si fueron rechazadas.</p>
        </div>
        <div className="bq-campo">
          <label className="bq-label" htmlFor="status">Estado</label>
          <select className="bq-select" id="status" value={status} onChange={(e) => changeStatus(e.target.value)}>
            <option value="">Todos</option>
            {Object.entries(statuses).map(([value, { label }]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </div>
      </div>

      {error ? (
        <div className="bq-alerta bq-alerta-peligro" role="alert">
          <strong>No se pudo cargar la información</strong>
          <span>{error}</span>
        </div>
      ) : null}

      {!applications && !error ? <p className="bq-texto-2">Cargando tus solicitudes…</p> : null}

      {applications && applications.content.length === 0 ? (
        <div className="bq-alerta bq-alerta-info" role="status">
          <strong>No tienes solicitudes</strong>
          <span>Cuando registres una solicitud aparecerá aquí.</span>
        </div>
      ) : null}

      {applications && applications.content.length > 0 ? (
        <div className="bq-tarjeta">
          <div className="bq-tabla-contenedor">
            <table className="bq-tabla">
              <thead>
                <tr>
                  <th className="bq-num">Solicitud</th>
                  <th>Fecha</th>
                  <th>Destino</th>
                  <th className="bq-num">Monto</th>
                  <th className="bq-num">Plazo</th>
                  <th>Sistema</th>
                  <th className="bq-num">Cuota referencial</th>
                  <th>Estado</th>
                  <th>Motivo</th>
                </tr>
              </thead>
              <tbody>
                {applications.content.map((application) => (
                  <tr key={application.idSolicitud}>
                    <td className="bq-num bq-mono">{application.idSolicitud}</td>
                    <td>{formatDate(application.fechaSolicitud)}</td>
                    <td>{purposes[application.destinoCredito] ?? application.destinoCredito}</td>
                    <td className="bq-num">{dollars.format(application.monto)}</td>
                    <td className="bq-num">{application.plazoMeses} meses</td>
                    <td>{amortizationMethods[application.metodoAmortizacion]}</td>
                    <td className="bq-num">{dollars.format(application.cuotaEvaluacionReferencial)}</td>
                    <td>
                      <span className={`bq-estado ${statuses[application.status]?.className ?? 'bq-estado-neutro'}`}>
                        {statuses[application.status]?.label ?? application.status}
                      </span>
                    </td>
                    <td>{application.motivoRechazo ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={applications.page} onPage={setPage} />
        </div>
      ) : null}
    </main>
  )
}

function Pagination({ page, onPage }) {
  if (page.totalPages <= 1) {
    return null
  }

  return (
    <div className="bq-acciones">
      <span className="bq-pequeno bq-texto-2">
        Página {page.number + 1} de {page.totalPages} · {page.totalElements} registros
      </span>
      <button className="bq-btn bq-btn-secundario bq-btn-sm" type="button" disabled={page.number === 0} onClick={() => onPage(page.number - 1)}>
        Anterior
      </button>
      <button
        className="bq-btn bq-btn-secundario bq-btn-sm"
        type="button"
        disabled={page.number + 1 >= page.totalPages}
        onClick={() => onPage(page.number + 1)}
      >
        Siguiente
      </button>
    </div>
  )
}
