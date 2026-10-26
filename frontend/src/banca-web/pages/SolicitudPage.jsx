import { useState } from 'react'
import { registerApplication } from '../services/solicitudesCreditoApi.js'
import CampoFormulario from '../components/CampoFormulario.jsx'
import {
  amountError,
  amountHelp,
  expensesError,
  incomeError,
  termError,
  termHelp,
  useCreditLimits,
} from '../components/reglasCredito.js'

const dollars = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })

const amortizationMethods = { FRANCES: 'Francés (cuota fija)', ALEMAN: 'Alemán (cuota decreciente)' }

const purposes = {
  BIENES_DURADEROS: 'Bienes duraderos',
  SALUD: 'Salud',
  EDUCACION: 'Educación',
  VIAJE: 'Viaje',
  OTRO: 'Otro',
}

const emptyForm = {
  monto: '',
  plazoMeses: '',
  metodoAmortizacion: 'FRANCES',
  destinoCredito: 'BIENES_DURADEROS',
  ingresosMensuales: '',
  gastosMensuales: '',
}

export default function SolicitudPage({ customer, simulatedLoan }) {
  const [form, setForm] = useState({ ...emptyForm, ...simulatedLoan })
  const [error, setError] = useState('')
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)
  const limits = useCreditLimits()
  const errors = {
    monto: amountError(form.monto, limits),
    plazoMeses: termError(form.plazoMeses, limits),
    ingresosMensuales: incomeError(form.ingresosMensuales),
    gastosMensuales: expensesError(form.gastosMensuales),
  }
  const invalid = Object.values(errors).some(Boolean)

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
          monto: Number(form.monto),
          plazoMeses: Number(form.plazoMeses),
          metodoAmortizacion: form.metodoAmortizacion,
          destinoCredito: form.destinoCredito,
          ingresosMensuales: Number(form.ingresosMensuales),
          gastosMensuales: Number(form.gastosMensuales),
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
          {simulatedLoan ? (
            <div className="bq-alerta bq-alerta-info" role="status">
              <strong>Datos de tu simulación</strong>
              <span>Completa el destino, tus ingresos y tus gastos para registrar la solicitud.</span>
            </div>
          ) : null}
          {error ? (
            <div className="bq-alerta bq-alerta-peligro" role="alert">
              <strong>No se pudo registrar la solicitud</strong>
              <span>{error}</span>
            </div>
          ) : null}

          <span className="bq-etiqueta-seccion">Crédito</span>
          <div className="bq-fila">
            <CampoFormulario
              id="monto"
              label="Monto (USD)"
              type="number"
              step="any"
              inputMode="decimal"
              value={form.monto}
              onChange={(e) => change('monto', e.target.value)}
              error={errors.monto}
              help={amountHelp(limits)}
              required
            />
            <CampoFormulario
              id="plazoMeses"
              label="Plazo (meses)"
              type="number"
              step="any"
              inputMode="numeric"
              value={form.plazoMeses}
              onChange={(e) => change('plazoMeses', e.target.value)}
              error={errors.plazoMeses}
              help={termHelp(limits)}
              required
            />
            <div className="bq-campo">
              <label className="bq-label" htmlFor="destinoCredito">
                Destino <span className="bq-obligatorio">*</span>
              </label>
              <select className="bq-select" id="destinoCredito" value={form.destinoCredito} onChange={(e) => change('destinoCredito', e.target.value)}>
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
                  aria-pressed={form.metodoAmortizacion === method}
                  onClick={() => change('metodoAmortizacion', method)}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>

          <span className="bq-etiqueta-seccion">Capacidad de pago</span>
          <div className="bq-fila">
            <CampoFormulario
              id="ingresosMensuales"
              label="Ingresos mensuales (USD)"
              type="number"
              step="any"
              inputMode="decimal"
              value={form.ingresosMensuales}
              onChange={(e) => change('ingresosMensuales', e.target.value)}
              error={errors.ingresosMensuales}
              required
            />
            <CampoFormulario
              id="gastosMensuales"
              label="Gastos mensuales (USD)"
              type="number"
              step="any"
              inputMode="decimal"
              value={form.gastosMensuales}
              onChange={(e) => change('gastosMensuales', e.target.value)}
              error={errors.gastosMensuales}
              help="Gastos del hogar y cuotas de otras deudas."
              required
            />
          </div>

          <div className="bq-acciones">
            <button className="bq-btn bq-btn-primario" type="submit" disabled={sending || invalid}>
              {sending ? 'Registrando…' : 'Registrar solicitud'}
            </button>
          </div>
        </form>
      )}
    </main>
  )
}

function ApplicationResult({ result, onNew }) {
  const registered = result.status === 'REGISTRADA'

  return (
    <div className="bq-tarjeta">
      {registered ? (
        <div className="bq-alerta bq-alerta-exito" role="status">
          <strong>Solicitud {result.idSolicitud} registrada</strong>
          <span>Cumple la evaluación de capacidad de pago. Un analista la revisará y te asignará la tasa definitiva.</span>
        </div>
      ) : (
        <div className="bq-alerta bq-alerta-peligro" role="alert">
          <strong>Solicitud {result.idSolicitud} rechazada</strong>
          <span>{result.motivoRechazo}</span>
        </div>
      )}

      <div className="bq-fila">
        <div className="bq-campo">
          <span className="bq-label">Monto solicitado</span>
          <span>{dollars.format(result.monto)}</span>
        </div>
        <div className="bq-campo">
          <span className="bq-label">Plazo</span>
          <span>{result.plazoMeses} meses</span>
        </div>
        <div className="bq-campo">
          <span className="bq-label">Sistema</span>
          <span>{amortizationMethods[result.metodoAmortizacion]}</span>
        </div>
        <div className="bq-campo">
          <span className="bq-label">{result.metodoAmortizacion === 'ALEMAN' ? 'Primera cuota (la más alta)' : 'Cuota mensual'} referencial</span>
          <span>{dollars.format(result.cuotaEvaluacionReferencial)}</span>
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
