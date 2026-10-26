const SESSION_KEY = 'bq-analyst-session'

export function saveAnalystSession(analyst) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(analyst))
}

export function getAnalystSession() {
  const raw = sessionStorage.getItem(SESSION_KEY)
  return raw ? JSON.parse(raw) : null
}

export function clearAnalystSession() {
  sessionStorage.removeItem(SESSION_KEY)
}
