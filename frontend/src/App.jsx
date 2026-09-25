import { useEffect, useState } from 'react'
import './App.css'

const API_BASE = '/api'

async function requestJson(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: options.body ? { 'Content-Type': 'application/json' } : undefined,
    ...options,
  })

  if (!response.ok) {
    let message = 'Request failed'

    try {
      const body = await response.json()
      message = body.message || message
    } catch {
      message = `${response.status} ${response.statusText}`
    }

    throw new Error(message)
  }

  return response.json()
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(Number(value || 0))
}

function formatDate(value) {
  if (!value) return '—'
  return new Date(value).toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function App() {
  const [beneficiaries, setBeneficiaries] = useState([])
  const [loans, setLoans] = useState([])
  const [portfolio, setPortfolio] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [beneficiaryForm, setBeneficiaryForm] = useState({ name: '', email: '', phone: '' })
  const [loanForm, setLoanForm] = useState({ beneficiaryId: '', amount: '', termMonths: '', purpose: '', annualInterestRate: '' })
  const [creditForm, setCreditForm] = useState({ beneficiaryId: '', monthlyIncome: '', monthlyDebt: '', onTimePayments: '', missedPayments: '' })
  const [creditResult, setCreditResult] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const refreshPortfolio = async () => {
    setPortfolio(await requestJson('/reports/portfolio'))
  }

  useEffect(() => {
    const loadDashboard = async () => {
      try {
        setLoading(true)
        setError('')

        const [beneficiaryData, loanData, portfolioData] = await Promise.all([
          requestJson('/beneficiaries'),
          requestJson('/loans?size=10'),
          requestJson('/reports/portfolio'),
        ])

        setBeneficiaries(beneficiaryData)
        setLoans(loanData)
        setPortfolio(portfolioData)
      } catch (loadError) {
        setError(loadError.message)
      } finally {
        setLoading(false)
      }
    }

    loadDashboard()
  }, [])

  const createBeneficiary = async (event) => {
    event.preventDefault()
    try {
      setSubmitting(true)
      setError('')
      const created = await requestJson('/beneficiaries', {
        method: 'POST',
        body: JSON.stringify(beneficiaryForm),
      })
      setBeneficiaries((current) => [created, ...current])
      setLoanForm((current) => ({ ...current, beneficiaryId: created.id }))
      setBeneficiaryForm({ name: '', email: '', phone: '' })
      setNotice('Beneficiary created and ready for a loan application.')
    } catch (submitError) {
      setError(submitError.message)
    } finally {
      setSubmitting(false)
    }
  }

  const createLoan = async (event) => {
    event.preventDefault()
    try {
      setSubmitting(true)
      setError('')
      const created = await requestJson('/loans', {
        method: 'POST',
        body: JSON.stringify({
          beneficiaryId: loanForm.beneficiaryId,
          amount: Number(loanForm.amount),
          termMonths: Number(loanForm.termMonths),
          purpose: loanForm.purpose,
          annualInterestRate: loanForm.annualInterestRate === '' ? 0 : Number(loanForm.annualInterestRate),
        }),
      })
      setLoans((current) => [created, ...current].slice(0, 10))
      setLoanForm({ beneficiaryId: '', amount: '', termMonths: '', purpose: '', annualInterestRate: '' })
      await refreshPortfolio()
      setNotice('Loan application submitted for review.')
    } catch (submitError) {
      setError(submitError.message)
    } finally {
      setSubmitting(false)
    }
  }

  const reviewLoan = async (loan, status) => {
    try {
      setError('')
      const updated = await requestJson(`/loans/${loan.id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status, reviewNotes: `${status === 'APPROVED' ? 'Approved' : 'Rejected'} from dashboard` }),
      })
      setLoans((current) => current.map((currentLoan) => currentLoan.id === updated.id ? updated : currentLoan))
      await refreshPortfolio()
      setNotice(`Loan marked ${status.toLowerCase()}.`)
    } catch (reviewError) {
      setError(reviewError.message)
    }
  }

  const calculateCreditScore = async (event) => {
    event.preventDefault()
    try {
      setSubmitting(true)
      setError('')
      const result = await requestJson(`/beneficiaries/${creditForm.beneficiaryId}/credit-score`, {
        method: 'POST',
        body: JSON.stringify({
          monthlyIncome: Number(creditForm.monthlyIncome),
          monthlyDebt: Number(creditForm.monthlyDebt),
          onTimePayments: Number(creditForm.onTimePayments),
          missedPayments: Number(creditForm.missedPayments),
        }),
      })
      setCreditResult(result)
      setNotice('Credit score calculated successfully.')
    } catch (scoreError) {
      setError(scoreError.message)
    } finally {
      setSubmitting(false)
    }
  }

  const summaryCards = portfolio
    ? [
        { label: 'Total applications', value: portfolio.totalApplications },
        { label: 'Approved', value: portfolio.approvedApplications },
        { label: 'Pending', value: portfolio.pendingApplications },
        { label: 'Outstanding', value: formatCurrency(portfolio.outstandingBalance) },
      ]
    : []

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Loan operations</p>
          <h1>Loan Management Dashboard</h1>
        </div>
      </header>

      {error ? <div className="error-banner">{error}</div> : null}
      {notice ? <div className="notice-banner">{notice}</div> : null}

      {loading ? (
        <div className="loading-panel">Loading dashboard data…</div>
      ) : (
        <>
          <section className="summary-grid">
            {summaryCards.map((card) => (
              <article key={card.label} className="summary-card">
                <span>{card.label}</span>
                <strong>{card.value}</strong>
              </article>
            ))}
          </section>

          <section className="panel-grid">
            <article className="panel">
              <div className="panel-header">
                <h2>Recent loan applications</h2>
              </div>

              {loans.length === 0 ? (
                <p className="empty-state">No loans found.</p>
              ) : (
                <div className="table-wrapper">
                  <table>
                    <thead>
                      <tr>
                        <th>Purpose</th>
                        <th>Amount</th>
                        <th>Status</th>
                        <th>Created</th>
                        <th>Review</th>
                      </tr>
                    </thead>
                    <tbody>
                      {loans.map((loan) => (
                        <tr key={loan.id}>
                          <td>{loan.purpose}</td>
                          <td>{formatCurrency(loan.amount)}</td>
                          <td>
                            <span className={`status-badge ${loan.status.toLowerCase()}`}>
                              {loan.status}
                            </span>
                          </td>
                          <td>{formatDate(loan.createdAt)}</td>
                          <td>
                            {loan.status === 'PENDING' ? (
                              <div className="row-actions">
                                <button type="button" className="approve-button" onClick={() => reviewLoan(loan, 'APPROVED')}>Approve</button>
                                <button type="button" className="reject-button" onClick={() => reviewLoan(loan, 'REJECTED')}>Reject</button>
                              </div>
                            ) : '—'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </article>

            <article className="panel">
              <div className="panel-header">
                <h2>Beneficiaries</h2>
              </div>

              {beneficiaries.length === 0 ? (
                <p className="empty-state">No beneficiaries found.</p>
              ) : (
                <ul className="beneficiary-list">
                  {beneficiaries.map((beneficiary) => (
                    <li key={beneficiary.id} className="beneficiary-item">
                      <div>
                        <strong>{beneficiary.name}</strong>
                        <span>{beneficiary.email}</span>
                      </div>
                      <small>{beneficiary.phone}</small>
                    </li>
                  ))}
                </ul>
              )}
            </article>
          </section>

          <section className="form-grid">
            <form className="panel form-panel" onSubmit={createBeneficiary}>
              <div className="panel-header">
                <p className="eyebrow">New record</p>
                <h2>Add beneficiary</h2>
              </div>
              <label>
                Full name
                <input required value={beneficiaryForm.name} onChange={(event) => setBeneficiaryForm({ ...beneficiaryForm, name: event.target.value })} />
              </label>
              <label>
                Email
                <input required type="email" value={beneficiaryForm.email} onChange={(event) => setBeneficiaryForm({ ...beneficiaryForm, email: event.target.value })} />
              </label>
              <label>
                Phone
                <input required value={beneficiaryForm.phone} onChange={(event) => setBeneficiaryForm({ ...beneficiaryForm, phone: event.target.value })} />
              </label>
              <button className="primary-button" disabled={submitting} type="submit">Create beneficiary</button>
            </form>

            <form className="panel form-panel" onSubmit={createLoan}>
              <div className="panel-header">
                <p className="eyebrow">New application</p>
                <h2>Submit a loan</h2>
              </div>
              <label>
                Beneficiary
                <select required value={loanForm.beneficiaryId} onChange={(event) => setLoanForm({ ...loanForm, beneficiaryId: event.target.value })}>
                  <option value="">Select beneficiary</option>
                  {beneficiaries.map((beneficiary) => <option key={beneficiary.id} value={beneficiary.id}>{beneficiary.name}</option>)}
                </select>
              </label>
              <div className="field-row">
                <label>
                  Amount
                  <input required min="1" type="number" value={loanForm.amount} onChange={(event) => setLoanForm({ ...loanForm, amount: event.target.value })} />
                </label>
                <label>
                  Term (months)
                  <input required min="1" type="number" value={loanForm.termMonths} onChange={(event) => setLoanForm({ ...loanForm, termMonths: event.target.value })} />
                </label>
              </div>
              <div className="field-row">
                <label>
                  Purpose
                  <input required value={loanForm.purpose} onChange={(event) => setLoanForm({ ...loanForm, purpose: event.target.value })} />
                </label>
                <label>
                  Interest %
                  <input min="0" step="0.01" type="number" value={loanForm.annualInterestRate} onChange={(event) => setLoanForm({ ...loanForm, annualInterestRate: event.target.value })} />
                </label>
              </div>
              <button className="primary-button" disabled={submitting || beneficiaries.length === 0} type="submit">Submit application</button>
            </form>
          </section>

          <section className="credit-section">
            <form className="panel form-panel" onSubmit={calculateCreditScore}>
              <div className="panel-header">
                <p className="eyebrow">Risk review</p>
                <h2>Calculate credit score</h2>
              </div>
              <label>
                Beneficiary
                <select required value={creditForm.beneficiaryId} onChange={(event) => setCreditForm({ ...creditForm, beneficiaryId: event.target.value })}>
                  <option value="">Select beneficiary</option>
                  {beneficiaries.map((beneficiary) => <option key={beneficiary.id} value={beneficiary.id}>{beneficiary.name}</option>)}
                </select>
              </label>
              <div className="field-row">
                <label>
                  Monthly income
                  <input required min="1" type="number" value={creditForm.monthlyIncome} onChange={(event) => setCreditForm({ ...creditForm, monthlyIncome: event.target.value })} />
                </label>
                <label>
                  Monthly debt
                  <input required min="0" type="number" value={creditForm.monthlyDebt} onChange={(event) => setCreditForm({ ...creditForm, monthlyDebt: event.target.value })} />
                </label>
              </div>
              <div className="field-row">
                <label>
                  On-time payments
                  <input required min="0" type="number" value={creditForm.onTimePayments} onChange={(event) => setCreditForm({ ...creditForm, onTimePayments: event.target.value })} />
                </label>
                <label>
                  Missed payments
                  <input required min="0" type="number" value={creditForm.missedPayments} onChange={(event) => setCreditForm({ ...creditForm, missedPayments: event.target.value })} />
                </label>
              </div>
              <button className="primary-button" disabled={submitting || beneficiaries.length === 0} type="submit">Calculate score</button>
            </form>

            <article className={`panel score-result ${creditResult ? 'score-result-visible' : ''}`}>
              {creditResult ? (
                <>
                  <p className="eyebrow">Decision snapshot</p>
                  <strong className="score-value">{creditResult.score}</strong>
                  <span className="score-rating">{creditResult.rating}</span>
                  <span className={`eligibility ${creditResult.eligible ? 'eligible' : 'ineligible'}`}>
                    {creditResult.eligible ? 'Eligible for consideration' : 'Needs further review'}
                  </span>
                </>
              ) : (
                <p className="empty-state">Your score and eligibility decision will appear here.</p>
              )}
            </article>
          </section>
        </>
      )}
    </div>
  )
}

export default App
