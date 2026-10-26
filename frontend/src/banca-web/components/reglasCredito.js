import { useEffect, useState } from 'react'
import { findCreditLimits } from '../services/solicitudesCreditoApi.js'

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
  if (limits && amount > limits.montoMaximo) {
    return `Excede el monto máximo de ${dollars.format(limits.montoMaximo)}.`
  }
  if (limits && amount < limits.montoMinimo) {
    return `El monto mínimo es ${dollars.format(limits.montoMinimo)}.`
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
  if (limits && term > limits.plazoMaximoMeses) {
    return `Excede el plazo máximo de ${limits.plazoMaximoMeses} meses.`
  }
  if (limits && term < limits.plazoMinimoMeses) {
    return `El plazo mínimo es ${limits.plazoMinimoMeses} meses.`
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
  return limits ? `Entre ${dollars.format(limits.montoMinimo)} y ${dollars.format(limits.montoMaximo)}.` : ''
}

export function termHelp(limits) {
  return limits ? `Entre ${limits.plazoMinimoMeses} y ${limits.plazoMaximoMeses} meses.` : ''
}

export function useCreditLimits() {
  const [limits, setLimits] = useState(null)

  useEffect(() => {
    findCreditLimits().then(setLimits).catch(() => setLimits(null))
  }, [])

  return limits
}
