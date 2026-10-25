import { getAnalystSession } from './analystSession.js'

const USE_SAMPLE_DATA = false

const API = import.meta.env.VITE_API ?? '/api'

function headers() {
  return {
    'Content-Type': 'application/json',
    'X-Rol': 'ANALISTA',
    'X-Identificacion': getAnalystSession()?.nationalId ?? '',
  }
}

async function readResponse(response) {
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(body?.detail ?? 'No se pudo completar la operación. Intenta de nuevo.')
  }
  return body
}

const samplePreview = {
  applicationId: 9001,
  customerNationalId: '1710034065',
  customerName: 'María Fernanda Andrade Lema',
  approvedAmount: 10000.0,
  effectiveAnnualRate: 0.155,
  termMonths: 2,
  amortizationMethod: 'FRENCH',
  monthlyInsurance: 6.0,
  solcaContribution: 50.0,
  netAmount: 9950.0,
  disbursementDate: '2026-10-31',
  accounts: [
    { accountNumber: '2200000001', accountType: 'AHORROS' },
    { accountNumber: '3100000001', accountType: 'CORRIENTE' },
  ],
}

export async function findPendingDisbursements(page) {
  if (USE_SAMPLE_DATA) {
    return {
      content: [
        {
          applicationId: samplePreview.applicationId,
          customerId: 1,
          customerNationalId: samplePreview.customerNationalId,
          customerName: samplePreview.customerName,
          approvedAmount: samplePreview.approvedAmount,
          effectiveAnnualRate: samplePreview.effectiveAnnualRate,
          termMonths: samplePreview.termMonths,
          amortizationMethod: samplePreview.amortizationMethod,
          resolutionDate: '2026-10-30',
        },
      ],
      page: { size: 10, number: 0, totalElements: 1, totalPages: 1 },
    }
  }
  const response = await fetch(`${API}/pending-disbursements?page=${page}`, { headers: headers() })
  return readResponse(response)
}

export async function getDisbursementPreview(applicationId) {
  if (USE_SAMPLE_DATA) {
    return { ...samplePreview, applicationId }
  }
  const response = await fetch(`${API}/disbursement-previews/${applicationId}`, { headers: headers() })
  return readResponse(response)
}

export async function disburse(applicationId, destinationAccount) {
  if (USE_SAMPLE_DATA) {
    return {
      applicationId,
      loanNumber: 'OP-20261031-009001',
      netAmount: samplePreview.netAmount,
      destinationAccount,
      disbursementDate: samplePreview.disbursementDate,
    }
  }
  const response = await fetch(`${API}/disbursements`, {
    method: 'POST',
    headers: headers(),
    body: JSON.stringify({ applicationId, destinationAccount }),
  })
  return readResponse(response)
}
