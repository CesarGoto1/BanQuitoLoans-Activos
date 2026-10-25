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
  const customerId = Number(params.get('customerId'))
  if (customerId) {
    saveCustomerSession({
      customerId,
      nationalId: params.get('nationalId'),
      firstNames: params.get('firstNames'),
      lastNames: params.get('lastNames'),
    })
    window.history.replaceState(null, '', window.location.pathname)
  }
  return getCustomerSession()
}
