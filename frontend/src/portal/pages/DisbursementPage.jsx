import { useEffect, useState } from 'react'
import { disburse, getDisbursementPreview } from '../services/disbursementApi.js'

const dollars = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })
const percentage = new Intl.NumberFormat('es-EC', { style: 'percent', minimumFractionDigits: 2 })
const amortizationMethods = { FRENCH: 'Francés', GERMAN: 'Alemán' }
const accountTypes = { AHORROS: 'Ahorros', CORRIENTE: 'Corriente' }

function formatDate(iso) {
  const [year, month, day] = iso.split('-')
  return `${day}/${month}/${year}`
}

export default function DisbursementPage({ applicationId }) {
  const [preview, setPreview] = useState(null)
  const [destinationAccount, setDestinationAccount] = useState('')
  const [error, setError] = useState('')
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  useEffect(() => {
    getDisbursementPreview(applicationId)
      .then((data) => {
        setPreview(data)
        setDestinationAccount(data.accounts[0]?.accountNumber ?? '')
      })
      .catch((e) => setError(e.message))
  }, [applicationId])

  async function confirm(event) {
    event.preventDefault()
    setSending(true)
    setError('')
    try {
      setResult(await disburse(applicationId, destinationAccount))
    } catch (e) {
      setError(e.message)
    } finally {
      setSending(false)
    }
  }

  if (!preview) {
    return (
      <main className="bq-pagina">
        {error ? (
          <div className="bq-alerta bq-alerta-peligro" role="alert">
            <strong>No se pudo cargar la solicitud</strong>
            <p>{error}</p>
          </div>
        ) : (
          <p className="bq-texto-2">Cargando…</p>
        )}
      </main>
    )
  }

  return (
    <main className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div>
          <h1>Desembolso</h1>
          <p className="bq-pequeno bq-texto-2">
            Solicitud {preview.applicationId} · {preview.customerName} ·{' '}
            <span className="bq-mono">{preview.customerNationalId}</span>
          </p>
        </div>
      </div>

      {result ? (
        <div className="bq-alerta bq-alerta-exito" role="status">
          <strong>Crédito desembolsado</strong>
          <p>
            Operación <span className="bq-mono">{result.loanNumber}</span>: se acreditaron{' '}
            <span className="bq-monto">{dollars.format(result.netAmount)}</span> en la cuenta{' '}
            <span className="bq-mono">{result.destinationAccount}</span> el {formatDate(result.disbursementDate)}.
          </p>
        </div>
      ) : null}

      <div className="bq-tarjeta">
        <span className="bq-etiqueta-seccion">Condiciones aprobadas</span>
        <div className="bq-fila">
          <Field label="Monto aprobado" value={dollars.format(preview.approvedAmount)} amount />
          <Field label="Tasa efectiva anual (TEA)" value={percentage.format(preview.effectiveAnnualRate)} />
          <Field label="Plazo" value={`${preview.termMonths} meses`} />
          <Field label="Sistema de amortización" value={amortizationMethods[preview.amortizationMethod]} />
        </div>

        <span className="bq-etiqueta-seccion">Valores del desembolso</span>
        <div className="bq-fila">
          <Field label="Seguro de desgravamen mensual" value={dollars.format(preview.monthlyInsurance)} amount />
          <Field label="Contribución SOLCA retenida" value={dollars.format(preview.solcaContribution)} amount />
          <Field label="Monto neto a acreditar" value={dollars.format(preview.netAmount)} amount highlighted />
          <Field label="Fecha de desembolso" value={formatDate(preview.disbursementDate)} />
        </div>
      </div>

      <form className="bq-tarjeta bq-formulario" onSubmit={confirm}>
        <div className="bq-campo">
          <label className="bq-label" htmlFor="destinationAccount">
            Cuenta de destino <span className="bq-obligatorio">*</span>
          </label>
          <select
            className="bq-select"
            id="destinationAccount"
            value={destinationAccount}
            onChange={(e) => setDestinationAccount(e.target.value)}
            disabled={result !== null}
            required
          >
            {preview.accounts.map((account) => (
              <option key={account.accountNumber} value={account.accountNumber}>
                {accountTypes[account.accountType] ?? account.accountType} · {account.accountNumber}
              </option>
            ))}
          </select>
          <span className="bq-ayuda">Cuentas activas del cliente en el Core de Pasivos.</span>
        </div>

        {error ? (
          <div className="bq-alerta bq-alerta-peligro" role="alert">
            <strong>No se pudo desembolsar</strong>
            <p>{error}</p>
          </div>
        ) : null}

        <div className="bq-acciones">
          <button
            className="bq-btn bq-btn-primario"
            type="submit"
            disabled={sending || result !== null || destinationAccount === ''}
          >
            {sending ? 'Desembolsando…' : 'Desembolsar crédito'}
          </button>
        </div>
      </form>
    </main>
  )
}

function Field({ label, value, amount = false, highlighted = false }) {
  return (
    <div className="bq-campo">
      <span className="bq-label">{label}</span>
      <span className={amount ? 'bq-monto' : undefined} style={highlighted ? { fontWeight: 700 } : undefined}>
        {value}
      </span>
    </div>
  )
}
