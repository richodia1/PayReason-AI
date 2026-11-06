export type Payment = {
  id: string; reference: string; customerReference: string | null
  amount: string; currency: string; channel: 'WEB' | 'MOBILE'
  status: 'DRAFT' | 'CANCELLED' | 'ORDER_CREATED' | 'AUTHORIZED' | 'CAPTURE_PENDING' | 'CAPTURED' | 'VOIDED' | 'DECLINED'
  createdAt: string; updatedAt: string
  paypalOrderId?: string | null; authorizationId?: string | null; captureId?: string | null; approvalUrl?: string | null
}
export type PaymentPage = { items: Payment[]; total: number; page: number; size: number }
export type Summary = { total: number; drafts: number; cancelled: number }
export type Activity = { id: string; action: string; actor: string; occurredAt: string; notes?: string }
export type Assessment = {
  paymentId: string; score: number; band: 'LOW' | 'MEDIUM' | 'HIGH'; modelVersion: string; source: string
  assessedAt: string; assessedBy: string; decision: 'HOLD' | 'RELEASE' | 'BLOCK'
  reviewNotes: string | null; reviewedBy: string | null; reviewedAt: string | null
  evidence: { explanation: string; limitation: string; base_log_odds: number; contributions: { feature: string; value: number; log_odds_contribution: number; direction: string }[] }
}
export type CaseRow = { payment: Payment; assessment: Assessment }
export type CasePage = { items: CaseRow[]; total: number; page: number; size: number }
export type Integration = { id: string; name: string; status: string; detail: string; credentialsPresent?: boolean }

export class ApiError extends Error {
  constructor(message: string, public status: number) { super(message) }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.method && !['GET', 'HEAD'].includes(init.method.toUpperCase())) {
    const csrf = await api<{ token: string; headerName: string }>('/auth/csrf')
    headers.set(csrf.headerName, csrf.token)
  }
  const response = await fetch(`/api${path}`, { ...init, headers, credentials: 'same-origin' })
  if (!response.ok) {
    const text = await response.text()
    let message = response.status === 401 ? 'Please sign in again.' : 'The request could not be completed.'
    try {
      const body = JSON.parse(text)
      if (body.detail) message = body.detail
    } catch { /* A proxy error may not return JSON. */ }
    throw new ApiError(message, response.status)
  }
  return response.status === 204 ? undefined as T : await response.json() as T
}

export function formatMoney(amount: string, currency: string) {
  return new Intl.NumberFormat('en-GB', { style: 'currency', currency }).format(Number(amount))
}
export function formatDate(value: string) {
  return new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
