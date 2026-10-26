export default function CampoFormulario({ id, label, error, help, ...inputProps }) {
  const message = error || help

  return (
    <div className={error ? 'bq-campo bq-error' : 'bq-campo'}>
      <label className="bq-label" htmlFor={id}>
        {label} <span className="bq-obligatorio">*</span>
      </label>
      <input className="bq-input" id={id} aria-invalid={Boolean(error)} aria-describedby={message ? `${id}Message` : undefined} {...inputProps} />
      {message ? (
        <span className="bq-ayuda" id={`${id}Message`} role={error ? 'alert' : undefined}>
          {message}
        </span>
      ) : null}
    </div>
  )
}
