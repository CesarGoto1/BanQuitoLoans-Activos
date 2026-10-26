import { useState } from 'react'
import {
  buscarPrestamo,
  buscarPrestamosDelCliente,
  buscarTablaAmortizacion,
} from '../services/consultaOperacionApi.js'

const dolares = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })
const numero = new Intl.NumberFormat('es-EC', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const porcentaje = new Intl.NumberFormat('es-EC', { style: 'percent', minimumFractionDigits: 2 })
const sistemasAmortizacion = { FRANCES: 'Francés', ALEMAN: 'Alemán' }

const estadosCartera = {
  POR_VENCER: { texto: 'Por vencer', clase: 'bq-estado-info' },
  VENCIDA: { texto: 'Vencida', clase: 'bq-estado-aviso' },
  EN_LITIGIO: { texto: 'En litigio', clase: 'bq-estado-peligro' },
  CASTIGADA: { texto: 'Castigada', clase: 'bq-estado-peligro' },
  CANCELADA: { texto: 'Cancelada', clase: 'bq-estado-neutro' },
}

const estadosCuota = {
  PENDIENTE: { texto: 'Pendiente', clase: 'bq-estado-neutro' },
  PAGO_PARCIAL: { texto: 'Pago parcial', clase: 'bq-estado-aviso' },
  PAGADA: { texto: 'Pagada', clase: 'bq-estado-exito' },
}

const modosBusqueda = {
  NUMERO_OPERACION: { texto: 'Número de operación', ejemplo: 'OP-20261031-000001' },
  CEDULA: { texto: 'Cédula del cliente', ejemplo: '1712345678' },
}

function formatearFecha(iso) {
  const [anio, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${anio}`
}

export default function ConsultaOperacionPagina() {
  const [modoBusqueda, setModoBusqueda] = useState('NUMERO_OPERACION')
  const [textoBusqueda, setTextoBusqueda] = useState('')
  const [estadoCartera, setEstadoCartera] = useState('')
  const [error, setError] = useState('')
  const [buscando, setBuscando] = useState(false)
  const [prestamosDelCliente, setPrestamosDelCliente] = useState(null)
  const [prestamo, setPrestamo] = useState(null)
  const [tablaAmortizacion, setTablaAmortizacion] = useState(null)

  async function ejecutar(accion) {
    setBuscando(true)
    setError('')
    try {
      await accion()
    } catch (e) {
      setError(e.message)
    } finally {
      setBuscando(false)
    }
  }

  async function abrirPrestamo(numeroOperacion) {
    const [prestamoEncontrado, tabla] = await Promise.all([
      buscarPrestamo(numeroOperacion),
      buscarTablaAmortizacion(numeroOperacion, 0),
    ])
    setPrestamo(prestamoEncontrado)
    setTablaAmortizacion(tabla)
  }

  async function buscarCliente(pagina) {
    setPrestamosDelCliente(await buscarPrestamosDelCliente(textoBusqueda.trim(), estadoCartera, pagina))
  }

  function buscar(evento) {
    evento.preventDefault()
    setPrestamo(null)
    setTablaAmortizacion(null)
    setPrestamosDelCliente(null)
    if (modoBusqueda === 'CEDULA' && !/^\d{10}$/.test(textoBusqueda.trim())) {
      setError('La cédula debe tener 10 dígitos.')
      return
    }
    ejecutar(() => (modoBusqueda === 'NUMERO_OPERACION' ? abrirPrestamo(textoBusqueda.trim()) : buscarCliente(0)))
  }

  function cambiarModoBusqueda(modo) {
    setModoBusqueda(modo)
    setTextoBusqueda('')
    setError('')
  }

  return (
    <main className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div>
          <h1>Consulta de operación</h1>
          <p className="bq-pequeno bq-texto-2">Busca un préstamo por su número de operación o por la cédula del cliente.</p>
        </div>
      </div>

      <form className="bq-tarjeta bq-formulario" onSubmit={buscar}>
        <div className="bq-segmentado" role="group" aria-label="Buscar por">
          {Object.entries(modosBusqueda).map(([modo, { texto }]) => (
            <button key={modo} type="button" aria-pressed={modoBusqueda === modo} onClick={() => cambiarModoBusqueda(modo)}>
              {texto}
            </button>
          ))}
        </div>

        <div className="bq-fila">
          <div className="bq-campo">
            <label className="bq-label" htmlFor="textoBusqueda">
              {modosBusqueda[modoBusqueda].texto} <span className="bq-obligatorio">*</span>
            </label>
            <input
              className="bq-input bq-buscador bq-mono"
              id="textoBusqueda"
              value={textoBusqueda}
              onChange={(e) => setTextoBusqueda(e.target.value)}
              placeholder={modosBusqueda[modoBusqueda].ejemplo}
              inputMode={modoBusqueda === 'CEDULA' ? 'numeric' : undefined}
              required
            />
          </div>

          {modoBusqueda === 'CEDULA' ? (
            <div className="bq-campo">
              <label className="bq-label" htmlFor="estadoCartera">Estado de cartera</label>
              <select
                className="bq-select"
                id="estadoCartera"
                value={estadoCartera}
                onChange={(e) => setEstadoCartera(e.target.value)}
              >
                <option value="">Todos</option>
                {Object.entries(estadosCartera).map(([estado, { texto }]) => (
                  <option key={estado} value={estado}>{texto}</option>
                ))}
              </select>
            </div>
          ) : null}
        </div>

        {error ? (
          <div className="bq-alerta bq-alerta-peligro" role="alert">
            <strong>No se pudo consultar</strong>
            <p>{error}</p>
          </div>
        ) : null}

        <div className="bq-acciones">
          <button className="bq-btn bq-btn-primario" type="submit" disabled={buscando || textoBusqueda.trim() === ''}>
            {buscando ? 'Buscando…' : 'Buscar'}
          </button>
        </div>
      </form>

      {prestamosDelCliente && !prestamo ? (
        <PrestamosDelCliente
          prestamos={prestamosDelCliente}
          alAbrir={(numeroOperacion) => ejecutar(() => abrirPrestamo(numeroOperacion))}
          alCambiarPagina={(pagina) => ejecutar(() => buscarCliente(pagina))}
        />
      ) : null}

      {prestamo ? (
        <>
          {prestamosDelCliente ? (
            <div className="bq-acciones">
              <button className="bq-btn bq-btn-texto" type="button" onClick={() => setPrestamo(null)}>
                ← Volver a los préstamos del cliente
              </button>
            </div>
          ) : null}
          <DetallePrestamo prestamo={prestamo} />
          <TablaAmortizacion
            tabla={tablaAmortizacion}
            alCambiarPagina={(pagina) =>
              ejecutar(async () => setTablaAmortizacion(await buscarTablaAmortizacion(prestamo.numeroOperacion, pagina)))
            }
          />
        </>
      ) : null}
    </main>
  )
}

function PrestamosDelCliente({ prestamos, alAbrir, alCambiarPagina }) {
  if (prestamos.content.length === 0) {
    return (
      <div className="bq-alerta bq-alerta-info" role="status">
        <strong>Sin préstamos</strong>
        <p>El cliente no tiene préstamos con ese criterio.</p>
      </div>
    )
  }

  return (
    <div className="bq-tarjeta">
      <span className="bq-etiqueta-seccion">Préstamos del cliente</span>
      <div className="bq-tabla-contenedor">
        <table className="bq-tabla">
          <thead>
            <tr>
              <th>Operación</th>
              <th>Desembolso</th>
              <th className="bq-num">Monto aprobado</th>
              <th className="bq-num">Saldo de capital</th>
              <th className="bq-num">Días de mora</th>
              <th>Estado</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {prestamos.content.map((prestamo) => (
              <tr key={prestamo.numeroOperacion}>
                <td className="bq-mono">{prestamo.numeroOperacion}</td>
                <td>{formatearFecha(prestamo.fechaDesembolso)}</td>
                <td className="bq-num">{numero.format(prestamo.montoAprobado)}</td>
                <td className="bq-num">{numero.format(prestamo.saldoCapital)}</td>
                <td className="bq-num">{prestamo.diasMora}</td>
                <td><EstadoCartera estado={prestamo.estadoCartera} /></td>
                <td>
                  <button
                    className="bq-btn bq-btn-secundario bq-btn-sm"
                    type="button"
                    onClick={() => alAbrir(prestamo.numeroOperacion)}
                  >
                    Ver detalle
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Paginacion pagina={prestamos.page} alCambiarPagina={alCambiarPagina} />
    </div>
  )
}

function DetallePrestamo({ prestamo }) {
  return (
    <div className="bq-tarjeta">
      <div className="bq-encabezado-tarjeta">
        <span className="bq-titulo-tarjeta">
          Operación <span className="bq-mono">{prestamo.numeroOperacion}</span>
        </span>
        <EstadoCartera estado={prestamo.estadoCartera} />
      </div>

      <span className="bq-etiqueta-seccion">Situación actual</span>
      <div className="bq-fila">
        <Dato etiqueta="Saldo de capital" valor={dolares.format(prestamo.saldoCapital)} monto destacado />
        <Dato etiqueta="Días de mora" valor={prestamo.diasMora} />
        <Dato etiqueta="Cédula del cliente" valor={prestamo.cedulaCliente} mono />
      </div>

      <span className="bq-etiqueta-seccion">Condiciones del préstamo</span>
      <div className="bq-fila">
        <Dato etiqueta="Monto aprobado" valor={dolares.format(prestamo.montoAprobado)} monto />
        <Dato etiqueta="Tasa efectiva anual (TEA)" valor={porcentaje.format(prestamo.tasaEfectivaAnual)} />
        <Dato etiqueta="Plazo" valor={`${prestamo.plazoMeses} meses`} />
        <Dato etiqueta="Sistema de amortización" valor={sistemasAmortizacion[prestamo.sistemaAmortizacion]} />
        <Dato etiqueta="Fecha de desembolso" valor={formatearFecha(prestamo.fechaDesembolso)} />
        <Dato etiqueta="Solicitud" valor={prestamo.idSolicitud} />
      </div>
    </div>
  )
}

function TablaAmortizacion({ tabla, alCambiarPagina }) {
  const cuotas = tabla.content

  return (
    <div className="bq-tarjeta">
      <span className="bq-etiqueta-seccion">Tabla de amortización vigente</span>
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
              <th>Estado</th>
            </tr>
          </thead>
          <tbody>
            {cuotas.map((cuota) => (
              <tr key={cuota.numeroCuota}>
                <td className="bq-num">{cuota.numeroCuota}</td>
                <td>{formatearFecha(cuota.fechaVencimiento)}</td>
                <td className="bq-num">{numero.format(cuota.valorCapital)}</td>
                <td className="bq-num">{numero.format(cuota.valorInteres)}</td>
                <td className="bq-num">{numero.format(cuota.valorSeguro)}</td>
                <td className="bq-num">{numero.format(cuota.valorCuota)}</td>
                <td className="bq-num">{numero.format(cuota.saldoCapitalPendiente)}</td>
                <td>
                  <span className={`bq-estado ${estadosCuota[cuota.estadoCuota]?.clase ?? 'bq-estado-neutro'}`}>
                    {estadosCuota[cuota.estadoCuota]?.texto ?? cuota.estadoCuota}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Paginacion pagina={tabla.page} alCambiarPagina={alCambiarPagina} />
    </div>
  )
}

function Paginacion({ pagina, alCambiarPagina }) {
  if (pagina.totalPages <= 1) {
    return null
  }

  return (
    <div className="bq-acciones">
      <span className="bq-pequeno bq-texto-2">
        Página {pagina.number + 1} de {pagina.totalPages} · {pagina.totalElements} registros
      </span>
      <button
        className="bq-btn bq-btn-secundario bq-btn-sm"
        type="button"
        disabled={pagina.number === 0}
        onClick={() => alCambiarPagina(pagina.number - 1)}
      >
        Anterior
      </button>
      <button
        className="bq-btn bq-btn-secundario bq-btn-sm"
        type="button"
        disabled={pagina.number + 1 >= pagina.totalPages}
        onClick={() => alCambiarPagina(pagina.number + 1)}
      >
        Siguiente
      </button>
    </div>
  )
}

function EstadoCartera({ estado }) {
  const { texto, clase } = estadosCartera[estado] ?? { texto: estado, clase: 'bq-estado-neutro' }
  return <span className={`bq-estado ${clase}`}>{texto}</span>
}

function Dato({ etiqueta, valor, monto = false, destacado = false, mono = false }) {
  const clase = [monto ? 'bq-monto' : null, mono ? 'bq-mono' : null].filter(Boolean).join(' ')
  return (
    <div className="bq-campo">
      <span className="bq-label">{etiqueta}</span>
      {destacado ? <strong className={clase || undefined}>{valor}</strong> : <span className={clase || undefined}>{valor}</span>}
    </div>
  )
}
