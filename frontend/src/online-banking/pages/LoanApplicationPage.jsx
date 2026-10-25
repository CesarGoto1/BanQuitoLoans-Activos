import { useState } from 'react'
import EvaluationResult from '../components/EvaluationResult.jsx'
import LoanApplicationForm from '../components/LoanApplicationForm.jsx'
import { registerLoanApplication } from '../services/loanApplicationsApi.js'

export default function LoanApplicationPage() {
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)

  const register = async (application) => {
    setSending(true)
    setError(null)
    setResult(null)
    try {
      setResult(await registerLoanApplication(application))
    } catch (e) {
      setError(e.message)
    } finally {
      setSending(false)
    }
  }

  return (
    <main className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div>
          <h1>Solicita tu préstamo</h1>
          <p>Registra tu solicitud y conoce al instante si cumple la evaluación de capacidad de pago.</p>
        </div>
      </div>

      {error && (
        <div className="bq-alerta bq-alerta-peligro">
          <strong>No se pudo registrar la solicitud</strong>
          <p>{error}</p>
        </div>
      )}

      <div className="bq-fila">
        <LoanApplicationForm onSubmit={register} sending={sending} />
        {result && <EvaluationResult result={result} />}
      </div>
    </main>
  )
}
