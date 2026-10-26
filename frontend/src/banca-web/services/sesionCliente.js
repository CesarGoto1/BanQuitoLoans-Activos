const SESSION_KEY = 'bq-customer-session'

export function saveCustomerSession(customer) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(customer))
}

export function getCustomerSession() {
  const raw = sessionStorage.getItem(SESSION_KEY)
  return raw ? JSON.parse(raw) : null
}

export function clearCustomerSession() {
  sessionStorage.removeItem(SESSION_KEY)
}

export function receiveCustomerFromUrl() {
  const params = new URLSearchParams(window.location.search)
  const idCliente = Number(params.get('idCliente'))
  if (idCliente) {
    saveCustomerSession({
      idCliente,
      cedulaCliente: params.get('cedulaCliente'),
      nombresCliente: params.get('nombresCliente'),
      apellidosCliente: params.get('apellidosCliente'),
    })
    window.history.replaceState(null, '', window.location.pathname)
  }
  return getCustomerSession()
}
