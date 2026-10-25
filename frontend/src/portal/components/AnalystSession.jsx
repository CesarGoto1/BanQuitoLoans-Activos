import { useState } from 'react';
import { setAnalystIdentification } from '../servicios/prestamosApi.js';

export default function AnalystSession({ onContinue }) {
  const [identification, setIdentification] = useState('');

  function submit(event) {
    event.preventDefault();
    setAnalystIdentification(identification);
    onContinue(identification.trim());
  }

  return (
    <div className="bq-pagina">
      <form className="bq-tarjeta bq-formulario" onSubmit={submit}>
        <h1>Acceso del analista</h1>
        <p>Ingresa la identificación de un analista registrado. Este portal utiliza acceso simulado.</p>
        <div className="bq-campo">
          <label className="bq-label" htmlFor="analyst-identification">Identificación del analista</label>
          <input id="analyst-identification" className="bq-input" inputMode="numeric"
            pattern="[0-9]{10}" minLength={10} maxLength={10} required autoComplete="off"
            value={identification} onChange={(event) => setIdentification(event.target.value)} />
          <p className="bq-ayuda">Debe contener 10 dígitos.</p>
        </div>
        <div className="bq-acciones"><button className="bq-btn bq-btn-primario" type="submit">Continuar</button></div>
      </form>
    </div>
  );
}
