import { useState } from 'react'
import { simulateLoan } from '../services/loanApplicationsApi.js'
import FormField from '../components/FormField.jsx'
import { amountError, amountHelp, termError, termHelp, useCreditLimits } from '../components/creditRules.js'

const dollars = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })
const percentage = new Intl.NumberFormat('es-EC', { style: 'percent', minimumFractionDigits: 2, maximumFractionDigits: 4 })

const amortizationMethods = { FRENCH: 'Francés (cuota fija)', GERMAN: 'Alemán (cuota decreciente)' }

function formatDate(iso) {
  const [year, month, day] = iso.split('-')
  return `${day}/${month}/${year}`
}

export default function SimulationPage({ onRequest }) {
  const [amount, setAmount] = useState('')
  const [termMonths, setTermMonths] = useState('')
  const [amortizationMethod, setAmortizationMethod] = useState('FRENCH')
  const [error, setError] = useState('')
  const [simulating, setSimulating] = useState(false)
  const [simulation, setSimulation] = useState(null)
  const limits = useCreditLimits()
  const errors = { amount: amountError(amount, limits), termMonths: termError(termMonths, limits) }
  const invalid = Object.values(errors).some(Boolean)

  async function submit(event) {
    event.preventDefault()
    setSimulating(true)
    setError('')
    try {
      setSimulation(await simulateLoan({ amount: Number(amount), termMonths: Number(termMonths), amortizationMethod }))
    } catch (e) {
      setError(e.message)
      setSimulation(null)
    } finally {
      setSimulating(false)
    }
  }

  return (
    <main className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div>
          <h1>Simular crédito</h1>
          <p className="bq-pequeno bq-texto-2">Calcula tu cuota y la tabla de amortización proyectada. La simulación no registra ninguna solicitud.</p>
        </div>
      </div>

      <form className="bq-tarjeta bq-formulario" onSubmit={submit}>
        {error ? (
          <div className="bq-alerta bq-alerta-peligro" role="alert">
            <strong>No se pudo simular</strong>
            <span>{error}</span>
          </div>
        ) : null}

        <div className="bq-fila">
          <FormField
            id="simulationAmount"
            label="Monto (USD)"
            type="number"
            step="any"
            inputMode="decimal"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            error={errors.amount}
            help={amountHelp(limits)}
            required
          />
          <FormField
            id="simulationTerm"
            label="Plazo (meses)"
            type="number"
            step="any"
            inputMode="numeric"
            value={termMonths}
            onChange={(e) => setTermMonths(e.target.value)}
            error={errors.termMonths}
            help={termHelp(limits)}
            required
          />
        </div>

        <div className="bq-campo">
          <span className="bq-label">Sistema de amortización</span>
          <div className="bq-segmentado" role="group" aria-label="Sistema de amortización">
            {Object.entries(amortizationMethods).map(([method, label]) => (
              <button key={method} type="button" aria-pressed={amortizationMethod === method} onClick={() => setAmortizationMethod(method)}>
                {label}
              </button>
            ))}
          </div>
        </div>

        <div className="bq-acciones">
          <button className="bq-btn bq-btn-primario" type="submit" disabled={simulating || invalid}>
            {simulating ? 'Simulando…' : 'Simular'}
          </button>
        </div>
      </form>

      {simulation ? <SimulationResult simulation={simulation} onRequest={onRequest} /> : null}
    </main>
  )
}

function SimulationResult({ simulation, onRequest }) {
  const german = simulation.amortizationMethod === 'GERMAN'

  return (
    <>
      <div className="bq-tarjeta">
        <span className="bq-etiqueta-seccion">Resultado referencial</span>
        <div className="bq-fila">
          <Field label={german ? 'Primera cuota' : 'Cuota mensual'} value={dollars.format(simulation.firstInstallment)} highlighted />
          {german ? <Field label="Última cuota" value={dollars.format(simulation.lastInstallment)} highlighted /> : null}
          <Field label="TEA referencial" value={percentage.format(simulation.effectiveAnnualRate)} />
          <Field label="Tasa nominal anual" value={percentage.format(simulation.nominalAnnualRate)} />
        </div>
        <div className="bq-fila">
          <Field label="Seguro de desgravamen mensual" value={dollars.format(simulation.monthlyInsurance)} />
          <Field label="Contribución SOLCA retenida" value={dollars.format(simulation.solcaContribution)} />
          <Field label="Monto neto a recibir" value={dollars.format(simulation.netAmount)} highlighted />
        </div>
        <div className="bq-fila">
          <Field label="Total de intereses" value={dollars.format(simulation.totalInterest)} />
          <Field label="Total de seguro" value={dollars.format(simulation.totalInsurance)} />
          <Field label="Costo total del crédito" value={dollars.format(simulation.totalCost)} highlighted />
        </div>
        <p className="bq-pequeno bq-texto-2">
          El costo total suma intereses, seguro de desgravamen y SOLCA. La tasa definitiva la asigna el analista al aprobar tu solicitud.
        </p>
        <div className="bq-acciones">
          <button
            className="bq-btn bq-btn-primario"
            type="button"
            onClick={() =>
              onRequest({
                amount: String(simulation.amount),
                termMonths: String(simulation.termMonths),
                amortizationMethod: simulation.amortizationMethod,
              })
            }
          >
            Solicitar este crédito
          </button>
        </div>
      </div>

      <div className="bq-tarjeta">
        <span className="bq-etiqueta-seccion">Tabla de amortización proyectada</span>
        <div className="bq-tabla-contenedor">
          <table className="bq-tabla">
            <thead>
              <tr>
                <th className="bq-num">N.º</th>
                <th>Vencimiento</th>
                <th className="bq-num">Capital</th>
                <th className="bq-num">Interés</th>
                <th className="bq-num">Seguro</th>
                <th className="bq-num">Cuota total</th>
                <th className="bq-num">Saldo</th>
              </tr>
            </thead>
            <tbody>
              {simulation.installments.map((installment) => (
                <tr key={installment.installmentNumber}>
                  <td className="bq-num">{installment.installmentNumber}</td>
                  <td>{formatDate(installment.dueDate)}</td>
                  <td className="bq-num">{dollars.format(installment.principal)}</td>
                  <td className="bq-num">{dollars.format(installment.interest)}</td>
                  <td className="bq-num">{dollars.format(installment.insurance)}</td>
                  <td className="bq-num">{dollars.format(installment.total)}</td>
                  <td className="bq-num">{dollars.format(installment.remainingPrincipalBalance)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}

function Field({ label, value, highlighted = false }) {
  return (
    <div className="bq-campo">
      <span className="bq-label">{label}</span>
      {highlighted ? <strong>{value}</strong> : <span>{value}</span>}
    </div>
  )
}
