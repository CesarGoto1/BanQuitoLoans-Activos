const STATUS = {
  REGISTERED: { label: 'Registrada', className: 'bq-estado-info' },
  REJECTED: { label: 'Rechazada', className: 'bq-estado-peligro' },
  APPROVED: { label: 'Aprobada', className: 'bq-estado-exito' },
}

const money = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })
const rate = new Intl.NumberFormat('es-EC', { style: 'percent', minimumFractionDigits: 2 })
const number = new Intl.NumberFormat('es-EC', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

function formatDate(isoDate) {
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}

export default function EvaluationResult({ result }) {
  const status = STATUS[result.status] ?? { label: result.status, className: 'bq-estado-neutro' }
  const rejected = result.status === 'REJECTED'

  return (
    <section className="bq-tarjeta">
      <div className="bq-encabezado-tarjeta">
        <h2>Resultado de la evaluación</h2>
        <span className={`bq-estado ${status.className}`}>{status.label}</span>
      </div>

      <div className="bq-cifras">
        <div className="bq-cifra bq-cifra-destacada">
          <span>Cuota proyectada</span>
          <strong>{money.format(result.projectedInstallment)}</strong>
        </div>
        <div className="bq-cifra">
          <span>Cuota máxima admisible</span>
          <strong>{money.format(result.maxAllowedInstallment)}</strong>
        </div>
        <div className="bq-cifra">
          <span>Remanente mensual</span>
          <strong>{money.format(result.monthlySurplus)}</strong>
        </div>
      </div>

      <div className="bq-datos">
        <div className="bq-dato"><span>Solicitud N.º</span><span>{result.id}</span></div>
        <div className="bq-dato"><span>Fecha de la solicitud</span><span>{formatDate(result.applicationDate)}</span></div>
        <div className="bq-dato"><span>TEA referencial</span><span>{rate.format(result.referenceEffectiveAnnualRate)}</span></div>
        {result.surplusPercentage != null && (
          <div className="bq-dato"><span>Cuota sobre el remanente</span><span>{number.format(result.surplusPercentage)} %</span></div>
        )}
      </div>

      {rejected ? (
        <div className="bq-alerta bq-alerta-peligro">
          <strong>La solicitud no cumple la evaluación de capacidad de pago</strong>
          <p>{result.rejectionReason}</p>
        </div>
      ) : (
        <div className="bq-alerta bq-alerta-info">
          <strong>Tu solicitud fue registrada</strong>
          <p>Un analista la revisará y asignará la tasa definitiva.</p>
        </div>
      )}
    </section>
  )
}
