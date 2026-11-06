import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { NewPaymentDialog } from './App'

beforeEach(() => vi.restoreAllMocks())

describe('Payment draft form', () => {
  it('closes after a successful save and sends an idempotency key', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(new Response(JSON.stringify({ token: 'csrf', headerName: 'X-CSRF-TOKEN' })))
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: 'created' })))
    const onClose = vi.fn(); const onSaved = vi.fn(); const user = userEvent.setup()
    render(<NewPaymentDialog open onClose={onClose} onSaved={onSaved} />)
    await user.type(screen.getByLabelText(/Order reference/), 'ORDER-101')
    await user.type(screen.getByLabelText(/Amount/), '12.34')
    await user.click(screen.getByRole('button', { name: 'Save draft' }))
    await waitFor(() => expect(onClose).toHaveBeenCalledOnce())
    expect(onSaved).toHaveBeenCalledOnce()
    const options = fetchMock.mock.calls[1][1]!
    expect((options.headers as Headers).get('Idempotency-Key')).toBeTruthy()
    expect(JSON.parse(String(options.body)).amount).toBe('12.34')
  })

  it('keeps the form and entered values after a server error', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(new Response(JSON.stringify({ token: 'csrf', headerName: 'X-CSRF-TOKEN' })))
      .mockResolvedValueOnce(new Response(JSON.stringify({ detail: 'Database unavailable' }), { status: 503 }))
    const onClose = vi.fn(); const user = userEvent.setup()
    render(<NewPaymentDialog open onClose={onClose} onSaved={vi.fn()} />)
    await user.type(screen.getByLabelText(/Order reference/), 'RETRY-101')
    await user.type(screen.getByLabelText(/Amount/), '10.00')
    await user.click(screen.getByRole('button', { name: 'Save draft' }))
    expect(await screen.findByText('Database unavailable')).toBeInTheDocument()
    expect(screen.getByLabelText(/Order reference/)).toHaveValue('RETRY-101')
    expect(onClose).not.toHaveBeenCalled()
  })
})
