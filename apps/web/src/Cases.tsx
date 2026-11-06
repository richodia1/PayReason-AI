import { useEffect, useState, type FormEvent } from 'react'
import { Alert, Button, Chip, CircularProgress, IconButton, MenuItem, TextField } from '@mui/material'
import { ArrowUpRight, ChevronLeft, ChevronRight, Search } from 'lucide-react'
import { api, formatDate, formatMoney, type CasePage, type CaseRow, type Payment } from './api'
import { PaymentStatus } from './PaymentDetail'

export function Cases({ onOpen, version }: { onOpen: (payment: Payment) => void; version: number }) {
  const [data, setData] = useState<CasePage | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [channel, setChannel] = useState('')
  const [decision, setDecision] = useState('')
  const [year, setYear] = useState('')
  const [page, setPage] = useState(0)
  useEffect(() => {
    const controller = new AbortController(); setLoading(true); setError('')
    const params = new URLSearchParams({ page: String(page), size: '20' })
    if (channel) params.set('channel', channel)
    if (decision) params.set('decision', decision)
    if (year) params.set('year', year)
    api<CasePage>(`/cases?${params}`, { signal: controller.signal }).then(setData)
      .catch(error => { if (!controller.signal.aborted) setError(error.message) })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [channel, decision, year, page, version])
  const countLabel = `${data?.total ?? 0} ${data?.total === 1 ? 'case' : 'cases'}`
  return <section><div className="case-filters"><TextField select label="Channel" size="small" value={channel} onChange={e => { setChannel(e.target.value); setPage(0) }}><MenuItem value="">All channels</MenuItem><MenuItem value="WEB">Web</MenuItem><MenuItem value="MOBILE">Mobile</MenuItem></TextField>
    <TextField select label="Decision" size="small" value={decision} onChange={e => { setDecision(e.target.value); setPage(0) }}><MenuItem value="">All decisions</MenuItem>{['HOLD', 'RELEASE', 'BLOCK'].map(d => <MenuItem value={d} key={d}>{d}</MenuItem>)}</TextField>
    <TextField select label="Assessment year (UTC)" size="small" value={year} onChange={e => { setYear(e.target.value); setPage(0) }}><MenuItem value="">All years</MenuItem>{Array.from({ length: 10 }, (_, i) => new Date().getUTCFullYear() - i).map(y => <MenuItem key={y} value={String(y)}>{y}</MenuItem>)}</TextField></div>
    {error && <Alert severity="error">{error}</Alert>}
    <div className="register"><div className="register-toolbar"><strong>Assessed payments</strong><span>{countLabel}</span></div><div className="table-scroll"><table><thead><tr><th>Reference</th><th>Channel</th><th>Amount</th><th>Model score</th><th>Review decision</th><th>Assessed</th><th>Evidence</th></tr></thead><tbody>{data?.items.map(({ payment, assessment }) => <tr key={payment.id}><td><button className="reference-button" onClick={() => onOpen(payment)}>{payment.reference}</button></td><td>{payment.channel}</td><td>{formatMoney(payment.amount, payment.currency)}</td><td><Chip size="small" label={`${assessment.band} / ${(assessment.score * 100).toFixed(1)}`} color={assessment.band === 'HIGH' ? 'error' : assessment.band === 'MEDIUM' ? 'warning' : 'success'} /></td><td><PaymentStatus status={assessment.decision} /></td><td>{formatDate(assessment.assessedAt)}</td><td><IconButton aria-label={`Review ${payment.reference}`} onClick={() => onOpen(payment)}><ArrowUpRight size={18} /></IconButton></td></tr>)}</tbody></table></div>
      {loading && <div className="loading-line"><CircularProgress size={18} />Loading cases</div>}{!loading && !data?.items.length && <div className="empty-state"><h2>No matching cases</h2></div>}
      <div className="table-footer"><span>{countLabel}</span><div className="button-row"><IconButton aria-label="Previous case page" disabled={loading || page === 0} onClick={() => setPage(page - 1)}><ChevronLeft size={18} /></IconButton><span>Page {page + 1}</span><IconButton aria-label="Next case page" disabled={loading || !data || (page + 1) * 20 >= data.total} onClick={() => setPage(page + 1)}><ChevronRight size={18} /></IconButton></div></div></div>
  </section>
}

type Answer = { answer: string; count: number; evidence: CaseRow[]; method: string; filters: Record<string, string>; asOf: string }
export function Insights({ onOpen }: { onOpen: (payment: Payment) => void }) {
  const [question, setQuestion] = useState('What is the latest case?')
  const [answer, setAnswer] = useState<Answer | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function ask(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError(''); setAnswer(null)
    try { setAnswer(await api<Answer>('/insights/questions', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ question }) })) }
    catch (error) { setError(error instanceof Error ? error.message : 'Unable to retrieve evidence.') }
    finally { setBusy(false) }
  }
  return <section className="insights"><form onSubmit={ask}><TextField label="Case question" value={question} onChange={e => setQuestion(e.target.value)} fullWidth required slotProps={{ htmlInput: { maxLength: 300 } }} /><Button type="submit" variant="contained" disabled={busy} startIcon={busy ? <CircularProgress size={16} /> : <Search size={18} />}>Query records</Button></form>
    <div className="question-options"><TextField select label="Saved query" size="small" value={['What is the latest case?', 'How many cases?', 'How many blocked cases from mobile this year?', 'How many cases from web this year?'].includes(question) ? question : ''} onChange={e => setQuestion(e.target.value)} fullWidth><MenuItem value="" disabled>Custom question</MenuItem>{['What is the latest case?', 'How many cases?', 'How many blocked cases from mobile this year?', 'How many cases from web this year?'].map(q => <MenuItem key={q} value={q}>{q}</MenuItem>)}</TextField></div>
    {error && <Alert severity="error">{error}</Alert>}{answer && <div className="insight-result"><span className="section-label">DATABASE EVIDENCE / {formatDate(answer.asOf)}</span><h2>{answer.answer}</h2><div className="button-row">{Object.entries(answer.filters).map(([key, value]) => <Chip size="small" variant="outlined" label={`${key}: ${value}`} key={key} />)}</div><ol className="evidence-list">{answer.evidence.map(({ payment, assessment }) => <li key={payment.id}><button className="reference-button" onClick={() => onOpen(payment)}>{payment.reference} <ArrowUpRight size={15} /></button><span>{payment.channel} / {assessment.decision} / {formatDate(assessment.assessedAt)}</span><code>{payment.id}</code></li>)}</ol><p className="muted">Evidence shows up to five matching records. Counts include all matches. These are assessed cases, not confirmed fraud.</p></div>}
  </section>
}
