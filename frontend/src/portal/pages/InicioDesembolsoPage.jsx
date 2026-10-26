import { useEffect, useState } from 'react'
import DesembolsoPage from './DesembolsoPage.jsx'
import { findPendingDisbursements } from '../services/desembolsoApi.js'

const dollars = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })
const percentage = new Intl.NumberFormat('es-EC', { style: 'percent', minimumFractionDigits: 2 })
const amortizationMethods = { FRENCH: 'Francés', GERMAN: 'Alemán' }

function formatDate(iso) {
  if (!iso) {
    return '-'
  }
  const [year, month, day] = iso.split('-')
  return `${day}/${month}/${year}`
}

export default function InicioDesembolsoPage() {
  const [page, setPage] = useState(0)
  const [pending, setPending] = useState(null)
  const [error, setError] = useState('')
  const [idSolicitud, setIdSolicitud] = useState(null)
  const [reload, setReload] = useState(0)

  useEffect(() => {
    if (idSolicitud) {
      return undefined
    }
    let current = true
    setError('')
    findPendingDisbursements(page)
      .then((data) => current && setPending(data))
      .catch((e) => current && setError(e.message))
    return () => {
      current = false
    }
  }, [page, idSolicitud, reload])

  if (idSolicitud) {
    return (
      <>
        <div className="bq-pagina">
          <div className="bq-acciones">
            <button type="button" className="bq-btn bq-btn-secundario bq-btn-sm" onClick={() => setIdSolicitud(null)}>
              Volver a la bandeja
            </button>
          </div>
        </div>
        <DesembolsoPage key={idSolicitud} idSolicitud={idSolicitud} />
      </>
    )
  }

  return (
    <main className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div>
          <h1>Solicitudes por desembolsar</h1>
          <p className="bq-pequeno bq-texto-2">Solicitudes aprobadas que todavía no tienen operación, de la más antigua a la más reciente.</p>
        </div>
        <div className="bq-acciones">
          <button type="button" className="bq-btn bq-btn-secundario bq-btn-sm" onClick={() => setReload(reload + 1)}>
            Actualizar
          </button>
        </div>
      </div>

      {error ? (
        <div className="bq-alerta bq-alerta-peligro" role="alert">
          <strong>No se pudo cargar la bandeja</strong>
          <p>{error}</p>
        </div>
      ) : null}

      {!pending && !error ? <p className="bq-texto-2">Cargando…</p> : null}

      {pending && pending.content.length === 0 ? (
        <div className="bq-tarjeta">
          <div className="bq-alerta bq-alerta-info">
            <strong>Bandeja al día</strong>
            <p>No hay solicitudes aprobadas pendientes de desembolso.</p>
          </div>
        </div>
      ) : null}

      {pending && pending.content.length > 0 ? (
        <div className="bq-tarjeta">
          <div className="bq-tabla-contenedor">
            <table className="bq-tabla">
              <thead>
                <tr>
                  <th># Solicitud</th>
                  <th>Cliente</th>
                  <th>Cédula</th>
                  <th className="bq-num">Monto aprobado</th>
                  <th>Plazo</th>
                  <th>Sistema</th>
                  <th className="bq-num">TEA</th>
                  <th>Fecha de aprobación</th>
                  <th>Acción</th>
                </tr>
              </thead>
              <tbody>
                {pending.content.map((application) => (
                  <tr key={application.idSolicitud}>
                    <td className="bq-mono">#{application.idSolicitud}</td>
                    <td>{application.nombreCliente ?? `Cliente ${application.idCliente}`}</td>
                    <td className="bq-mono">{application.cedulaCliente ?? '-'}</td>
                    <td className="bq-num bq-monto">{dollars.format(application.montoAprobado)}</td>
                    <td>{application.plazoMeses} meses</td>
                    <td>{amortizationMethods[application.metodoAmortizacion] ?? application.metodoAmortizacion}</td>
                    <td className="bq-num">{percentage.format(application.teaAsignada)}</td>
                    <td>{formatDate(application.fechaResolucion)}</td>
                    <td>
                      <button
                        type="button"
                        className="bq-btn bq-btn-primario bq-btn-sm"
                        onClick={() => setIdSolicitud(application.idSolicitud)}
                      >
                        Desembolsar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={pending.page} onPage={setPage} />
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
