import { useState } from 'react'
import { Activity, ArrowRight } from 'lucide-react'
import BranchExperience from './BranchExperience.jsx'
import DashboardApp from './DashboardApp.jsx'
import './EntrancePage.css'

export default function App() {
	const [stage, setStage] = useState('entrance')
	const [dashboardView, setDashboardView] = useState('Overview')

	if (stage === 'dashboard') {
		return <DashboardApp initialView={dashboardView} onBackToBranches={() => setStage('branches')} />
	}

	if (stage === 'branches') {
		return <BranchExperience onOpenDashboard={(view = 'Overview') => { setDashboardView(view); setStage('dashboard') }} />
	}

	return <main className="entrance-page">
		<div className="entrance-scene" aria-hidden="true" />
		<header className="entrance-topbar">
			<div className="entrance-brand"><span className="entrance-brand-mark"><Activity size={18} strokeWidth={2.2} /></span><span>RETAILOPS <b>AI</b></span></div>
			<div className="entrance-classification"><span /> RETAIL OPERATIONS INTELLIGENCE</div>
		</header>

		<section className="entrance-content" aria-labelledby="entrance-title">
			<div className="entrance-kicker"><span>BUILT FOR MODERN RETAIL</span><i /></div>
			<h1 id="entrance-title">RETAILOPS <span>AI</span></h1>
			<p className="entrance-subtitle">AI-Powered Retail Operations &amp; Inventory Intelligence</p>
			<p className="entrance-tagline">Turn everyday retail operations into intelligent decisions.</p>
			<button className="entrance-cta" onClick={() => setStage('branches')}>
				<span>ENTER RETAILOPS AI</span><ArrowRight size={18} strokeWidth={2} />
			</button>
		</section>

		<footer className="entrance-footer"><span>SALES</span><i /><span>INVENTORY</span><i /><span>SUPPLIER OPERATIONS</span></footer>
	</main>
}
