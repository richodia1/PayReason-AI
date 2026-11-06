import { useEffect, useState, type FormEvent } from 'react'
import { Alert, Button, Checkbox, Chip, CircularProgress, Drawer, FormControlLabel, IconButton, MenuItem, TextField } from '@mui/material'
import { Ban, BrainCircuit, Check, CreditCard, ExternalLink, FileClock, RefreshCw, ShieldCheck, X } from 'lucide-react'
import { api, ApiError, formatDate, formatMoney, type Activity, type Assessment, type Payment } from './api'

export const statusLabel = (status: string) => status.toLowerCase().replaceAll('_', ' ').replace(/^./, c => c.toUpperCase())
export function PaymentStatus({ status }: { status: string }) {
  return <Chip size="small" label={statusLabel(status)} className={`status-${status.toLowerCase()}`} />
}

export default function PaymentDetail({ payment, onClose, onChanged }: { payment: Payment | null; onClose: () => void; onChanged: () => void }) {
  const [current, setCurrent] = useState(payment)
  const [assessment, setAssessment] = useState<Assessment | null>(null)
  const [activity, setActivity] = useState<Activity[]>([])
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [decision, setDecision] = useState('HOLD')
  const [notes, setNotes] = useState('')
  const [features, setFeatures] = useState({ amount_ratio: '1', account_age_days: '1000', transactions_last_hour: '1', new_device: false, cross_border: false })
  const [version, setVersion] = useState(0)
  const [pending, setPending] = useState<string[]>([])
  useEffect(() => {
    setCurrent(payment); setError(''); setConfirmation(''); setAssessment(null); setActivity([]); setNotes(''); setDecision('HOLD'); setPending([])
  }, [payment])
  useEffect(() => {
    if (!payment) return
    const controller = new AbortController(); setLoading(true)
    Promise.all([
      api<Payment>(`/payments/${payment.id}`, { signal: controller.signal }),
      api<Activity[]>(`/payments/${payment.id}/activity`, { signal: controller.signal }),
      api<Assessment>(`/payments/${payment.id}/risk`, { signal: controller.signal }).catch(error => { if (error instanceof ApiError && error.status === 404) return null; throw error }),
      api<{ kind: string; state: string }[]>(`/payments/${payment.id}/paypal/operations`, { signal: controller.signal }),
    ]).then(([p, a, r, ops]) => { if (!controller.signal.aborted) { setCurrent(p); setActivity(a); setAssessment(r); setDecision(r?.decision || 'HOLD'); setNotes(r?.reviewNotes || ''); setPending(ops.filter(o => o.state === 'PENDING').map(o => o.kind)) } })
      .catch(error => { if (!controller.signal.aborted) setError(error.message) })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [payment, version])

  async function mutate(path: string, body?: unknown) {
    if (!current) return
    setBusy(true); setError('')
    try {
      await api(`/payments/${current.id}/${path}`, { method: 'POST', ...(body ? { headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) } : {}) })
      setVersion(v => v + 1); setConfirmation(''); onChanged()
      if (path === 'cancel') onClose()
    } catch (error) { setError(error instanceof Error ? error.message : 'The operation failed.'); setVersion(v => v + 1) }
    finally { setBusy(false) }
  }
  function score(event: FormEvent) {
    event.preventDefault()
    void mutate('risk', { ...features, amount_ratio: Number(features.amount_ratio), account_age_days: Number(features.account_age_days), transactions_last_hour: Number(features.transactions_last_hour) })
  }
  const active = !!current && ['DRAFT', 'ORDER_CREATED', 'AUTHORIZED'].includes(current.status)
  const locked = busy || loading
  return <Drawer anchor="right" open={!!payment} onClose={() => !busy && onClose()} sx={{ overflow: 'hidden' }}>
    {current && <div className="detail-panel risk-detail"><div className="detail-top"><span className="section-label">PAYMENT & RISK REVIEW</span><div className="button-row"><IconButton aria-label="Refresh details" disabled={locked} onClick={() => setVersion(v => v + 1)}><RefreshCw size={18} /></IconButton><IconButton aria-label="Close payment details" disabled={busy} onClick={onClose}><X size={20} /></IconButton></div></div>
      <h2>{current.reference}</h2><div className="detail-amount">{formatMoney(current.amount, current.currency)}</div><PaymentStatus status={current.status} />
      {error && <Alert severity="error">{error}</Alert>}{loading && <CircularProgress size={18} aria-label="Loading payment details" />}
      {!!pending.length && <Alert severity="warning">Unresolved PayPal operation: {pending.join(', ')}. Review changes are locked. Reconcile or retry the same operation.</Alert>}
      <dl className="detail-list"><dt>Payment ID</dt><dd className="mono">{current.id}</dd><dt>Customer</dt><dd>{current.customerReference || 'Not provided'}</dd><dt>Channel</dt><dd>{statusLabel(current.channel)}</dd>
        <dt>PayPal order</dt><dd>{current.paypalOrderId || 'Not submitted'}</dd><dt>Authorization</dt><dd>{current.authorizationId || 'Not authorized'}</dd><dt>Capture</dt><dd>{current.captureId || 'Not captured'}</dd></dl>
      <section className="detail-section"><h3><BrainCircuit size={18} /> Risk assessment</h3><Alert severity="warning" className="model-disclaimer">Synthetic-data research model. Not validated on real fraud. Human review required.</Alert>
        {assessment ? <><div className="risk-summary"><div><span>Model score</span><strong>{(assessment.score * 100).toFixed(1)}<small>/100</small></strong></div><Chip label={`${assessment.band} risk`} color={assessment.band === 'HIGH' ? 'error' : assessment.band === 'MEDIUM' ? 'warning' : 'success'} size="small" /></div>
          <p className="explanation-text">{assessment.evidence.explanation}</p><h4>Feature contributions <small>(log-odds)</small></h4>
          <div className="contributions">{assessment.evidence.contributions.map(c => <div className="contribution" key={c.feature}><div><span>{statusLabel(c.feature)}</span><small>Observed: {c.value}</small></div><div className="contribution-track"><span className={c.log_odds_contribution > 0 ? 'increases' : 'decreases'} style={{ width: `${Math.max(1, Math.abs(c.log_odds_contribution) / Math.max(...assessment.evidence.contributions.map(x => Math.abs(x.log_odds_contribution)), 0.01) * 100)}%` }} /></div><b>{c.log_odds_contribution > 0 ? '+' : ''}{c.log_odds_contribution.toFixed(2)}</b></div>)}</div>
          <p className="evidence-meta">{assessment.modelVersion} / {formatDate(assessment.assessedAt)} / {assessment.assessedBy}</p>
        </> : active ? <form className="risk-form" onSubmit={score}><TextField select label="Demo scenario" value={features.new_device ? 'elevated' : 'routine'} onChange={e => setFeatures(e.target.value === 'routine' ? { amount_ratio: '1', account_age_days: '1000', transactions_last_hour: '1', new_device: false, cross_border: false } : { amount_ratio: '10', account_age_days: '2', transactions_last_hour: '8', new_device: true, cross_border: true })} size="small"><MenuItem value="routine">Routine sample</MenuItem><MenuItem value="elevated">Elevated-risk sample</MenuItem></TextField>
          <TextField label="Amount / historical average" type="number" value={features.amount_ratio} onChange={e => setFeatures({ ...features, amount_ratio: e.target.value })} required slotProps={{ htmlInput: { min: 0.01, max: 30, step: 0.01 } }} size="small" />
          <div className="form-pair"><TextField label="Account age (days)" type="number" value={features.account_age_days} onChange={e => setFeatures({ ...features, account_age_days: e.target.value })} required slotProps={{ htmlInput: { min: 0, max: 3650, step: 1 } }} size="small" /><TextField label="Transactions / hour" type="number" value={features.transactions_last_hour} onChange={e => setFeatures({ ...features, transactions_last_hour: e.target.value })} required slotProps={{ htmlInput: { min: 0, max: 50, step: 1 } }} size="small" /></div>
          <div className="button-row"><FormControlLabel control={<Checkbox checked={features.new_device} onChange={e => setFeatures({ ...features, new_device: e.target.checked })} />} label="New device" /><FormControlLabel control={<Checkbox checked={features.cross_border} onChange={e => setFeatures({ ...features, cross_border: e.target.checked })} />} label="Cross-border" /></div>
          <Button variant="outlined" type="submit" disabled={locked} startIcon={<BrainCircuit size={17} />}>Assess sample</Button></form> : <p className="muted">No assessment recorded.</p>}
      </section>
      {assessment && <section className="detail-section"><h3><ShieldCheck size={18} /> Review decision</h3><PaymentStatus status={assessment.decision} />
        {active && <form className="risk-form" onSubmit={e => { e.preventDefault(); void mutate('risk/review', { decision, notes }) }}><TextField select label="Decision" size="small" value={decision} onChange={e => setDecision(e.target.value)}>{['HOLD', 'RELEASE', 'BLOCK'].map(value => <MenuItem key={value} value={value}>{statusLabel(value)}</MenuItem>)}</TextField>
          <TextField label="Review reason" value={notes} onChange={e => setNotes(e.target.value)} multiline minRows={2} required slotProps={{ htmlInput: { minLength: 10, maxLength: 2000 } }} /><Button type="submit" variant="contained" disabled={locked || !!pending.length} startIcon={<Check size={17} />}>Save review</Button></form>}
        {assessment.reviewedBy && <p className="evidence-meta">{assessment.reviewedBy} / {formatDate(assessment.reviewedAt!)}<br />{assessment.reviewNotes}</p>}
      </section>}
      <section className="detail-section"><h3><CreditCard size={18} /> PayPal Sandbox</h3><div className="payment-actions">
        {current.status === 'DRAFT' && <Button disabled={locked} variant="outlined" startIcon={<CreditCard size={17} />} onClick={() => void mutate('paypal/order')}>Create Sandbox order</Button>}
        {current.status === 'ORDER_CREATED' && <>{current.approvalUrl && <Button component="a" href={current.approvalUrl} target="_blank" rel="noopener noreferrer" variant="outlined" startIcon={<ExternalLink size={17} />}>Buyer approval</Button>}<Button disabled={locked} onClick={() => void mutate('paypal/authorize')} variant="outlined">Authorize approved order</Button></>}
        {current.paypalOrderId && <Button disabled={locked} startIcon={<RefreshCw size={17} />} onClick={() => void mutate('paypal/reconcile')}>Reconcile</Button>}
        {current.status === 'AUTHORIZED' && assessment?.decision === 'RELEASE' && <Button disabled={locked} variant="contained" onClick={() => setConfirmation('paypal/capture')}>Capture payment</Button>}
        {current.status === 'AUTHORIZED' && assessment?.decision === 'BLOCK' && <Button disabled={locked} color="error" variant="outlined" onClick={() => setConfirmation('paypal/void')}>Void authorization</Button>}
        {current.status === 'DRAFT' && <Button disabled={locked || !!pending.length} color="error" startIcon={<Ban size={17} />} onClick={() => setConfirmation('cancel')}>Cancel draft</Button>}
      </div>{confirmation && <div className="confirmation"><p>{confirmation === 'cancel' ? 'Cancel this local payment draft?' : confirmation === 'paypal/capture' ? 'Capture the authorized Sandbox funds?' : 'Void the Sandbox authorization?'}</p><div className="button-row"><Button disabled={locked} onClick={() => setConfirmation('')}>Keep unchanged</Button><Button disabled={locked} color={confirmation === 'paypal/capture' ? 'primary' : 'error'} variant="contained" onClick={() => void mutate(confirmation)}>{confirmation === 'cancel' ? 'Confirm cancellation' : 'Confirm'}</Button></div></div>}</section>
      <section className="detail-section"><h3><FileClock size={18} /> Activity</h3><ol className="activity-list">{activity.map(item => <li key={item.id}><span className="activity-icon"><FileClock size={16} /></span><div><strong>{statusLabel(item.action)}</strong><p>{item.actor} / {formatDate(item.occurredAt)}</p>{item.notes && <p>{item.notes}</p>}</div></li>)}</ol></section>
    </div>}
  </Drawer>
}
