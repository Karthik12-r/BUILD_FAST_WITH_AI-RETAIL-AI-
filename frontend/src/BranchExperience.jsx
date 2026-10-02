import { useEffect, useState } from 'react'
import {
  Activity, AlertTriangle, ArrowLeft, ArrowRight, ArrowUpRight, Boxes, CalendarDays,
  CheckCircle2, Clock3, Factory, LayoutDashboard, PackageCheck,
  RefreshCw, ShieldAlert, Sparkles, TrendingDown, TrendingUp,
} from 'lucide-react'
import {
  Area, AreaChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer,
  Tooltip, XAxis, YAxis,
} from 'recharts'
import './BranchExperience.css'

const currency = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })
const compactCurrency = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', notation: 'compact', maximumFractionDigits: 1 })
const number = new Intl.NumberFormat('en-IN')
const dateFormat = new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'UTC' })
const inventoryColors = ['#8db45d', '#e3ae52', '#d96856']
const branchVisuals = {
  BR01: { image: '/branch-bengaluru.jpg', accent: '#2b877b', position: 'center 52%', caption: 'CITY MARKET' },
  BR02: { image: '/branch-mysuru.jpg', accent: '#a87937', position: 'center 49%', caption: 'FRESH PRODUCE' },
  BR03: { image: '/branch-hubballi.jpg', accent: '#477a9a', position: 'center 55%', caption: 'DAILY OPERATIONS' },
  BR04: { image: '/branch-mangaluru.jpg', accent: '#c66d56', position: 'center 56%', caption: 'MARKET FLOOR' },
}
const branchDirectory = [
  { id: 'BR01', name: 'Bengaluru Central', storeIds: ['S001', 'S002'], status: 'Loading snapshot' },
  { id: 'BR02', name: 'Mysuru', storeIds: ['S003'], status: 'Loading snapshot' },
  { id: 'BR03', name: 'Hubballi', storeIds: ['S004'], status: 'Loading snapshot' },
  { id: 'BR04', name: 'Mangaluru', storeIds: ['S005'], status: 'Loading snapshot' },
]

async function getJson(path) {
  const response = await fetch(path)
  if (!response.ok) {
    const payload = await response.json().catch(() => ({}))
    const fallback = response.status >= 500
      ? 'The service is temporarily unavailable. Please try again shortly.'
      : 'The request could not be completed. Please try again.'
    throw new Error(payload.message || fallback)
  }
  return response.json()
}

function formatDate(value) {
  return value ? dateFormat.format(new Date(`${value}T00:00:00Z`)) : 'No sales records'
}

function formatChange(value) {
  if (value == null) return 'No prior-period baseline'
  const Icon = value >= 0 ? TrendingUp : TrendingDown
  return <><Icon size={13} /> {value >= 0 ? '+' : ''}{Number(value).toFixed(1)}% vs previous period</>
}

export default function BranchExperience({ onOpenDashboard }) {
  const [branches, setBranches] = useState(branchDirectory)
  const [selectedBranch, setSelectedBranch] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    let current = true
    getJson('/api/branches')
      .then((data) => {
        if (current) {
          setBranches(data)
          setError('')
        }
      })
      .catch((requestError) => { if (current) setError(requestError.message) })
      .finally(() => { if (current) setLoading(false) })
    return () => { current = false }
  }, [retry])

  return <div className="branch-experience">
    {error && <div className="branch-error" role="alert"><AlertTriangle size={16} /><span>{error}</span><button onClick={() => { setLoading(true); setRetry((value) => value + 1) }}>Retry</button></div>}
    {selectedBranch
      ? <BranchOverviewPage
          key={selectedBranch.id}
          branch={selectedBranch}
          onBack={() => setSelectedBranch(null)}
          onOpenDashboard={onOpenDashboard}
        />
      : <BranchSelectionPage
          branches={branches}
          loading={loading}
          onSelect={setSelectedBranch}
          onOpenDashboard={() => onOpenDashboard('Overview')}
        />}
  </div>
}

function Brand({ suffix }) {
  return <div className="branch-brand"><span className="branch-brand-mark"><Activity size={18} /></span><span>RETAILOPS <b>AI</b><small>{suffix}</small></span></div>
}

function BranchSelectionPage({ branches, loading, onSelect, onOpenDashboard }) {
  return <main className="branch-page branch-select-page">
    <header className="branch-topbar"><Brand suffix="NETWORK OPERATIONS" /><button className="branch-text-link" onClick={onOpenDashboard}><LayoutDashboard size={15} /> All-store dashboard <ArrowUpRight size={14} /></button></header>
    <section className="branch-selection-content">
      <div className="branch-page-intro"><div className="branch-eyebrow">NETWORK CONTROL <span>·</span> 04 LOCATIONS</div><h1>Select a Branch</h1><p>Monitor performance, inventory and operational intelligence across your retail network.</p></div>
      <div className="branch-card-grid">
        {branches.map((branch, index) => {
          const visual = branchVisuals[branch.id] || branchVisuals.BR01
          return <button type="button" className="branch-card" key={branch.id} onClick={() => onSelect(branch)} style={{ '--branch-accent': visual.accent }}>
          <div className="branch-card-photo"><img src={visual.image} alt="" style={{ objectPosition: visual.position }} /><div className="branch-card-photo-shade" /><span>{visual.caption}</span></div>
          <div className="branch-card-top"><span className="branch-index">BRANCH {String(index + 1).padStart(2, '0')}</span><span className={`branch-health-dot ${statusClass(branch.status)}`} /></div>
          <h2>{branch.name}</h2>
          <div className="branch-card-data-date"><CalendarDays size={12} /> {branch.dataAsOf ? `Latest data · ${formatDate(branch.dataAsOf)}` : loading ? 'Loading latest snapshot…' : 'Latest data unavailable'}</div>
          <div className="branch-card-metrics">
            <div><span>Latest day revenue</span><strong>{branch.latestDayRevenue == null ? '—' : compactCurrency.format(branch.latestDayRevenue)}</strong></div>
            <div><span>Units sold</span><strong>{branch.latestDayUnitsSold == null ? '—' : number.format(branch.latestDayUnitsSold)}</strong></div>
            <div><span>Stock alerts</span><strong className={branch.outOfStockItems > 0 ? 'branch-alert-value' : ''}>{branch.lowStockItems == null || branch.outOfStockItems == null ? '—' : branch.lowStockItems + branch.outOfStockItems}</strong></div>
          </div>
          <div className="branch-card-bottom"><span className={`branch-status ${statusClass(branch.status)}`}><i />{branch.status || 'Status unavailable'}</span><span className="branch-card-cta">VIEW BRANCH <ArrowRight size={15} /></span></div>
        </button>
        })}
      </div>
      <p className="branch-source-note">Branch labels are mapped to existing store records. Figures reflect the latest dates in the imported sales data.</p>
    </section>
    <footer className="branch-footer"><span>RETAILOPS AI</span><span>Sales intelligence <i /> Inventory health <i /> Manager operations</span></footer>
  </main>
}

function BranchOverviewPage({ branch, onBack, onOpenDashboard }) {
  const [period, setPeriod] = useState('30D')
  const [overview, setOverview] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [updatedAt, setUpdatedAt] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    let current = true
    getJson(`/api/branches/${encodeURIComponent(branch.id)}/overview?period=${period}`)
      .then((data) => {
        if (!current) return
        setOverview(data)
        setUpdatedAt(new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }))
        setError('')
      })
      .catch((requestError) => { if (current) setError(requestError.message) })
      .finally(() => { if (current) setLoading(false) })
    return () => { current = false }
  }, [branch.id, period, refreshKey])

  const summary = overview?.branch || branch
  const isLoading = loading || (!overview && !error) || (overview && overview.period !== period)
  const statusTone = statusClass(summary.status)
  const inventoryData = overview ? [
    { name: 'Healthy', value: overview.healthyItems },
    { name: 'Low stock', value: overview.lowStockItems },
    { name: 'Out of stock', value: overview.outOfStockItems },
  ].filter((item) => item.value > 0) : []
  const inventoryTotal = inventoryData.reduce((total, item) => total + item.value, 0)

  return <main className="branch-page branch-overview-page">
    <header className="branch-topbar branch-overview-topbar">
      <Brand suffix="BRANCH OPERATIONS" />
      <div className="branch-top-actions"><button className="branch-text-link" onClick={onBack}><ArrowLeft size={15} /> Back to branches</button><button className="branch-dashboard-button" onClick={() => onOpenDashboard('Overview')}><LayoutDashboard size={15} /> Open full dashboard</button></div>
    </header>

    <section className="branch-overview-content">
      <div className="branch-detail-heading">
        <div><div className="branch-eyebrow">BRANCH OPERATIONS OVERVIEW <span>·</span> {summary.storeIds?.join(' / ')}</div><h1>{summary.name}</h1><p>Live operational picture from the latest available branch records.</p></div>
        <div className={`branch-status-large ${statusTone}`}><i />{summary.status}</div>
      </div>
      <div className="branch-data-freshness"><span><CalendarDays size={14} /> Data through <strong>{formatDate(summary.dataAsOf)}</strong></span><span><Clock3 size={14} /> Retrieved {updatedAt || '—'}</span><button aria-label="Refresh branch data" title="Refresh branch data" onClick={() => { setLoading(true); setRefreshKey((value) => value + 1) }}><RefreshCw size={14} /></button></div>

      {error && <div className="branch-error" role="alert"><AlertTriangle size={16} /><span>{error}</span></div>}
      {isLoading ? <BranchOverviewSkeleton /> : overview && <>
        <section className="branch-kpi-grid" aria-label="Branch key performance indicators">
          <KpiCard label={`${period} revenue`} value={currency.format(overview.revenue)} detail={formatChange(overview.revenueChangePercent)} icon={Activity} tone="green" />
          <KpiCard label="Units sold" value={number.format(overview.unitsSold)} detail={formatChangeText(percentage(overview.unitsSold, overview.previousUnitsSold))} icon={Boxes} tone="blue" />
          <KpiCard label="Avg. sales row" value={currency.format(overview.averageSalesRowValue)} detail="Revenue ÷ product-day records" icon={TrendingUp} tone="lime" />
          <KpiCard label="Inventory value" value={compactCurrency.format(overview.inventoryValue)} detail="Latest stock × supplier unit cost" icon={PackageCheck} tone="amber" />
          <KpiCard label="Low stock" value={number.format(overview.lowStockItems)} detail="Below lead-time coverage" icon={AlertTriangle} tone="amber" attention={overview.lowStockItems > 0} />
          <KpiCard label="Out of stock" value={number.format(overview.outOfStockItems)} detail="Requires replenishment" icon={ShieldAlert} tone="red" attention={overview.outOfStockItems > 0} />
        </section>

        <section className="branch-main-grid">
          <article className="branch-panel branch-revenue-panel">
            <div className="branch-panel-heading"><div><div className="branch-eyebrow">SALES PERFORMANCE</div><h2>Sales Revenue</h2><p>{formatDate(overview.periodStart)} – {formatDate(overview.periodEnd)}</p></div><PeriodPicker period={period} onChange={setPeriod} /></div>
            <div className="branch-chart-legend"><span><i className="legend-current" /> Selected period</span><span><i className="legend-previous" /> Previous period</span></div>
            <div className="branch-revenue-chart"><ResponsiveContainer width="100%" height="100%"><AreaChart data={overview.salesTrend} margin={{ top: 10, right: 10, bottom: 0, left: 3 }}>
              <defs><linearGradient id="branchRevenueFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#77a957" stopOpacity={0.28} /><stop offset="95%" stopColor="#77a957" stopOpacity={0.015} /></linearGradient></defs>
              <CartesianGrid vertical={false} stroke="#e8ede7" />
              <XAxis dataKey="date" tickFormatter={(value) => value.slice(5)} tickLine={false} axisLine={false} tick={{ fill: '#858d86', fontSize: 10 }} minTickGap={28} />
              <YAxis tickFormatter={(value) => compactCurrency.format(value)} tickLine={false} axisLine={false} tick={{ fill: '#858d86', fontSize: 10 }} width={58} />
              <Tooltip labelFormatter={(value) => formatDate(value)} formatter={(value, name) => [currency.format(value), name === 'revenue' ? 'Selected period' : 'Previous period']} contentStyle={{ border: '1px solid #dce4dc', borderRadius: 5, fontSize: 11 }} />
              <Area type="monotone" dataKey="previousRevenue" stroke="#aab4aa" strokeWidth={1.5} strokeDasharray="4 4" fill="transparent" activeDot={false} />
              <Area type="monotone" dataKey="revenue" stroke="#527f3d" strokeWidth={2.5} fill="url(#branchRevenueFill)" activeDot={{ r: 4, fill: '#527f3d', stroke: '#fff', strokeWidth: 2 }} />
            </AreaChart></ResponsiveContainer></div>
            <div className="branch-chart-foot"><span>Highest · {formatDate(maxPoint(overview.salesTrend)?.date)} <strong>{currency.format(maxPoint(overview.salesTrend)?.revenue || 0)}</strong></span><span>Lowest · {formatDate(minPoint(overview.salesTrend)?.date)} <strong>{currency.format(minPoint(overview.salesTrend)?.revenue || 0)}</strong></span><span>Previous-period baseline <strong>{currency.format(overview.performanceBaseline)}</strong></span></div>
          </article>

          <article className="branch-panel branch-inventory-panel">
            <div className="branch-panel-heading"><div><div className="branch-eyebrow">CURRENT STOCK POSITION</div><h2>Inventory Health</h2><p>Latest record per store and product</p></div></div>
            <div className="inventory-chart-wrap">
              {inventoryTotal > 0 ? <><ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={inventoryData} dataKey="value" nameKey="name" innerRadius="67%" outerRadius="88%" paddingAngle={3} stroke="none"><Cell fill={inventoryColors[0]} /><Cell fill={inventoryColors[1]} /><Cell fill={inventoryColors[2]} /></Pie><Tooltip formatter={(value, name) => [number.format(value), name]} contentStyle={{ border: '1px solid #dce4dc', borderRadius: 5, fontSize: 11 }} /></PieChart></ResponsiveContainer><div className="inventory-chart-center"><strong>{number.format(inventoryTotal)}</strong><span>items tracked</span></div></> : <div className="inventory-empty">No current stock records</div>}
            </div>
            <div className="inventory-legend"><InventoryLegend color="healthy" label="Healthy stock" value={overview.healthyItems} /><InventoryLegend color="low" label="Low stock" value={overview.lowStockItems} /><InventoryLegend color="out" label="Out of stock" value={overview.outOfStockItems} /></div>
          </article>
        </section>

        <section className="branch-secondary-grid">
          <article className="branch-panel branch-products-panel"><div className="branch-panel-heading"><div><div className="branch-eyebrow">PRODUCT PERFORMANCE</div><h2>Top Performing Products</h2><p>Units and revenue during the selected period</p></div></div>
            <div className="branch-product-list">{overview.topProducts.length ? overview.topProducts.map((product, index) => <ProductRow key={product.productId} product={product} rank={index + 1} maxUnits={overview.topProducts[0].unitsSold} />) : <EmptyMessage>No sales records for this period</EmptyMessage>}</div>
          </article>
          <article className="branch-intelligence-panel"><div className="intelligence-label"><Sparkles size={15} /> RETAILOPS AI INTELLIGENCE</div><h2>Signals from this branch</h2><p className="intelligence-copy">{overview.insight}</p><div className="intelligence-foot">RULE-BASED INSIGHT <span>·</span> GENERATED FROM BRANCH DATA</div></article>
        </section>

        <section className="branch-panel branch-performance-panel"><div className="branch-panel-heading"><div><div className="branch-eyebrow">PERIOD COMPARISON</div><h2>Branch Performance</h2><p>Compared with the previous {period} period; no manager-set sales target is configured.</p></div><div className="performance-achievement">{overview.achievementPercent == null ? '—' : `${Number(overview.achievementPercent).toFixed(1)}%`}<small>of baseline</small></div></div>
          <div className="performance-values"><span>Current revenue <strong>{currency.format(overview.revenue)}</strong></span><span>Previous-period baseline <strong>{currency.format(overview.performanceBaseline)}</strong></span></div>
          <div className="performance-track"><i style={{ width: `${Math.min(Math.max(Number(overview.achievementPercent || 0), 0), 100)}%` }} /></div>
        </section>

        <section className="branch-attention-section"><div className="branch-section-heading"><div><div className="branch-eyebrow">PRIORITIZE THE NEXT ACTION</div><h2>Manager Attention Required</h2></div><span>{overview.outOfStockItems + overview.lowStockItems + overview.pendingApprovals} open signals</span></div>
          <div className="attention-grid">
            <AttentionCard tone="out" icon={ShieldAlert} count={overview.outOfStockItems} title="Out-of-stock products" detail="Replenishment may be needed." action="View inventory" onClick={() => onOpenDashboard('Inventory')} />
            <AttentionCard tone="low" icon={AlertTriangle} count={overview.lowStockItems} title="Low-stock products" detail="Below existing supplier lead-time coverage." action="Review recommendations" onClick={() => onOpenDashboard('Recommendations')} />
            <AttentionCard tone="pending" icon={Factory} count={overview.pendingApprovals} title="Pending approvals" detail="Manager reorder decisions awaiting review." action="Review approvals" onClick={() => onOpenDashboard('Approvals')} />
          </div>
        </section>

        <section className="branch-panel branch-activity-panel"><div className="branch-panel-heading"><div><div className="branch-eyebrow">RECORDED ACTIVITY</div><h2>Recent Operations</h2><p>Existing approval history and sales-data coverage</p></div></div>
          <div className="branch-activity-list">{overview.recentOperations.map((operation, index) => <div className="branch-activity-row" key={`${operation.type}-${operation.timestamp}-${index}`}><span className={`activity-icon ${operation.status.toLowerCase()}`}>{operation.type === 'SALES DATA' ? <RefreshCw size={14} /> : <CheckCircle2 size={14} />}</span><div><strong>{operation.detail}</strong><span>{operation.status}</span></div><time>{operation.timestamp}</time></div>)}</div>
        </section>

        <p className="branch-data-caveat">Source data ends {formatDate(summary.dataAsOf)}. Average sales row is a product-day value proxy; this dataset has no order IDs for true AOV.</p>
      </>}
    </section>
    <footer className="branch-footer"><span>RETAILOPS AI</span><span>{summary.name} <i /> DATA AS OF {formatDate(summary.dataAsOf).toUpperCase()}</span></footer>
  </main>
}

function KpiCard({ label, value, detail, icon: Icon, tone, attention = false }) {
  return <article className={`branch-kpi-card ${tone}${attention ? ' has-attention' : ''}`}><div className="kpi-card-top"><span>{label}</span><Icon size={16} /></div><strong className="kpi-value">{value}</strong><span className="kpi-detail">{detail}</span></article>
}

function BranchOverviewSkeleton() {
  return <div className="branch-overview-skeleton" aria-label="Loading branch metrics">
    <div className="branch-skeleton-kpis">{Array.from({ length: 6 }, (_, index) => <div className="branch-skeleton-block kpi-skeleton" key={index}><i /><i /><i /></div>)}</div>
    <div className="branch-skeleton-main"><div className="branch-skeleton-block chart-skeleton"><i /><i /><i /><i /></div><div className="branch-skeleton-block inventory-skeleton"><i /><i /><i /></div></div>
    <div className="branch-skeleton-main"><div className="branch-skeleton-block products-skeleton"><i /><i /><i /><i /></div><div className="branch-skeleton-block insight-skeleton"><i /><i /><i /></div></div>
    <div className="branch-skeleton-block attention-skeleton"><i /><i /><i /></div>
  </div>
}

function PeriodPicker({ period, onChange }) {
  return <div className="period-picker" role="group" aria-label="Sales period">{['7D', '30D', '90D'].map((item) => <button key={item} className={period === item ? 'selected' : ''} aria-pressed={period === item} onClick={() => onChange(item)}>{item}</button>)}</div>
}

function InventoryLegend({ color, label, value }) {
  return <div className="inventory-legend-row"><span className={`inventory-dot ${color}`} />{label}<strong>{number.format(value)}</strong></div>
}

function ProductRow({ product, rank, maxUnits }) {
  const width = maxUnits > 0 ? `${Math.max((product.unitsSold / maxUnits) * 100, 3)}%` : '0%'
  return <div className="branch-product-row"><span className="product-rank">{String(rank).padStart(2, '0')}</span><div className="product-details"><div className="product-title-line"><strong>{product.productName}</strong><span>{product.category}</span></div><div className="product-performance-track"><i style={{ width }} /></div></div><div className="product-result"><strong>{number.format(product.unitsSold)}</strong><span>units</span></div><div className="product-revenue">{compactCurrency.format(product.revenue)}</div></div>
}

function AttentionCard({ tone, icon: Icon, count, title, detail, action, onClick }) {
  return <article className={`attention-card ${tone}`}><div className="attention-icon"><Icon size={17} /></div><div className="attention-count">{number.format(count)}</div><h3>{title}</h3><p>{detail}</p><button onClick={onClick}>{action}<ArrowRight size={14} /></button></article>
}

function EmptyMessage({ children }) { return <div className="branch-empty-message">{children}</div> }

function maxPoint(points) { return points.reduce((max, point) => !max || point.revenue > max.revenue ? point : max, null) }

function percentage(current, previous) {
  if (!previous) return null
  return ((current - previous) / previous) * 100
}

function formatChangeText(value) {
  if (value == null) return 'No prior-period baseline'
  return `${value >= 0 ? '↑' : '↓'} ${Math.abs(value).toFixed(1)}% vs previous period`
}

function minPoint(points) { return points.reduce((min, point) => !min || point.revenue < min.revenue ? point : min, null) }

function statusClass(status) {
  if (status === 'Loading snapshot') return 'loading'
  if (status === 'Critical') return 'critical'
  if (status === 'Needs attention') return 'needs-attention'
  return 'normal'
}
