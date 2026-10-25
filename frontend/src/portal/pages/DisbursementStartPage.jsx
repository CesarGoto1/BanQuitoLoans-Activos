import { useState } from 'react'
import DisbursementPage from './DisbursementPage.jsx'

export default function DisbursementStartPage() {
  const [applicationNumber, setApplicationNumber] = useState('')
  const [applicationId, setApplicationId] = useState(null)

  function handleSubmit(event) {
    event.preventDefault()
    setApplicationId(Number(applicationNumber))
  }

  return (
    <>
      <main className="bq-pagina">
        <div className="bq-titulo-pagina">
          <div>
            <h1>Desembolsar crédito</h1>
            <p className="bq-pequeno bq-texto-2">Escribe el número de una solicitud aprobada.</p>
          </div>
        </div>
        <form className="bq-tarjeta bq-formulario" onSubmit={handleSubmit}>
          <div className="bq-campo">
            <label className="bq-label" htmlFor="applicationNumber">
              Número de solicitud <span className="bq-obligatorio">*</span>
            </label>
            <input
              id="applicationNumber"
              className="bq-input"
              type="text"
              inputMode="numeric"
              value={applicationNumber}
              onChange={(event) => setApplicationNumber(event.target.value.replace(/\D/g, ''))}
            />
          </div>
          <div className="bq-acciones">
            <button type="submit" className="bq-btn bq-btn-primario" disabled={!applicationNumber}>
              Buscar
            </button>
          </div>
        </form>
      </main>
      {applicationId && <DisbursementPage key={applicationId} applicationId={applicationId} />}
    </>
  )
}
