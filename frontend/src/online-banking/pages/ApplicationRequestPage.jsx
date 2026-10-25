import { useState } from 'react'
import { registerApplication } from '../services/loanApplicationsApi.js'

const dollars = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })

const amortizationMethods = { FRENCH: 'Francés (cuota fija)', GERMAN: 'Alemán (cuota decreciente)' }

const purposes = {
  DURABLE_GOODS: 'Bienes duraderos',
  HEALTH: 'Salud',
  EDUCATION: 'Educación',
  TRAVEL: 'Viaje',
  OTHER: 'Otro',
}

const emptyForm = {
  amount: '',
  termMonths: '',
  amortizationMethod: 'FRENCH',
  purpose: 'DURABLE_GOODS',
  monthlyIncome: '',
  monthlyExpenses: '',
}

export default function ApplicationRequestPage({ customer }) {
  const [form, setForm] = useState(emptyForm)
  const [error, setError] = useState('')
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  function change(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  async function submit(event) {
    event.preventDefault()
    setSending(true)
    setError('')
    try {
      setResult(
        await registerApplication({
          customerId: customer.customerId,
          amount: Number(form.amount),
          termMonths: Number(form.termMonths),
          amortizationMethod: form.amortizationMethod,
          purpose: form.purpose,
          monthlyIncome: Number(form.monthlyIncome),
          monthlyExpenses: Number(form.monthlyExpenses),
        }),
      )
    } catch (e) {
      setError(e.message)
    } finally {
      setSending(false)
    }
  }

  function newApplication() {
    setForm(emptyForm)
    setResult(null)
  }

  return (
    <main className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div>
          <h1>Solicitar crédito</h1>
          <p className="bq-pequeno bq-texto-2">Registra tu solicitud y conoce al instante si cumple la evaluación de capacidad de pago.</p>
        </div>
      </div>

      {result ? (
        <ApplicationResult result={result} onNew={newApplication} />
      ) : (
        <form className="bq-tarjeta bq-formulario" onSubmit={submit}>
          {error ? (
            <div className="bq-alerta bq-alerta-peligro" role="alert">
              <strong>No se pudo registrar la solicitud</strong>
              <span>{error}</span>
            </div>
          ) : null}

          <span className="bq-etiqueta-seccion">Crédito</span>
          <div className="bq-fila">
            <div className="bq-campo">
              <label className="bq-label" htmlFor="amount">
                Monto (USD) <span className="bq-obligatorio">*</span>
              </label>
              <input
                className="bq-input"
                id="amount"
                type="number"
                min="0.01"
                step="0.01"
                inputMode="decimal"
                value={form.amount}
                onChange={(e) => change('amount', e.target.value)}
                required
              />
            </div>
            <div className="bq-campo">
              <label className="bq-label" htmlFor="termMonths">
                Plazo (meses) <span className="bq-obligatorio">*</span>
              </label>
              <input
                className="bq-input"
                id="termMonths"
                type="number"
                min="1"
                step="1"
                inputMode="numeric"
                value={form.termMonths}
                onChange={(e) => change('termMonths', e.target.value)}
                required
              />
            </div>
            <div className="bq-campo">
              <label className="bq-label" htmlFor="purpose">
                Destino <span className="bq-obligatorio">*</span>
              </label>
              <select className="bq-select" id="purpose" value={form.purpose} onChange={(e) => change('purpose', e.target.value)}>
                {Object.entries(purposes).map(([purpose, label]) => (
                  <option key={purpose} value={purpose}>{label}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="bq-campo">
            <span className="bq-label">Sistema de amortización</span>
            <div className="bq-segmentado" role="group" aria-label="Sistema de amortización">
              {Object.entries(amortizationMethods).map(([method, label]) => (
                <button
                  key={method}
                  type="button"
                  aria-pressed={form.amortizationMethod === method}
                  onClick={() => change('amortizationMethod', method)}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>

          <span className="bq-etiqueta-seccion">Capacidad de pago</span>
          <div className="bq-fila">
            <div className="bq-campo">
              <label className="bq-label" htmlFor="monthlyIncome">
                Ingresos mensuales (USD) <span className="bq-obligatorio">*</span>
              </label>
              <input
                className="bq-input"
                id="monthlyIncome"
                type="number"
                min="0.01"
                step="0.01"
                inputMode="decimal"
                value={form.monthlyIncome}
                onChange={(e) => change('monthlyIncome', e.target.value)}
                required
              />
            </div>
            <div className="bq-campo">
              <label className="bq-label" htmlFor="monthlyExpenses">
                Gastos mensuales (USD) <span className="bq-obligatorio">*</span>
              </label>
              <input
                className="bq-input"
                id="monthlyExpenses"
                type="number"
                min="0"
                step="0.01"
                inputMode="decimal"
                value={form.monthlyExpenses}
                onChange={(e) => change('monthlyExpenses', e.target.value)}
                required
              />
              <span className="bq-ayuda">Gastos del hogar y cuotas de otras deudas.</span>
            </div>
          </div>

          <div className="bq-acciones">
            <button className="bq-btn bq-btn-primario" type="submit" disabled={sending}>
              {sending ? 'Registrando…' : 'Registrar solicitud'}
            </button>
          </div>
        </form>
      )}
    </main>
  )
}

function ApplicationResult({ result, onNew }) {
  const registered = result.status === 'REGISTERED'

  return (
    <div className="bq-tarjeta">
      {registered ? (
        <div className="bq-alerta bq-alerta-exito" role="status">
          <strong>Solicitud {result.applicationId} registrada</strong>
          <span>Cumple la evaluación de capacidad de pago. Un analista la revisará y te asignará la tasa definitiva.</span>
        </div>
      ) : (
        <div className="bq-alerta bq-alerta-peligro" role="alert">
          <strong>Solicitud {result.applicationId} rechazada</strong>
          <span>{result.rejectionReason}</span>
        </div>
      )}

      <div className="bq-fila">
        <div className="bq-campo">
          <span className="bq-label">Monto solicitado</span>
          <span>{dollars.format(result.amount)}</span>
        </div>
        <div className="bq-campo">
          <span className="bq-label">Plazo</span>
          <span>{result.termMonths} meses</span>
        </div>
        <div className="bq-campo">
          <span className="bq-label">Sistema</span>
          <span>{amortizationMethods[result.amortizationMethod]}</span>
        </div>
        <div className="bq-campo">
          <span className="bq-label">{result.amortizationMethod === 'GERMAN' ? 'Primera cuota (la más alta)' : 'Cuota mensual'} referencial</span>
          <span>{dollars.format(result.referenceInstallment)}</span>
        </div>
      </div>
      <p className="bq-pequeno bq-texto-2">La cuota incluye capital, interés y seguro de desgravamen, calculada con la tasa referencial.</p>

      <div className="bq-acciones">
        <button className="bq-btn bq-btn-secundario" type="button" onClick={onNew}>
          Nueva solicitud
        </button>
      </div>
    </div>
  )
}
