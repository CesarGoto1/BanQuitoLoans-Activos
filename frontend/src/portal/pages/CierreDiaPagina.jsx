import { useEffect, useState } from 'react'
import { listDayClosings, runDayClosing } from '../services/cierreDiaApi.js'

const closingStates = {
  PENDIENTE: { texto: 'Pendiente', clase: 'bq-estado-neutro' },
  EN_PROCESO: { texto: 'En proceso', clase: 'bq-estado-info' },
  COMPLETADO: { texto: 'Completado', clase: 'bq-estado-exito' },
  FALLIDO: { texto: 'Fallido', clase: 'bq-estado-peligro' },
}

const stageStates = {
  EN_PROCESO: { texto: 'En proceso', clase: 'bq-estado-info' },
  EXITOSA: { texto: 'Exitosa', clase: 'bq-estado-exito' },
  FALLIDA: { texto: 'Fallida', clase: 'bq-estado-peligro' },
}

const stageNames = {
  RECAUDACIONES: 'Recaudaciones',
  DEVENGO: 'Devengo de intereses',
  MORA: 'Interés de mora',
  CLASIFICACION: 'Clasificación de cartera',
  PROVISIONES: 'Provisiones y contabilidad',
  FOTOGRAFIA: 'Fotografía de cartera',
}

function formatDate(iso) {
  if (!iso) {
    return '-'
  }
  const [year, month, day] = iso.slice(0, 10).split('-')
  return `${day}/${month}/${year}`
}

function formatDateTime(iso) {
  return iso ? `${formatDate(iso)} ${iso.slice(11, 19)}` : '-'
}

function formatDuration(milliseconds) {
  if (milliseconds == null) {
    return '-'
  }
  return milliseconds < 1000 ? `${milliseconds} ms` : `${(milliseconds / 1000).toLocaleString('es-EC')} s`
}

function nextDay(iso) {
  const date = new Date(`${iso}T00:00:00`)
  date.setDate(date.getDate() + 1)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

export default function CierreDiaPagina() {
  const [page, setPage] = useState(0)
  const [closings, setClosings] = useState(null)
  const [error, setError] = useState('')
  const [confirming, setConfirming] = useState(false)
  const [running, setRunning] = useState(false)
  const [result, setResult] = useState(null)
  const [selected, setSelected] = useState(null)
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let current = true
    setError('')
    listDayClosings(page)
      .then((data) => current && setClosings(data))
      .catch((e) => current && setError(e.message))
    return () => {
      current = false
    }
  }, [page, reload])

  const open = page === 0 ? closings?.content.find((closing) => closing.estadoCierre !== 'COMPLETADO') : null

  async function closeDay() {
    setConfirming(false)
    setRunning(true)
    setError('')
    setResult(null)
    try {
      const closing = await runDayClosing(open.fechaNegocio)
      setResult(closing)
      setSelected(closing)
    } catch (e) {
      setError(e.message)
    } finally {
      setRunning(false)
      setReload((value) => value + 1)
    }
  }

  return (
    <main className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div>
          <h1>Cierre de día</h1>
          <p className="bq-pequeno bq-texto-2">
            Ejecuta en orden recaudaciones, devengo, mora, clasificación, provisiones y fotografía de la cartera.
            Si una etapa falla, la fecha de negocio no avanza y el cierre se puede reintentar.
          </p>
        </div>
        <div className="bq-acciones">
          <button type="button" className="bq-btn bq-btn-secundario bq-btn-sm" disabled={running} onClick={() => setReload(reload + 1)}>
            Actualizar
          </button>
        </div>
      </div>

      {error ? (
        <div className="bq-alerta bq-alerta-peligro" role="alert">
          <strong>No se pudo completar la operación</strong>
          <p>{error}</p>
        </div>
      ) : null}

      {!closings && !error ? <p className="bq-texto-2">Cargando…</p> : null}

      {page === 0 && closings ? (
        <div className="bq-tarjeta bq-tarjeta-destacada">
          {open ? (
            <>
              <div className="bq-encabezado-tarjeta">
                <div className="bq-cifra">
                  <span>Fecha de negocio vigente</span>
                  <strong>{formatDate(open.fechaNegocio)}</strong>
                </div>
                <ClosingState state={open.estadoCierre} />
              </div>
              {open.estadoCierre === 'FALLIDO' ? (
                <p className="bq-pequeno bq-texto-2">
                  El último intento falló. Al reintentar se saltan las etapas que ya terminaron con éxito.
                </p>
              ) : null}
              {confirming ? (
                <div className="bq-alerta bq-alerta-aviso" role="alert">
                  <strong>¿Cerrar el día {formatDate(open.fechaNegocio)}?</strong>
                  <p>Si todas las etapas terminan con éxito, la fecha de negocio pasará al {formatDate(nextDay(open.fechaNegocio))} y este día ya no se podrá volver a cerrar.</p>
                  <div className="bq-acciones">
                    <button type="button" className="bq-btn bq-btn-secundario bq-btn-sm" onClick={() => setConfirming(false)}>
                      Cancelar
                    </button>
                    <button type="button" className="bq-btn bq-btn-primario bq-btn-sm" onClick={closeDay}>
                      Confirmar cierre
                    </button>
                  </div>
                </div>
              ) : (
                <div className="bq-acciones">
                  <button
                    type="button"
                    className="bq-btn bq-btn-primario"
                    disabled={running || open.estadoCierre === 'EN_PROCESO'}
                    onClick={() => setConfirming(true)}
                  >
                    {running ? 'Ejecutando cierre…' : open.estadoCierre === 'FALLIDO' ? 'Reintentar cierre' : 'Ejecutar cierre'}
                  </button>
                </div>
              )}
            </>
          ) : (
            <div className="bq-alerta bq-alerta-info">
              <strong>Sin fecha de negocio abierta</strong>
              <p>No hay ningún día pendiente de cierre.</p>
            </div>
          )}
        </div>
      ) : null}

      {result ? <ClosingResult closing={result} /> : null}

      {selected ? <StageLog closing={selected} onClose={() => setSelected(null)} /> : null}

      {closings && closings.content.length > 0 ? (
        <div className="bq-tarjeta">
          <h2>Historial de cierres</h2>
          <div className="bq-tabla-contenedor">
            <table className="bq-tabla">
              <thead>
                <tr>
                  <th>Fecha de negocio</th>
                  <th>Estado</th>
                  <th className="bq-num">Etapas exitosas</th>
                  <th className="bq-num">Intentos fallidos</th>
                  <th>Acción</th>
                </tr>
              </thead>
              <tbody>
                {closings.content.map((closing) => (
                  <tr key={closing.fechaNegocio}>
                    <td>{formatDate(closing.fechaNegocio)}</td>
                    <td><ClosingState state={closing.estadoCierre} /></td>
                    <td className="bq-num">
                      {closing.ejecuciones.filter((execution) => execution.estadoEtapa === 'EXITOSA').length} de 6
                    </td>
                    <td className="bq-num">
                      {closing.ejecuciones.filter((execution) => execution.estadoEtapa === 'FALLIDA').length}
                    </td>
                    <td>
                      <button
                        type="button"
                        className="bq-btn bq-btn-secundario bq-btn-sm"
                        disabled={closing.ejecuciones.length === 0}
                        onClick={() => setSelected(closing)}
                      >
                        Ver bitácora
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={closings.page} onPage={setPage} />
        </div>
      ) : null}
    </main>
  )
}

function ClosingState({ state }) {
  const status = closingStates[state] ?? { texto: state, clase: 'bq-estado-neutro' }
  return <span className={`bq-estado ${status.clase}`}>{status.texto}</span>
}

function ClosingResult({ closing }) {
  if (closing.estadoCierre === 'COMPLETADO') {
    return (
      <div className="bq-alerta bq-alerta-exito" role="status">
        <strong>Cierre del {formatDate(closing.fechaNegocio)} completado</strong>
        <p>La nueva fecha de negocio es el {formatDate(nextDay(closing.fechaNegocio))}.</p>
      </div>
    )
  }
  const failed = closing.ejecuciones.findLast((execution) => execution.estadoEtapa === 'FALLIDA')
  return (
    <div className="bq-alerta bq-alerta-peligro" role="alert">
      <strong>El cierre del {formatDate(closing.fechaNegocio)} se detuvo{failed ? ` en la etapa ${stageNames[failed.etapa] ?? failed.etapa}` : ''}</strong>
      <p>{failed?.mensajeError ?? 'Revisa la bitácora.'} La fecha de negocio no avanzó; corrige el problema y reintenta el cierre.</p>
    </div>
  )
}

function StageLog({ closing, onClose }) {
  return (
    <div className="bq-tarjeta">
      <div className="bq-encabezado-tarjeta">
        <h2>Bitácora del cierre del {formatDate(closing.fechaNegocio)}</h2>
        <button type="button" className="bq-btn bq-btn-secundario bq-btn-sm" onClick={onClose}>
          Ocultar
        </button>
      </div>
      <div className="bq-tabla-contenedor">
        <table className="bq-tabla">
          <thead>
            <tr>
              <th>#</th>
              <th>Etapa</th>
              <th className="bq-num">Intento</th>
              <th>Estado</th>
              <th>Inicio</th>
              <th>Fin</th>
              <th className="bq-num">Duración</th>
              <th className="bq-num">Registros</th>
              <th>Error</th>
            </tr>
          </thead>
          <tbody>
            {closing.ejecuciones.map((execution) => {
              const status = stageStates[execution.estadoEtapa] ?? { texto: execution.estadoEtapa, clase: 'bq-estado-neutro' }
              return (
                <tr key={`${execution.numeroEtapa}-${execution.numeroIntento}`}>
                  <td className="bq-mono">{execution.numeroEtapa}</td>
                  <td>{stageNames[execution.etapa] ?? execution.etapa}</td>
                  <td className="bq-num">{execution.numeroIntento}</td>
                  <td><span className={`bq-estado ${status.clase}`}>{status.texto}</span></td>
                  <td className="bq-mono">{formatDateTime(execution.fechaHoraInicio)}</td>
                  <td className="bq-mono">{formatDateTime(execution.fechaHoraFin)}</td>
                  <td className="bq-num">{formatDuration(execution.duracionMilisegundos)}</td>
                  <td className="bq-num">{execution.registrosProcesados}</td>
                  <td>{execution.mensajeError ?? '-'}</td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
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
