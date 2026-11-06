import { useCallback, useEffect, useState, type FormEvent } from 'react'
import {
  Alert, Button, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  Drawer, IconButton, MenuItem, Snackbar, TextField, Tooltip,
} from '@mui/material'
import {
  ArrowDownLeft, ArrowUpRight, Ban, ChevronLeft, ChevronRight, CircleHelp,
  CreditCard, FileClock, LogOut, Menu, Plus, RefreshCw, Settings2,
  ShieldCheck, WalletCards, X, FolderSearch, MessageSquare,
} from 'lucide-react'
import PaymentDetail, { PaymentStatus as Status } from './PaymentDetail'
import { Cases, Insights } from './Cases'
import { api, ApiError, formatDate, formatMoney, type Activity, type Integration, type Payment, type PaymentPage, type Summary } from './api'

function Brand() {
  return <div className="brand"><span className="brand-icon"><ShieldCheck size={23} /></span><span>PayReason <b>AI</b></span></div>
}

function SignIn({ onSuccess }: { onSuccess: () => void }) {
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError('')
    const data = new FormData(event.currentTarget)
    try {
      await api('/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ username: String(data.get('username')), password: String(data.get('password')) }) })
      onSuccess()
    } catch (error) {
      setError(error instanceof ApiError && error.status === 401 ? 'The username or password is incorrect.' : 'Unable to sign in. Check the API connection.')
    } finally { setBusy(false) }
  }
  return <div className="login-page"><div className="login-top"><Brand /><Chip label="Sandbox workspace" size="small" variant="outlined" /></div>
    <main className="login-form"><span className="section-label">MERCHANT WORKSPACE</span><h1>Welcome to PayReason.</h1><p className="muted">Sign in to your workspace.</p>
      <form onSubmit={submit}>{error && <Alert severity="error">{error}</Alert>}
        <TextField label="Username" name="username" autoComplete="username" required autoFocus />
        <TextField label="Password" name="password" type="password" autoComplete="current-password" required />
        <Button type="submit" variant="contained" disabled={busy} endIcon={busy ? <CircularProgress size={16} /> : <ArrowUpRight size={18} />}>Sign in</Button>
      </form><div className="login-footer"><ShieldCheck size={16} /> PayReason AI / Sandbox</div>
    </main></div>
}

export function NewPaymentDialog({ open, onClose, onSaved }: { open: boolean; onClose: () => void; onSaved: () => void }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [requestKey, setRequestKey] = useState(() => crypto.randomUUID())
  useEffect(() => { if (open) { setError(''); setRequestKey(crypto.randomUUID()) } }, [open])
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError('')
    const values = new FormData(event.currentTarget)
    try {
      await api<Payment>('/payments', { method: 'POST', headers: { 'Content-Type': 'application/json', 'Idempotency-Key': requestKey },
        body: JSON.stringify(Object.fromEntries(values)) })
      onSaved(); onClose()
    } catch (error) { setError(error instanceof Error ? error.message : 'Unable to save draft.') }
    finally { setBusy(false) }
  }
  return <Dialog open={open} onClose={() => !busy && onClose()} fullWidth maxWidth="sm" aria-labelledby="new-payment-title">
    <form onSubmit={submit} onChange={() => setRequestKey(crypto.randomUUID())}>
      <DialogTitle id="new-payment-title">New payment draft</DialogTitle>
      <DialogContent><div className="form-fields">
        {error && <Alert severity="error">{error}</Alert>}
        <TextField label="Order reference" name="reference" required slotProps={{ htmlInput: { maxLength: 80 } }} autoFocus />
        <div className="form-pair"><TextField label="Amount" name="amount" type="number" required slotProps={{ htmlInput: { min: '0.01', max: '999999999999.99', step: '0.01' } }} />
          <TextField select label="Currency" name="currency" defaultValue="GBP">{['GBP', 'USD', 'EUR'].map(currency => <MenuItem key={currency} value={currency}>{currency}</MenuItem>)}</TextField></div>
        <TextField label="Customer reference" name="customerReference" slotProps={{ htmlInput: { maxLength: 80 } }} />
        <TextField select label="Channel" name="channel" defaultValue="WEB"><MenuItem value="WEB">Web</MenuItem><MenuItem value="MOBILE">Mobile</MenuItem></TextField>
      </div></DialogContent>
      <DialogActions><Button onClick={onClose} disabled={busy}>Cancel</Button><Button variant="contained" type="submit" disabled={busy} startIcon={busy ? <CircularProgress size={16} /> : <Plus size={17} />}>Save draft</Button></DialogActions>
    </form></Dialog>
}

function Integrations({ items }: { items: Integration[] }) {
  return <section className="integrations-list">{items.map(item => <article className="integration-row" key={item.id}><div className={`integration-icon integration-${item.id}`}>{item.id === 'paypal' ? <CreditCard size={24} /> : item.id === 'ml' ? <ShieldCheck size={24} /> : <CircleHelp size={24} />}</div>
    <div className="integration-copy"><h2>{item.name}</h2><p>{item.detail}</p>{item.credentialsPresent !== undefined && <small>Credentials {item.credentialsPresent ? 'present' : 'not configured'}</small>}</div>
    <Chip label={item.status === 'NOT_CONNECTED' ? 'Not connected' : item.status === 'AVAILABLE' ? 'Available' : item.status === 'CONFIGURED' ? 'Configured' : 'Unavailable'} size="small" variant="outlined" />
  </article>)}</section>
}

function Workspace({ username, onSignOut }: { username: string; onSignOut: () => void }) {
  const [view, setView] = useState<'payments' | 'integrations' | 'cases' | 'insights'>('payments')
  const [mobileNav, setMobileNav] = useState(false)
  const [filter, setFilter] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState<PaymentPage | null>(null)
  const [summary, setSummary] = useState<Summary | null>(null)
  const [integrations, setIntegrations] = useState<Integration[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [version, setVersion] = useState(0)
  const [create, setCreate] = useState(false)
  const [detail, setDetail] = useState<Payment | null>(null)
  const [notice, setNotice] = useState('')
  const refresh = useCallback(() => setVersion(value => value + 1), [])
  useEffect(() => {
    const controller = new AbortController()
    setLoading(true); setError('')
    Promise.all([
      api<PaymentPage>(`/payments?page=${page}&size=20${filter ? `&status=${filter}` : ''}`, { signal: controller.signal }),
      api<Summary>('/payments/summary', { signal: controller.signal }),
      api<{ integrations: Integration[] }>('/integrations', { signal: controller.signal }),
    ]).then(([payments, summary, integrations]) => { setData(payments); setSummary(summary); setIntegrations(integrations.integrations) })
      .catch(error => { if (!controller.signal.aborted) { setError(error.message); if (error instanceof ApiError && error.status === 401) onSignOut() } })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [page, filter, version, onSignOut])
  async function logout() {
    try { await api('/auth/logout', { method: 'POST' }); onSignOut() }
    catch { setError('Unable to sign out. Please retry.') }
  }
  const navigation = <><Brand /><div className="workspace-label"><span className="workspace-avatar">P</span><div><strong>Merchant workspace</strong><small>Sandbox environment</small></div></div>
    <div className="nav-section-label">WORKSPACE</div><nav>{([
      ['payments', WalletCards, 'Payments'], ['cases', FolderSearch, 'Cases'], ['insights', MessageSquare, 'Case insights'], ['integrations', Settings2, 'Integrations'],
    ] as const).map(([key, Icon, label]) => <button key={key} className={`nav-item ${view === key ? 'active' : ''}`} onClick={() => { setView(key); setMobileNav(false) }} aria-current={view === key ? 'page' : undefined}><Icon size={18} />{label}</button>)}</nav>
    <div className="sidebar-bottom"><div className="environment"><span />Sandbox</div><p>PayReason AI <span>v0.1</span></p></div></>

  return <div className="app-shell"><aside className="sidebar">{navigation}</aside><Drawer open={mobileNav} onClose={() => setMobileNav(false)} sx={{ overflow: 'hidden' }}><aside className="mobile-sidebar">{navigation}</aside></Drawer>
    <div className="main-column"><header className="topbar"><div className="topbar-left"><IconButton className="mobile-menu" aria-label="Open navigation" onClick={() => setMobileNav(true)}><Menu size={21} /></IconButton><span className="breadcrumb">Workspace <ChevronRight size={13} /><strong>{{ payments: 'Payments', integrations: 'Integrations', cases: 'Cases', insights: 'Case insights' }[view]}</strong></span></div>
      <div className="topbar-right"><Chip label="Sandbox" size="small" className="sandbox-chip" /><span className="user-name">{username}</span><Tooltip title="Sign out"><IconButton aria-label="Sign out" onClick={logout}><LogOut size={18} /></IconButton></Tooltip></div></header>
      <main className="workspace-main"><div className="page-heading"><div><span className="section-label">{view === 'payments' ? 'PAYMENT OPERATIONS' : 'WORKSPACE SETTINGS'}</span><h1>{{ payments: 'Payment register', integrations: 'Integrations', cases: 'Case queue', insights: 'Case insights' }[view]}</h1></div><div className="button-row"><Tooltip title="Refresh"><IconButton aria-label="Refresh payments" onClick={refresh} disabled={loading}><RefreshCw size={19} /></IconButton></Tooltip>{view === 'payments' && <Button variant="contained" startIcon={<Plus size={18} />} onClick={() => setCreate(true)}>New draft</Button>}</div></div>
        {error && <Alert severity="error" action={<Button color="inherit" onClick={refresh}>Retry</Button>}>{error}</Alert>}
        {view === 'payments' ? <><section className="metrics" aria-label="Payment summary"><div><span><WalletCards size={17} />Total records</span><strong>{summary?.total ?? '-'}</strong><small>All payment drafts</small></div><div><span><FileClock size={17} />Open drafts</span><strong>{summary?.drafts ?? '-'}</strong><small>Not submitted to PayPal</small></div><div><span><Ban size={17} />Cancelled</span><strong>{summary?.cancelled ?? '-'}</strong><small>Closed local drafts</small></div></section>
          <div className="register"><div className="register-toolbar"><div role="tablist" aria-label="Payment status" className="filter-tabs">{[['', 'All payments'], ['DRAFT', 'Drafts'], ['AUTHORIZED', 'Authorized'], ['CAPTURED', 'Captured'], ['CANCELLED', 'Cancelled']].map(([key, label]) => <button key={key} role="tab" aria-selected={filter === key} className={filter === key ? 'selected' : ''} onClick={() => { setFilter(key); setPage(0) }}>{label}</button>)}</div><span className="record-count">{data?.total ?? 0} records</span></div>
          <div className="table-scroll"><table><thead><tr><th>Reference</th><th>Customer</th><th>Channel</th><th>Created</th><th className="amount-cell">Amount</th><th>Status</th><th><span className="sr-only">Details</span></th></tr></thead><tbody>
            {data?.items.map(payment => <tr key={payment.id}><td><button className="reference-button" onClick={() => setDetail(payment)}>{payment.reference}</button><small className="payment-id">{payment.id.slice(0, 8)}</small></td><td>{payment.customerReference || <span className="muted">Not provided</span>}</td><td>{payment.channel === 'WEB' ? 'Web' : 'Mobile'}</td><td className="date-cell">{formatDate(payment.createdAt)}</td><td className="amount-cell">{formatMoney(payment.amount, payment.currency)}<small>{payment.currency}</small></td><td><Status status={payment.status} /></td><td><Tooltip title="View payment"><IconButton aria-label={`View ${payment.reference}`} size="small" onClick={() => setDetail(payment)}><ArrowUpRight size={17} /></IconButton></Tooltip></td></tr>)}
          </tbody></table></div>
          {loading && <div className="loading-line" role="status"><CircularProgress size={17} /> Loading payments</div>}
          {!loading && !error && data?.items.length === 0 && <div className="empty-state"><span className="empty-icon"><ArrowDownLeft size={28} /></span><h2>{filter ? 'No matching payments' : 'No payments yet'}</h2><p>{filter ? 'No records with this status.' : 'Your payment register is empty.'}</p>{!filter && <Button variant="outlined" startIcon={<Plus size={17} />} onClick={() => setCreate(true)}>Create a draft</Button>}</div>}
          <div className="table-footer"><span>{data?.total ? `${page * 20 + 1}-${Math.min((page + 1) * 20, data.total)} of ${data.total}` : '0 payments'}</span><div className="button-row"><IconButton aria-label="Previous page" disabled={loading || page === 0} onClick={() => setPage(page - 1)}><ChevronLeft size={19} /></IconButton><span>Page {page + 1}</span><IconButton aria-label="Next page" disabled={loading || !data || (page + 1) * 20 >= data.total} onClick={() => setPage(page + 1)}><ChevronRight size={19} /></IconButton></div></div></div>
        </> : view === 'cases' ? <Cases onOpen={setDetail} version={version} /> : view === 'insights' ? <Insights onOpen={setDetail} /> : <Integrations items={integrations} />}
        <footer className="workspace-footer"><span>PayReason AI</span><span><ShieldCheck size={14} /> Sandbox workspace</span></footer>
      </main></div>
    <NewPaymentDialog open={create} onClose={() => setCreate(false)} onSaved={() => { refresh(); setNotice('Payment draft saved.') }} />
    <PaymentDetail payment={detail} onClose={() => setDetail(null)} onChanged={() => { refresh(); setNotice('Payment updated.') }} />
    <Snackbar open={!!notice} autoHideDuration={4500} onClose={() => setNotice('')}><Alert severity="success" onClose={() => setNotice('')}>{notice}</Alert></Snackbar>
  </div>
}

export default function App() {
  const [user, setUser] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const signedOut = useCallback(() => setUser(null), [])
  const session = useCallback(() => {
    api<{ username: string }>('/auth/me').then(result => setUser(result.username)).catch(() => setUser(null)).finally(() => setLoading(false))
  }, [])
  useEffect(session, [session])
  if (loading) return <div className="app-loading"><CircularProgress size={28} aria-label="Loading workspace" /></div>
  return user ? <Workspace username={user} onSignOut={signedOut} /> : <SignIn onSuccess={session} />
}
