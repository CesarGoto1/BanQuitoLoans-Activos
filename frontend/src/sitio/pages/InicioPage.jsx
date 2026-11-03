import { useEffect, useRef, useState } from 'react'

const URL_BANCA_WEB = import.meta.env.VITE_URL_PASIVOS ?? 'http://localhost:5174'
const URL_PORTAL = '/portal'

const SOLUCIONES = [
  {
    titulo: 'Simula tu crédito',
    detalle: 'Conoce tu cuota en segundos, en francés o alemán.',
    accion: 'Simular ahora',
    icono: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="4" y="2" width="16" height="20" rx="2" />
        <path d="M8 6h8M8 11h.01M12 11h.01M16 11h.01M8 15h.01M12 15h.01M16 15h.01M8 18h.01M12 18h4" />
      </svg>
    ),
  },
  {
    titulo: 'Solicita en línea',
    detalle: 'Envía tu solicitud y conoce al instante tu capacidad de pago.',
    accion: 'Solicitar crédito',
    icono: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
        <path d="M14 2v6h6M9 15l2 2 4-4" />
      </svg>
    ),
  },
  {
    titulo: 'Consulta tus cuentas',
    detalle: 'Revisa tu saldo y cada movimiento cuando quieras.',
    accion: 'Ver mis cuentas',
    icono: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M20 7H5a2 2 0 0 1 0-4h13v4" />
        <path d="M3 5v14a2 2 0 0 0 2 2h15V7" />
        <path d="M16 14h.01" />
      </svg>
    ),
  },
  {
    titulo: 'Sigue tus cuotas',
    detalle: 'Mira tu tabla de amortización y cada pago aplicado.',
    accion: 'Ver mis operaciones',
    icono: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M3 3v18h18" />
        <path d="M8 17v-4M12 17V9M16 17V6" />
      </svg>
    ),
  },
]

const PASOS = [
  { numero: '1', titulo: 'Simula', detalle: 'Elige el monto, el plazo y el sistema de amortización que prefieras.' },
  { numero: '2', titulo: 'Solicita', detalle: 'Envía tu solicitud desde la Banca Web. Un analista la revisa por ti.' },
  { numero: '3', titulo: 'Recibe tu dinero', detalle: 'Al aprobarse, el monto se acredita directo en tu cuenta BanQuito.' },
]

function MenuAcceso() {
  const [abierto, establecerAbierto] = useState(false)
  const contenedor = useRef(null)

  useEffect(() => {
    if (!abierto) {
      return undefined
    }
    function cerrarFuera(evento) {
      if (contenedor.current && !contenedor.current.contains(evento.target)) {
        establecerAbierto(false)
      }
    }
    function cerrarConEscape(evento) {
      if (evento.key === 'Escape') {
        establecerAbierto(false)
      }
    }
    document.addEventListener('mousedown', cerrarFuera)
    document.addEventListener('keydown', cerrarConEscape)
    return () => {
      document.removeEventListener('mousedown', cerrarFuera)
      document.removeEventListener('keydown', cerrarConEscape)
    }
  }, [abierto])

  return (
    <div className="bq-sitio-acceso" ref={contenedor}>
      <button
        type="button"
        className="bq-btn bq-btn-primario"
        aria-haspopup="true"
        aria-expanded={abierto}
        onClick={() => establecerAbierto((actual) => !actual)}
      >
        Acceso clientes
        <svg className="bq-sitio-flecha" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="m6 9 6 6 6-6" />
        </svg>
      </button>
      {abierto && (
        <div className="bq-sitio-acceso-menu" role="menu">
          <a role="menuitem" href={URL_BANCA_WEB}>
            <strong>Banca Web</strong>
            <small>Cuentas, simulación y créditos</small>
          </a>
          <a role="menuitem" href={URL_PORTAL}>
            <strong>Portal del analista</strong>
            <small>Uso interno de BanQuito</small>
          </a>
        </div>
      )}
    </div>
  )
}

export default function InicioPage() {
  return (
    <div className="bq-sitio">
      <header className="bq-sitio-cabecera">
        <a className="bq-marca" href="/">
          <img className="bq-logo" src="/logo-bq.png" alt="" />
          <span className="bq-marca-nombre">BanQuito</span>
        </a>
        <nav className="bq-sitio-nav" aria-label="Secciones">
          <a href="#soluciones">Soluciones</a>
          <a href="#credito">Crédito de consumo</a>
          <a href="#pasos">Cómo funciona</a>
        </nav>
        <div className="bq-sitio-cabecera-acciones">
          <a className="bq-sitio-enlace" href={URL_BANCA_WEB}>Banca Web</a>
          <MenuAcceso />
        </div>
      </header>

      <main>
        <section className="bq-sitio-heroe">
          <div className="bq-sitio-heroe-texto">
            <span className="bq-ingreso-etiqueta">Crédito de consumo</span>
            <h1>
              Tu crédito, <em>cuota a cuota.</em>
            </h1>
            <p>
              Simula, solicita y sigue tu préstamo desde la Banca Web. Sin filas y con respuesta inmediata sobre tu
              capacidad de pago.
            </p>
            <div className="bq-sitio-heroe-acciones">
              <a className="bq-btn bq-btn-primario bq-sitio-btn-grande" href={URL_BANCA_WEB}>
                Simula tu crédito
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M5 12h14M13 6l6 6-6 6" />
                </svg>
              </a>
              <a className="bq-btn bq-sitio-btn-claro bq-sitio-btn-grande" href="#credito">
                Conoce el crédito
              </a>
            </div>
          </div>
          <div className="bq-sitio-ejemplo" aria-hidden="true">
            <div className="bq-sitio-ejemplo-cabecera">
              <span>Simulación referencial</span>
              <span className="bq-sitio-ejemplo-chip">Francés</span>
            </div>
            <div className="bq-sitio-ejemplo-monto">
              <small>Cuota mensual</small>
              <strong>$906,21</strong>
            </div>
            <div className="bq-sitio-ejemplo-barras">
              {Array.from({ length: 12 }, (_, indice) => (
                <span key={indice} data-pagada={indice < 4} />
              ))}
            </div>
            <dl className="bq-sitio-ejemplo-datos">
              <div>
                <dt>Monto</dt>
                <dd>$10.000,00</dd>
              </div>
              <div>
                <dt>Plazo</dt>
                <dd>12 meses</dd>
              </div>
              <div>
                <dt>TEA</dt>
                <dd>15,50 %</dd>
              </div>
            </dl>
          </div>
        </section>

        <section className="bq-sitio-seccion" id="soluciones">
          <div className="bq-sitio-titulo">
            <h2>Soluciones en línea</h2>
            <p>Todo lo que necesitas, sin salir de casa.</p>
          </div>
          <div className="bq-sitio-tarjetas">
            {SOLUCIONES.map((solucion) => (
              <a key={solucion.titulo} className="bq-sitio-tarjeta" href={URL_BANCA_WEB}>
                <span className="bq-sitio-tarjeta-icono" aria-hidden="true">{solucion.icono}</span>
                <h3>{solucion.titulo}</h3>
                <p>{solucion.detalle}</p>
                <span className="bq-sitio-tarjeta-accion">{solucion.accion} →</span>
              </a>
            ))}
          </div>
        </section>

        <section className="bq-sitio-seccion bq-sitio-credito" id="credito">
          <div className="bq-sitio-credito-texto">
            <span className="bq-sitio-sobretitulo">Crédito de consumo BanQuito</span>
            <h2>Un préstamo a tu medida, con reglas claras.</h2>
            <p>
              Elige cuánto necesitas y en cuánto tiempo pagarlo. Antes de solicitar, verás tu cuota, los intereses, el
              seguro de desgravamen y la contribución SOLCA.
            </p>
            <a className="bq-btn bq-btn-secundario" href={URL_BANCA_WEB}>
              Ingresar a la Banca Web
            </a>
          </div>
          <div className="bq-sitio-cifras">
            <div>
              <small>Monto</small>
              <strong>$500 a $30.000</strong>
            </div>
            <div>
              <small>Plazo</small>
              <strong>6 a 60 meses</strong>
            </div>
            <div>
              <small>TEA referencial</small>
              <strong>15,50 %</strong>
            </div>
            <div>
              <small>Amortización</small>
              <strong>Francés o alemán</strong>
            </div>
          </div>
        </section>

        <section className="bq-sitio-seccion" id="pasos">
          <div className="bq-sitio-titulo">
            <h2>Cómo funciona</h2>
            <p>Tres pasos y el dinero llega a tu cuenta.</p>
          </div>
          <ol className="bq-sitio-pasos">
            {PASOS.map((paso) => (
              <li key={paso.numero}>
                <span className="bq-sitio-paso-numero">{paso.numero}</span>
                <h3>{paso.titulo}</h3>
                <p>{paso.detalle}</p>
              </li>
            ))}
          </ol>
        </section>

        <section className="bq-sitio-seccion">
          <div className="bq-sitio-banda">
            <span className="bq-sitio-tarjeta-icono" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="7" width="18" height="13" rx="2" />
                <path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M3 13h18" />
              </svg>
            </span>
            <div>
              <h3>¿Eres analista de BanQuito?</h3>
              <p>Gestiona solicitudes, desembolsos y el cierre del día desde el portal interno.</p>
            </div>
            <a className="bq-btn bq-sitio-btn-oscuro" href={URL_PORTAL}>
              Ir al portal
            </a>
          </div>
        </section>
      </main>

      <footer className="bq-sitio-pie">
        <div className="bq-sitio-pie-contenido">
          <div className="bq-sitio-pie-marca">
            <span className="bq-marca">
              <img className="bq-logo" src="/logo-bq.png" alt="" />
              <span className="bq-marca-nombre">BanQuito</span>
            </span>
            <p>Tu crédito, cuota a cuota.</p>
          </div>
          <div className="bq-sitio-pie-enlaces">
            <a href={URL_BANCA_WEB}>Banca Web</a>
            <a href={URL_PORTAL}>Portal del analista</a>
            <a href="#credito">Crédito de consumo</a>
          </div>
        </div>
        <p className="bq-sitio-pie-legal">© 2026 Banco BanQuito. Todos los derechos reservados.</p>
      </footer>
    </div>
  )
}
