import { useState } from 'react'

const AMORTIZATION_SYSTEMS = [
  { value: 'FRENCH', label: 'Francés (cuota fija)' },
  { value: 'GERMAN', label: 'Alemán (capital fijo)' },
]

const CREDIT_PURPOSES = [
  { value: 'DURABLE_GOODS', label: 'Bienes duraderos' },
  { value: 'HEALTH', label: 'Salud' },
  { value: 'EDUCATION', label: 'Educación' },
  { value: 'TRAVEL', label: 'Viaje' },
  { value: 'OTHER', label: 'Otro' },
]

const EMPTY_FORM = {
  requestedAmount: '',
  termMonths: '',
  amortizationSystem: 'FRENCH',
  creditPurpose: 'DURABLE_GOODS',
  monthlyIncome: '',
  monthlyExpenses: '',
}

const MONEY = /^\d+(\.\d{1,2})?$/
const WHOLE = /^\d+$/

// Solo formato: los rangos (PAR-01..PAR-04) los valida el backend.
function validate(form) {
  const errors = {}
  if (form.requestedAmount === '') errors.requestedAmount = 'Ingresa el monto que necesitas'
  else if (!MONEY.test(form.requestedAmount) || Number(form.requestedAmount) <= 0)
    errors.requestedAmount = 'Ingresa un monto mayor que cero, con máximo dos decimales'

  if (form.termMonths === '') errors.termMonths = 'Ingresa el plazo en meses'
  else if (!WHOLE.test(form.termMonths) || Number(form.termMonths) <= 0)
    errors.termMonths = 'El plazo debe ser un número entero de meses'

  if (form.monthlyIncome === '') errors.monthlyIncome = 'Ingresa tus ingresos mensuales'
  else if (!MONEY.test(form.monthlyIncome)) errors.monthlyIncome = 'Ingresa un valor no negativo, con máximo dos decimales'

  if (form.monthlyExpenses === '') errors.monthlyExpenses = 'Ingresa tus gastos mensuales'
  else if (!MONEY.test(form.monthlyExpenses)) errors.monthlyExpenses = 'Ingresa un valor no negativo, con máximo dos decimales'

  return errors
}

function Field({ id, label, help, error, children }) {
  return (
    <div className={error ? 'bq-campo bq-error' : 'bq-campo'}>
      <label className="bq-label" htmlFor={id}>{label} <span className="bq-obligatorio">*</span></label>
      {children}
      {(error || help) && <span className="bq-ayuda" id={`${id}-ayuda`}>{error ?? help}</span>}
    </div>
  )
}

export default function LoanApplicationForm({ onSubmit, sending }) {
  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})

  const update = (field) => (event) => {
    setForm({ ...form, [field]: event.target.value })
    if (errors[field]) setErrors({ ...errors, [field]: undefined })
  }

  const input = (field, className) => ({
    id: field,
    className: `bq-input ${className}`,
    inputMode: 'decimal',
    value: form[field],
    onChange: update(field),
    'aria-invalid': Boolean(errors[field]),
    'aria-describedby': `${field}-ayuda`,
  })

  const submit = (event) => {
    event.preventDefault()
    const found = validate(form)
    setErrors(found)
    const firstInvalid = Object.keys(found).find((field) => found[field])
    if (firstInvalid) {
      document.getElementById(firstInvalid)?.focus()
      return
    }
    onSubmit({
      requestedAmount: Number(form.requestedAmount),
      termMonths: Number(form.termMonths),
      amortizationSystem: form.amortizationSystem,
      creditPurpose: form.creditPurpose,
      monthlyIncome: Number(form.monthlyIncome),
      monthlyExpenses: Number(form.monthlyExpenses),
    })
  }

  return (
    <form className="bq-tarjeta bq-formulario" onSubmit={submit} noValidate>
      <div className="bq-titulo-tarjeta">
        <span className="bq-icono">$</span>
        <h2>Datos del préstamo</h2>
      </div>

      <Field id="requestedAmount" label="Monto" help="Entre $ 500,00 y $ 30.000,00" error={errors.requestedAmount}>
        <input {...input('requestedAmount', 'bq-monto')} />
      </Field>

      <Field id="termMonths" label="Plazo en meses" help="Entre 6 y 60 meses" error={errors.termMonths}>
        <input {...input('termMonths', 'bq-num')} inputMode="numeric" />
      </Field>

      <div className="bq-campo">
        <span className="bq-label">Sistema de amortización <span className="bq-obligatorio">*</span></span>
        <div className="bq-segmentado">
          {AMORTIZATION_SYSTEMS.map((system) => (
            <button key={system.value} type="button" aria-pressed={form.amortizationSystem === system.value}
                    onClick={() => setForm({ ...form, amortizationSystem: system.value })}>
              {system.label}
            </button>
          ))}
        </div>
      </div>

      <div className="bq-campo">
        <label className="bq-label" htmlFor="creditPurpose">Destino del crédito <span className="bq-obligatorio">*</span></label>
        <select className="bq-select" id="creditPurpose" value={form.creditPurpose} onChange={update('creditPurpose')}>
          {CREDIT_PURPOSES.map((purpose) => (
            <option key={purpose.value} value={purpose.value}>{purpose.label}</option>
          ))}
        </select>
      </div>

      <div className="bq-rejilla">
        <Field id="monthlyIncome" label="Ingresos mensuales" error={errors.monthlyIncome}>
          <input {...input('monthlyIncome', 'bq-monto')} />
        </Field>
        <Field id="monthlyExpenses" label="Gastos mensuales" help="Gastos del hogar y cuotas de otras deudas"
               error={errors.monthlyExpenses}>
          <input {...input('monthlyExpenses', 'bq-monto')} />
        </Field>
      </div>

      <div className="bq-acciones">
        <button className="bq-btn bq-btn-primario" type="submit" disabled={sending}>
          {sending ? 'Enviando…' : 'Registrar solicitud'}
        </button>
      </div>
    </form>
  )
}
