import { useState } from 'react'
import { loginAnalyst } from '../services/accessApi.js'
import { saveAnalystSession } from '../services/analystSession.js'

export default function LoginPage({ onLogin }) {
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
        <div className="bq-formulario">
          <h1>Portal del analista</h1>
          <p>Gestiona solicitudes, desembolsos y operaciones de crédito.</p>
        </div>
      </section>
      <section className="bq-ingreso-formulario">
        <form className="bq-formulario" onSubmit={handleSubmit}>
          <h2>Ingresa al portal</h2>
          <p className="bq-texto-2">Escribe tu número de cédula para continuar.</p>
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
            <input
              id="nationalId"
              className="bq-input"
              type="text"
              inputMode="numeric"
              maxLength={10}
              placeholder="10 dígitos"
              value={nationalId}
              onChange={handleChange}
              autoFocus
            />
          </div>
          <button
            type="submit"
            className="bq-btn bq-btn-primario bq-btn-bloque"
            disabled={loading || nationalId.length !== 10}
          >
            {loading ? 'Ingresando...' : 'Ingresar'}
          </button>
        </form>
      </section>
    </main>
  )
}
