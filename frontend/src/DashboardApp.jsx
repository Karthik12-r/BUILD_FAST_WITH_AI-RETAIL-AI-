import { useEffect, useState } from 'react'
import {
  Activity, Bot, Boxes, Check, ChevronRight, CircleAlert, ClipboardCheck, Clock3,
  Factory, LayoutDashboard, LoaderCircle, RefreshCw, Search, Sparkles, TrendingUp, X,
} from 'lucide-react'
import {
  Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import './DashboardApp.css'

const navigation = [
  { label: 'Overview', icon: LayoutDashboard },
  { label: 'Sales', icon: TrendingUp },
  { label: 'Inventory', icon: Boxes },
  { label: 'Recommendations', icon: Sparkles },
  { label: 'Approvals', icon: ClipboardCheck },
  { label: 'Suppliers', icon: Factory },
  { label: 'Assistant', icon: Bot },
]

const money = new Intl.NumberFormat('en-US', {
  style: 'currency', currency: 'USD', maximumFractionDigits: 0,
})

async function getJson(path, options) {
  const response = await fetch(path, options)
  if (!response.ok) {
    const payload = await response.json().catch(() => ({}))
    throw new Error(payload.detail || payload.message || `Request failed (${response.status})`)
  }
  return response.json()
}

function DashboardApp() {
  const [activeView, setActiveView] = useState('Overview')
  const [overview, setOverview] = useState(null)
  const [sales, setSales] = useState([])
  const [suppliers, setSuppliers] = useState([])
  const [approvals, setApprovals] = useState([])
  const [recommendations, setRecommendations] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [decision, setDecision] = useState(null)
  const [approver, setApprover] = useState('')
  const [managerComment, setManagerComment] = useState('')
  const [actionError, setActionError] = useState('')
  const [busy, setBusy] = useState(false)
  const [assistantQuestion, setAssistantQuestion] = useState('')
  const [assistantMessages, setAssistantMessages] = useState([])
  const [assistantBusy, setAssistantBusy] = useState(false)

  useEffect(() => {
    let current = true

    Promise.all([
      getJson('/api/analytics/dashboard'),
      getJson('/api/sales?page=0&size=12'),
      getJson('/api/suppliers'),
      getJson('/api/approvals'),
      getJson('/api/recommendations'),
    ])
      .then(([dashboardData, salesData, supplierData, approvalData, recommendationData]) => {
        if (!current) return
        setOverview(dashboardData)
        setSales(salesData)
        setSuppliers(supplierData)
        setApprovals(approvalData)
        setRecommendations(recommendationData)
        setLoadError('')
      })
      .catch((error) => {
        if (current) setLoadError(error.message)
      })
      .finally(() => {
        if (current) setLoading(false)
      })

    return () => { current = false }
  }, [refreshKey])

  const pendingApprovals = approvals.filter((approval) => approval.status === 'PENDING')

  async function submitDecision(event) {
    event.preventDefault()
    if (!decision) return
    setBusy(true)
    setActionError('')
    const action = decision.type.toLowerCase()

    try {
      await getJson(`/api/approvals/${encodeURIComponent(decision.actionId)}/${action}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ approver, managerComment }),
      })
      setDecision(null)
      setApprover('')
      setManagerComment('')
      setRefreshKey((value) => value + 1)
    } catch (error) {
      setActionError(error.message)
    } finally {
      setBusy(false)
    }
  }

  async function createApprovals() {
    setBusy(true)
    setActionError('')
    try {
      await getJson('/api/approvals/generate', { method: 'POST' })
      setRefreshKey((value) => value + 1)
    } catch (error) {
      setActionError(error.message)
    } finally {
      setBusy(false)
    }
  }

  async function askAssistant(event) {
    event.preventDefault()
    const question = assistantQuestion.trim()
    if (!question) return
    setAssistantQuestion('')
    setAssistantMessages((messages) => [...messages, { role: 'user', content: question }])
    setAssistantBusy(true)

    try {
      const response = await getJson('/api/assistant/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question }),
      })
      setAssistantMessages((messages) => [...messages, { role: 'assistant', content: response.answer }])
    } catch (error) {
      setAssistantMessages((messages) => [...messages, { role: 'error', content: error.message }])
    } finally {
      setAssistantBusy(false)
    }
  }

  function renderContent() {
    if (loading && !overview) {
      return <div className="loading-state"><LoaderCircle className="spin" size={22} /> Loading live retail data</div>
    }
    if (activeView === 'Sales') {
      return <section className="panel table-panel"><PanelHeading title="Latest sales records" detail="Most recent imported transactions" /><SalesTable sales={sales} /></section>
    }
    if (activeView === 'Inventory') {
      return <RecommendationPanel recommendations={recommendations} title="Inventory alerts" detail="Latest stock position compared with supplier lead-time coverage" />
    }
    if (activeView === 'Recommendations') {
      return <RecommendationPanel recommendations={recommendations} title="Reorder recommendations" detail="Generated from latest store/product data and supplier terms" onCreateApprovals={createApprovals} busy={busy} />
    }
    if (activeView === 'Approvals') return <ApprovalsPanel approvals={approvals} onDecide={setDecision} />
    if (activeView === 'Suppliers') return <SuppliersPanel suppliers={suppliers} />
    if (activeView === 'Assistant') return <AssistantPanel messages={assistantMessages} question={assistantQuestion} setQuestion={setAssistantQuestion} onSubmit={askAssistant} busy={assistantBusy} />

    return <>
      <section className="metric-grid">
        <Metric label="Sales revenue" value={money.format(overview?.totalRevenue ?? 0)} detail="Recorded sales value" icon={TrendingUp} tone="mint" />
        <Metric label="Units sold" value={Number(overview?.totalUnitsSold ?? 0).toLocaleString()} detail="Across imported sales history" icon={Activity} tone="blue" />
        <Metric label="Reorder alerts" value={overview?.reorderAlerts ?? 0} detail="No pending action for this item" icon={CircleAlert} tone="coral" />
        <Metric label="Pending approvals" value={overview?.pendingApprovals ?? 0} detail={`${overview?.supplierCount ?? 0} supplier records`} icon={Clock3} tone="lime" />
      </section>
      <section className="overview-grid">
        <SalesTrend data={overview?.salesTrend ?? []} />
        <TopProducts products={overview?.topProducts ?? []} />
      </section>
      <section className="content-grid">
        <RecommendationPanel recommendations={recommendations.slice(0, 8)} title="Priority inventory alerts" detail={`${recommendations.length} recommendations from current data`} compact />
        <ApprovalsPanel approvals={pendingApprovals.slice(0, 6)} compact onDecide={setDecision} />
      </section>
    </>
  }

  const heading = activeView === 'Overview' ? 'Operations overview' : activeView

  return <div className="app-shell">
    <aside className="sidebar">
      <a className="brand" href="#overview" onClick={() => setActiveView('Overview')}>
        <span className="brand-mark"><Activity size={21} strokeWidth={2.5} /></span>
        <span>retailops<span className="brand-ai">.ai</span><small>OPERATIONS INTELLIGENCE</small></span>
      </a>
      <div className="nav-caption">WORKSPACE</div>
      <nav className="primary-nav" aria-label="Main navigation">
        {navigation.map(({ label, icon: Icon }) => <button className={activeView === label ? 'nav-item active' : 'nav-item'} key={label} onClick={() => setActiveView(label)}>
          <Icon size={18} strokeWidth={1.8} /><span>{label}</span>
          {label === 'Approvals' && pendingApprovals.length > 0 && <span className="nav-count">{pendingApprovals.length}</span>}
        </button>)}
      </nav>
      <div className="sidebar-bottom"><div className="system-status"><span className={loadError ? 'status-dot disconnected' : 'status-dot'} /><span>{loadError ? 'Data connection needs attention' : 'Connected to live data'}</span></div><div className="sidebar-foot">RETAILOPS AI <span>·</span> DEMO ENVIRONMENT</div></div>
    </aside>

    <main className="main-content">
      <header className="topbar">
        <div className="breadcrumbs"><span>Workspace</span><ChevronRight size={14} /><strong>{heading}</strong></div>
        <div className="topbar-actions"><button className="icon-button" aria-label="Refresh data" title="Refresh data" onClick={() => setRefreshKey((value) => value + 1)}><RefreshCw size={17} /></button><div className="avatar" aria-label="Manager">RM</div></div>
      </header>

      <div className="page-wrap">
        <div className="page-heading">
          <div><div className="eyebrow">RETAIL OPERATIONS <span>·</span> LIVE</div><h1>{heading}</h1><p>Sales, stock coverage, and manager actions from your imported data.</p></div>
          <div className="data-stamp"><span className={loadError ? 'status-dot disconnected' : 'status-dot'} /> {loadError ? 'CONNECTION ERROR' : 'DATABASE ONLINE'}</div>
        </div>
        {loadError && <div className="error-banner"><CircleAlert size={17} /><span>{loadError}</span><button onClick={() => setRefreshKey((value) => value + 1)}>Retry</button></div>}
        {actionError && <div className="error-banner"><CircleAlert size={17} /><span>{actionError}</span><button aria-label="Dismiss" onClick={() => setActionError('')}><X size={16} /></button></div>}
        {renderContent()}
        <footer className="page-footer"><span>RETAILOPS AI</span><span>Inventory decisions grounded in recorded sales and supplier data</span></footer>
      </div>
    </main>

    {decision && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setDecision(null) }}>
      <form className="decision-modal" onSubmit={submitDecision}>
        <div className="modal-head"><div><div className="eyebrow">MANAGER ACTION</div><h2>{decision.type === 'APPROVED' ? 'Approve reorder' : 'Reject reorder'}</h2></div><button type="button" className="icon-button" aria-label="Close" onClick={() => setDecision(null)}><X size={18} /></button></div>
        <p className="modal-summary">{decision.actionId} <span>·</span> {decision.productId} <span>·</span> {decision.storeId}</p>
        <label>Manager name<input value={approver} onChange={(event) => setApprover(event.target.value)} required maxLength={100} autoFocus /></label>
        <label>{decision.type === 'REJECTED' ? 'Reason for rejection' : 'Comment'}<textarea value={managerComment} onChange={(event) => setManagerComment(event.target.value)} required={decision.type === 'REJECTED'} rows={3} maxLength={1000} /></label>
        <div className="modal-actions"><button type="button" className="button-secondary" onClick={() => setDecision(null)}>Cancel</button><button className={decision.type === 'APPROVED' ? 'button-primary' : 'button-danger'} disabled={busy}>{busy ? 'Saving…' : decision.type === 'APPROVED' ? 'Confirm approval' : 'Confirm rejection'}</button></div>
      </form>
    </div>}
  </div>
}

function Metric({ label, value, detail, icon: Icon, tone }) {
  return <article className="metric"><div className={`metric-icon ${tone}`}><Icon size={18} /></div><div className="metric-label">{label}</div><div className="metric-value">{value}</div><div className="metric-detail">{detail}</div></article>
}

function PanelHeading({ title, detail, action }) {
  return <div className="panel-heading"><div><h2>{title}</h2><p>{detail}</p></div>{action}</div>
}

function SalesTrend({ data }) {
  return <section className="panel trend-panel"><PanelHeading title="Sales trend" detail="Monthly revenue · full imported history" action={<span className="period-tag">LAST 12 MONTHS</span>} />
    <div className="chart-wrap">{data.length ? <ResponsiveContainer width="100%" height="100%"><AreaChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
      <CartesianGrid vertical={false} stroke="#e7ece9" /><XAxis dataKey="month" tickLine={false} axisLine={false} tick={{ fill: '#7b8883', fontSize: 11 }} tickFormatter={(value) => value.slice(2)} minTickGap={22} />
      <YAxis tickLine={false} axisLine={false} tick={{ fill: '#7b8883', fontSize: 11 }} tickFormatter={(value) => `$${Math.round(value / 1000)}k`} width={42} />
      <Tooltip formatter={(value) => [money.format(value), 'Revenue']} labelFormatter={(label) => `Month ${label}`} contentStyle={{ border: '1px solid #dfe6e2', borderRadius: 6, fontSize: 12 }} />
      <Area type="monotone" dataKey="revenue" stroke="#087f68" strokeWidth={2.5} fill="#d6eee5" activeDot={{ r: 4, fill: '#087f68', stroke: '#fff', strokeWidth: 2 }} />
    </AreaChart></ResponsiveContainer> : <EmptyState label="No sales trend data available" />}</div>
  </section>
}

function TopProducts({ products }) {
  const maxUnits = Math.max(...products.map((product) => product.unitsSold), 1)
  return <section className="panel top-products"><PanelHeading title="Top products" detail="Units sold across all stores" />
    <div className="product-rank-list">{products.map((product, index) => <div className="product-rank" key={product.productId}><span className="rank-index">{String(index + 1).padStart(2, '0')}</span><span className="rank-product">{product.productId}</span><span className="rank-bar"><i style={{ width: `${(product.unitsSold / maxUnits) * 100}%` }} /></span><span className="rank-units">{Number(product.unitsSold).toLocaleString()}</span></div>)}</div>
  </section>
}

function RecommendationPanel({ recommendations, title, detail, compact = false, onCreateApprovals, busy = false }) {
  return <section className="panel table-panel"><PanelHeading title={title} detail={detail} action={onCreateApprovals && <button className="button-primary button-small" onClick={onCreateApprovals} disabled={busy || recommendations.length === 0}>{busy ? 'Creating…' : 'Create pending approvals'}</button>} />
    {recommendations.length ? <div className="table-scroll"><table><thead><tr><th>PRODUCT / STORE</th><th>STOCK</th><th>DEMAND COVERAGE</th><th>REORDER QTY</th><th>PRIORITY</th><th>WHY</th></tr></thead><tbody>{recommendations.map((item) => <tr key={`${item.storeId}-${item.productId}`}><td><strong>{item.productId}</strong><small>{item.storeId} · {item.asOf}</small></td><td>{item.inventoryLevel}</td><td>{item.dailyDemand} / day · {item.leadTimeDays} days</td><td className="quantity-cell">{item.recommendedQuantity.toLocaleString()}<small>MOQ {item.minimumOrderQuantity}</small></td><td><span className={`priority ${item.priority.toLowerCase()}`}>{item.priority}</span></td><td className={compact ? 'reason-cell compact-reason' : 'reason-cell'}>{item.reason}</td></tr>)}</tbody></table></div> : <EmptyState label="No reorder recommendations right now" />}
  </section>
}

function ApprovalsPanel({ approvals, compact = false, onDecide = () => {} }) {
  return <section className="panel table-panel approvals-panel"><PanelHeading title={compact ? 'Pending approvals' : 'Manager approvals'} detail={`${approvals.length} ${compact ? 'awaiting review' : 'records'}`} />
    {approvals.length ? <div className="approval-list">{approvals.map((approval) => <div className="approval-row" key={approval.actionId}><div className="approval-glyph"><ClipboardCheck size={17} /></div><div className="approval-main"><strong>{approval.productId} <span>·</span> {approval.storeId}</strong><small>{approval.actionId} · {approval.recommendedQuantity?.toLocaleString()} units · {approval.approvalDate}</small><p>{approval.reason}</p></div><span className={`status-pill ${approval.status.toLowerCase()}`}>{approval.status}</span>{approval.status === 'PENDING' && <div className="approval-actions"><button title="Approve" aria-label={`Approve ${approval.actionId}`} onClick={() => onDecide({ ...approval, type: 'APPROVED' })}><Check size={16} /></button><button title="Reject" aria-label={`Reject ${approval.actionId}`} onClick={() => onDecide({ ...approval, type: 'REJECTED' })}><X size={16} /></button></div>}</div>)}</div> : <EmptyState label="No approvals to show" />}
  </section>
}

function SuppliersPanel({ suppliers }) {
  return <section className="panel table-panel"><PanelHeading title="Supplier directory" detail={`${suppliers.length} supplier records from MySQL`} /><div className="table-scroll"><table><thead><tr><th>SUPPLIER</th><th>PRODUCT</th><th>LEAD TIME</th><th>MINIMUM ORDER</th><th>UNIT COST</th><th>CONTACT</th></tr></thead><tbody>{suppliers.map((supplier) => <tr key={supplier.supplierId}><td><strong>{supplier.supplierName}</strong><small>{supplier.supplierId}</small></td><td>{supplier.productId}</td><td>{supplier.leadTimeDays} days</td><td>{supplier.minimumOrderQuantity}</td><td>{money.format(supplier.unitCost)}</td><td><span>{supplier.email}</span><small>{supplier.contactNumber}</small></td></tr>)}</tbody></table></div></section>
}

function SalesTable({ sales }) {
  return <div className="table-scroll"><table><thead><tr><th>DATE</th><th>PRODUCT</th><th>STORE</th><th>CATEGORY</th><th>UNITS SOLD</th><th>INVENTORY</th><th>DEMAND</th><th>PRICE</th></tr></thead><tbody>{sales.map((sale) => <tr key={sale.id}><td>{sale.date}</td><td><strong>{sale.productId}</strong></td><td>{sale.storeId}</td><td>{sale.category}</td><td>{sale.unitsSold}</td><td>{sale.inventoryLevel}</td><td>{sale.demand}</td><td>{money.format(sale.price)}</td></tr>)}</tbody></table></div>
}

function AssistantPanel({ messages, question, setQuestion, onSubmit, busy }) {
  return <section className="assistant-layout">
    <div className="assistant-intro"><div className="assistant-emblem"><Bot size={21} /></div><div><div className="eyebrow">RETAILOPS DATA ASSISTANT</div><h2>Ask about your operation</h2><p>Answers use current sales, supplier terms, reorder recommendations, and pending approvals.</p></div></div>
    <div className="provider-notice"><span className="status-dot muted-dot" /><span>External AI provider required</span><code>AI_API_KEY</code><small>No generated answer is shown while the provider is unavailable.</small></div>
    <div className="assistant-thread" aria-live="polite">
      {messages.length === 0 && <div className="assistant-empty"><Sparkles size={18} /><span>Try a question grounded in the connected data</span><div className="suggestion-list"><span>Which products need reordering?</span><span>Show pending approvals</span><span>Which supplier serves P0018?</span></div></div>}
      {messages.map((message, index) => <div className={`chat-message ${message.role}`} key={`${message.role}-${index}`}><strong>{message.role === 'user' ? 'YOU' : message.role === 'error' ? 'PROVIDER STATUS' : 'RETAILOPS ASSISTANT'}</strong><p>{message.content}</p></div>)}
      {busy && <div className="chat-message assistant"><strong>RETAILOPS ASSISTANT</strong><p><LoaderCircle className="spin" size={15} /> Checking current data with the configured provider…</p></div>}
    </div>
    <form className="assistant-form" onSubmit={onSubmit}><textarea aria-label="Ask a retail operations question" placeholder="Ask about sales, inventory, suppliers, or approvals…" value={question} onChange={(event) => setQuestion(event.target.value)} rows={2} maxLength={1000} disabled={busy} /><button className="button-primary" disabled={busy || !question.trim()}>Ask assistant <ChevronRight size={15} /></button></form>
  </section>
}

function EmptyState({ label }) {
  return <div className="empty-state"><Search size={20} /><span>{label}</span></div>
}

export default DashboardApp
