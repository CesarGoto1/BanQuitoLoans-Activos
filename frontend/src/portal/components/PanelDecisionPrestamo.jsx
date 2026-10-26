import { useRef, useState } from 'react';
import { approveLoanApplication, rejectLoanApplication, obtenerDetalleSolicitud } from '../services/prestamosApi.js';

export default function PanelDecisionPrestamo({ application, onResolved }) {
  const [annualEffectiveRate, setAnnualEffectiveRate] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [confirmRejection, setConfirmRejection] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [requiresRefresh, setRequiresRefresh] = useState(false);
  const sending = useRef(false);
  const resolved = application.estadoSolicitud !== 'REGISTRADA';
  const disabled = busy || resolved || requiresRefresh;
  const money = (value) => new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' }).format(value);

  async function refresh() {
    setBusy(true);
    try {
      const detail = await obtenerDetalleSolicitud(application.idSolicitud);
      onResolved(detail);
      setRequiresRefresh(false);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setBusy(false);
    }
  }

  async function submit(event, decision) {
    event.preventDefault();
    if (sending.current || disabled) return;
    if (decision === 'reject' && (!rejectionReason.trim() || rejectionReason.length > 200)) {
      setError('Escribe un motivo de rechazo de hasta 200 caracteres.');
      return;
    }
    if (decision === 'reject' && !confirmRejection) {
      setConfirmRejection(true);
      return;
    }
    sending.current = true;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      const result = decision === 'approve'
        ? await approveLoanApplication(application.idSolicitud, Number(annualEffectiveRate))
        : await rejectLoanApplication(application.idSolicitud, rejectionReason.trim());
      onResolved(result);
      setSuccess(decision === 'approve' ? 'Solicitud aprobada correctamente.' : 'Solicitud rechazada correctamente.');
    } catch (failure) {
      setError(failure.message);
      if (!failure.status || failure.status >= 500 || failure.status === 409) {
        setRequiresRefresh(true);
        try {
          onResolved(await obtenerDetalleSolicitud(application.idSolicitud));
          setRequiresRefresh(false);
        } catch {
          setRequiresRefresh(true);
        }
      }
    } finally {
      sending.current = false;
      setBusy(false);
      setConfirmRejection(false);
    }
  }

  return (
    <section id="contenedor-resolucion-p6" className="bq-tarjeta" aria-labelledby="decision-title" aria-busy={busy}>
      <h2 id="decision-title">Resolución del analista</h2>
      {error && <div className="bq-alerta bq-alerta-peligro" role="alert"><strong>No se pudo completar la resolución</strong><p>{error}</p></div>}
      {success && <div className="bq-alerta bq-alerta-exito" role="status">{success}</div>}
      {requiresRefresh && <button className="bq-btn bq-btn-secundario" type="button" disabled={busy} onClick={refresh}>Actualizar estado</button>}
      {resolved ? (
        <div className="bq-datos">
          <p>La solicitud ya fue resuelta y no admite nuevas decisiones.</p>
          <div className="bq-dato"><span>Fecha de resolución</span><strong>{application.fechaResolucion}</strong></div>
          <div className="bq-dato"><span>Analista</span><strong>{application.idAnalista}</strong></div>
          {application.estadoSolicitud === 'APROBADA' ? <>
            <div className="bq-dato"><span>Tasa efectiva anual</span><strong>{new Intl.NumberFormat('es-EC', { style: 'percent', maximumFractionDigits: 2 }).format(application.teaAsignada)}</strong></div>
            <div className="bq-dato"><span>Cuota definitiva de evaluación</span><strong>{money(application.cuotaEvaluacionDefinitiva)}</strong></div>
          </> : <div className="bq-dato"><span>Motivo del rechazo</span><strong>{application.motivoRechazo}</strong></div>}
        </div>
      ) : (
        <div className="bq-fila">
          <form className="bq-formulario" onSubmit={(event) => submit(event, 'approve')}>
            <h3>Aprobar solicitud</h3>
            <div className="bq-campo">
              <label className="bq-label" htmlFor="annual-effective-rate">Tasa efectiva anual (fracción decimal)</label>
              <input id="annual-effective-rate" className="bq-input" type="number" inputMode="decimal"
                min="0.0001" max="9.9999" step="0.0001" required disabled={disabled}
                aria-describedby="annual-effective-rate-help" value={annualEffectiveRate}
                onChange={(event) => setAnnualEffectiveRate(event.target.value)} />
              <p id="annual-effective-rate-help" className="bq-ayuda">Ejemplo: 0,1650 equivale a 16,50 %. El servidor valida el máximo vigente y recalcula la cuota con seguro.</p>
            </div>
            <button className="bq-btn bq-btn-primario" type="submit" disabled={disabled}>{busy ? 'Procesando…' : 'Aprobar solicitud'}</button>
          </form>
          <form className="bq-formulario" onSubmit={(event) => submit(event, 'reject')}>
            <h3>Rechazar solicitud</h3>
            <div className="bq-campo">
              <label className="bq-label" htmlFor="rejection-reason">Motivo del rechazo</label>
              <textarea id="rejection-reason" className="bq-textarea" required maxLength={200}
                disabled={disabled} value={rejectionReason} aria-describedby="rejection-reason-help"
                onChange={(event) => { setRejectionReason(event.target.value); setConfirmRejection(false); }} />
              <p id="rejection-reason-help" className="bq-ayuda">{rejectionReason.length}/200 caracteres</p>
            </div>
            {confirmRejection && <div className="bq-alerta bq-alerta-aviso" role="alert">Confirma el rechazo. Una solicitud resuelta no puede modificarse.</div>}
            <div className="bq-acciones">
              {confirmRejection && <button className="bq-btn bq-btn-secundario" type="button" disabled={disabled} onClick={() => setConfirmRejection(false)}>Cancelar</button>}
              <button className="bq-btn bq-btn-peligro" type="submit" disabled={disabled}>{confirmRejection ? 'Confirmar rechazo' : 'Rechazar solicitud'}</button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
