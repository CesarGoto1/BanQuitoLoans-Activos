import { useState } from 'react'
import { simulateLoan } from '../services/solicitudesCreditoApi.js'
import CampoFormulario from '../components/CampoFormulario.jsx'
import { amountError, amountHelp, termError, termHelp, useCreditLimits } from '../components/reglasCredito.js'

const dollars = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })
const percentage = new Intl.NumberFormat('es-EC', { style: 'percent', minimumFractionDigits: 2, maximumFractionDigits: 4 })

const amortizationMethods = { FRENCH: 'Francés (cuota fija)', GERMAN: 'Alemán (cuota decreciente)' }

function formatDate(iso) {
  const [year, month, day] = iso.split('-')
  return `${day}/${month}/${year}`
}

export default function SimulacionPage({ onRequest }) {
  const [monto, setMonto] = useState('')
  const [plazoMeses, setPlazoMeses] = useState('')
  const [metodoAmortizacion, setMetodoAmortizacion] = useState('FRENCH')
  const [error, setError] = useState('')
  const [simulating, setSimulating] = useState(false)
  const [simulation, setSimulation] = useState(null)
  const limits = useCreditLimits()
  const errors = { monto: amountError(monto, limits), plazoMeses: termError(plazoMeses, limits) }
  const invalid = Object.values(errors).some(Boolean)

  async function submit(event) {
    event.preventDefault()
    setSimulating(true)
    setError('')
    try {
      setSimulation(await simulateLoan({ monto: Number(monto), plazoMeses: Number(plazoMeses), metodoAmortizacion }))
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
          <CampoFormulario
            id="simulationAmount"
            label="Monto (USD)"
            type="number"
            step="any"
            inputMode="decimal"
            value={monto}
            onChange={(e) => setMonto(e.target.value)}
            error={errors.monto}
            help={amountHelp(limits)}
            required
          />
          <CampoFormulario
            id="simulationTerm"
            label="Plazo (meses)"
            type="number"
            step="any"
            inputMode="numeric"
            value={plazoMeses}
            onChange={(e) => setPlazoMeses(e.target.value)}
            error={errors.plazoMeses}
            help={termHelp(limits)}
            required
          />
        </div>

        <div className="bq-campo">
          <span className="bq-label">Sistema de amortización</span>
          <div className="bq-segmentado" role="group" aria-label="Sistema de amortización">
            {Object.entries(amortizationMethods).map(([method, label]) => (
              <button key={method} type="button" aria-pressed={metodoAmortizacion === method} onClick={() => setMetodoAmortizacion(method)}>
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
  const german = simulation.metodoAmortizacion === 'GERMAN'

  return (
    <>
      <div className="bq-tarjeta">
        <span className="bq-etiqueta-seccion">Resultado referencial</span>
        <div className="bq-fila">
          <Field label={german ? 'Primera cuota' : 'Cuota mensual'} value={dollars.format(simulation.primeraCuota)} highlighted />
          {german ? <Field label="Última cuota" value={dollars.format(simulation.ultimaCuota)} highlighted /> : null}
          <Field label="TEA referencial" value={percentage.format(simulation.teaAsignada)} />
          <Field label="Tasa nominal anual" value={percentage.format(simulation.tasaNominalAnual)} />
        </div>
        <div className="bq-fila">
          <Field label="Seguro de desgravamen mensual" value={dollars.format(simulation.seguroMensual)} />
          <Field label="Contribución SOLCA retenida" value={dollars.format(simulation.contribucionSolca)} />
          <Field label="Monto neto a recibir" value={dollars.format(simulation.montoNeto)} highlighted />
        </div>
        <div className="bq-fila">
          <Field label="Total de intereses" value={dollars.format(simulation.interesTotal)} />
          <Field label="Total de seguro" value={dollars.format(simulation.seguroTotal)} />
          <Field label="Costo total del crédito" value={dollars.format(simulation.costoTotal)} highlighted />
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
                monto: String(simulation.monto),
                plazoMeses: String(simulation.plazoMeses),
                metodoAmortizacion: simulation.metodoAmortizacion,
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
              {simulation.cuotas.map((cuota) => (
                <tr key={cuota.numeroCuota}>
                  <td className="bq-num">{cuota.numeroCuota}</td>
                  <td>{formatDate(cuota.fechaVencimiento)}</td>
                  <td className="bq-num">{dollars.format(cuota.capital)}</td>
                  <td className="bq-num">{dollars.format(cuota.interes)}</td>
                  <td className="bq-num">{dollars.format(cuota.seguro)}</td>
                  <td className="bq-num">{dollars.format(cuota.total)}</td>
                  <td className="bq-num">{dollars.format(cuota.remainingPrincipalBalance)}</td>
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
