import { useEffect, useState } from 'react'
import { findCreditLimits } from '../services/loanApplicationsApi.js'

const dollars = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })

function hasMoreThanTwoDecimals(value) {
  const decimals = value.split('.')[1]
  return decimals !== undefined && decimals.length > 2
}

export function amountError(value, limits) {
  if (value === '') {
    return ''
  }
  const amount = Number(value)
  if (Number.isNaN(amount) || amount <= 0) {
    return 'Ingresa un monto mayor a cero.'
  }
  if (hasMoreThanTwoDecimals(value)) {
    return 'El monto admite máximo 2 decimales.'
  }
  if (limits && amount > limits.maxAmount) {
    return `Excede el monto máximo de ${dollars.format(limits.maxAmount)}.`
  }
  if (limits && amount < limits.minAmount) {
    return `El monto mínimo es ${dollars.format(limits.minAmount)}.`
  }
  return ''
}

export function termError(value, limits) {
  if (value === '') {
    return ''
  }
  const term = Number(value)
  if (!Number.isInteger(term) || term <= 0) {
    return 'El plazo debe ser un número entero de meses.'
  }
  if (limits && term > limits.maxTermMonths) {
    return `Excede el plazo máximo de ${limits.maxTermMonths} meses.`
  }
  if (limits && term < limits.minTermMonths) {
    return `El plazo mínimo es ${limits.minTermMonths} meses.`
  }
  return ''
}

export function incomeError(value) {
  if (value === '') {
    return ''
  }
  const income = Number(value)
  if (Number.isNaN(income) || income <= 0) {
    return 'Los ingresos deben ser mayores a cero.'
  }
  return hasMoreThanTwoDecimals(value) ? 'Los ingresos admiten máximo 2 decimales.' : ''
}

export function expensesError(value) {
  if (value === '') {
    return ''
  }
  const expenses = Number(value)
  if (Number.isNaN(expenses) || expenses < 0) {
    return 'Los gastos no pueden ser negativos.'
  }
  return hasMoreThanTwoDecimals(value) ? 'Los gastos admiten máximo 2 decimales.' : ''
}

export function amountHelp(limits) {
  return limits ? `Entre ${dollars.format(limits.minAmount)} y ${dollars.format(limits.maxAmount)}.` : ''
}

export function termHelp(limits) {
  return limits ? `Entre ${limits.minTermMonths} y ${limits.maxTermMonths} meses.` : ''
}

export function useCreditLimits() {
  const [limits, setLimits] = useState(null)

  useEffect(() => {
    findCreditLimits().then(setLimits).catch(() => setLimits(null))
  }, [])

  return limits
}
