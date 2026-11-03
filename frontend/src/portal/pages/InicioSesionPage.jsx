import { useState } from 'react'
import { loginAnalyst } from '../services/accesoApi.js'
import { saveAnalystSession } from '../services/sesionAnalista.js'

const BENEFICIOS = [
  {
    titulo: 'Bandeja de solicitudes',
    detalle: 'Revisa, aprueba o rechaza créditos en minutos.',
    icono: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M22 12h-6l-2 3h-4l-2-3H2" />
        <path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z" />
      </svg>
    ),
  },
  {
    titulo: 'Desembolsos',
    detalle: 'Acredita el préstamo en la cuenta del cliente.',
    icono: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="2" y="6" width="20" height="12" rx="2" />
        <circle cx="12" cy="12" r="2.5" />
        <path d="M6 12h.01M18 12h.01" />
      </svg>
    ),
  },
  {
    titulo: 'Cartera y cierre',
    detalle: 'Consulta operaciones, reportes y el cierre del día.',
    icono: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M3 3v18h18" />
        <path d="M7 15l4-4 3 3 5-6" />
      </svg>
    ),
  },
]

export default function InicioSesionPage({ onLogin }) {
  const [nationalId, setNationalId] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  function handleChange(event) {
    setNationalId(event.target.value.replace(/\D/g, ''))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      const analyst = await loginAnalyst(nationalId)
      saveAnalystSession(analyst)
      onLogin(analyst)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="bq-ingreso">
      <section className="bq-ingreso-panel">
        <div className="bq-marca">
          <img className="bq-logo" src="/logo-bq.png" alt="BanQuito" />
          <span className="bq-marca-nombre">BanQuito</span>
        </div>
        <div className="bq-ingreso-mensaje">
          <span className="bq-ingreso-etiqueta">Portal del analista</span>
          <h1>
            Gestiona el crédito <em>de principio a fin.</em>
          </h1>
          <p className="bq-ingreso-descripcion">
            Todo lo que necesitas para atender solicitudes, desembolsar y dar seguimiento a cada operación.
          </p>
          <ul className="bq-ingreso-lista">
            {BENEFICIOS.map((beneficio) => (
              <li key={beneficio.titulo}>
                <span className="bq-ingreso-icono" aria-hidden="true">{beneficio.icono}</span>
                <span>
                  <strong>{beneficio.titulo}</strong>
                  <small>{beneficio.detalle}</small>
                </span>
              </li>
            ))}
          </ul>
        </div>
        <div className="bq-ingreso-pie">
          <span className="bq-ingreso-lema">
            <span className="bq-ingreso-cuotas" aria-hidden="true">
              <span />
              <span />
              <span />
              <span />
            </span>
            Tu crédito, cuota a cuota.
          </span>
          <span className="bq-ingreso-seguro">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <rect x="4" y="11" width="16" height="10" rx="2" />
              <path d="M8 11V7a4 4 0 0 1 8 0v4" />
            </svg>
            Uso interno · Banco BanQuito
          </span>
        </div>
      </section>
      <section className="bq-ingreso-formulario">
        <form className="bq-formulario bq-ingreso-tarjeta" onSubmit={handleSubmit}>
          <div className="bq-ingreso-encabezado">
            <span className="bq-ingreso-icono" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="7" width="18" height="13" rx="2" />
                <path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                <path d="M3 13h18" />
              </svg>
            </span>
            <h2>Ingresa al portal</h2>
            <p className="bq-texto-2">Usa tu número de cédula de analista para continuar.</p>
          </div>
          {error && (
            <div className="bq-alerta bq-alerta-peligro" role="alert">
              <strong>No se pudo ingresar</strong>
              <span>{error}</span>
            </div>
          )}
          <div className="bq-campo">
            <label className="bq-label" htmlFor="nationalId">
              Cédula <span className="bq-obligatorio">*</span>
            </label>
            <div className="bq-ingreso-entrada">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <rect x="2" y="5" width="20" height="14" rx="2" />
                <circle cx="8" cy="11" r="2" />
                <path d="M5 16a3 3 0 0 1 6 0M14 10h4M14 14h3" />
              </svg>
              <input
                id="nationalId"
                className="bq-input"
                type="text"
                inputMode="numeric"
                autoComplete="username"
                maxLength={10}
                placeholder="Ej. 1712345678"
                aria-describedby="nationalIdAyuda"
                value={nationalId}
                onChange={handleChange}
                autoFocus
              />
            </div>
            <div className="bq-ingreso-ayuda" id="nationalIdAyuda">
              <span>10 dígitos, sin guiones</span>
              <span className="bq-ingreso-contador" data-completo={nationalId.length === 10}>
                {nationalId.length}/10
              </span>
            </div>
          </div>
          <button
            type="submit"
            className="bq-btn bq-btn-primario bq-btn-bloque bq-ingreso-boton"
            disabled={loading || nationalId.length !== 10}
          >
            {loading ? 'Ingresando...' : 'Ingresar'}
            {!loading && (
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M5 12h14M13 6l6 6-6 6" />
              </svg>
            )}
          </button>
          <div className="bq-ingreso-aviso">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              <path d="m9 12 2 2 4-4" />
            </svg>
            <span>Acceso exclusivo para personal autorizado. Cada acción queda registrada con tu usuario.</span>
          </div>
        </form>
        <a className="bq-ingreso-volver" href="/">
          ← Volver a la página principal
        </a>
        <p className="bq-ingreso-ayuda-pie">¿Problemas para ingresar? Comunícate con tu supervisor.</p>
      </section>
    </main>
  )
}
