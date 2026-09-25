import { useEffect, useState } from 'react'
import './App.css'

const API_BASE = '/api'

async function requestJson(path) {
  const response = await fetch(`${API_BASE}${path}`)

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
        </>
      )}
    </div>
  )
}

export default App
