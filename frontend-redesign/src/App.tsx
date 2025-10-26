import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ErrorBoundary } from './components/common/ErrorBoundary';
import { HomePage } from './pages/HomePage';
import { TripWizardPage } from './pages/TripWizardPage';
import { AgentProgressPage } from './pages/AgentProgressPage';
import { DashboardPage } from './pages/DashboardPage';
import { TripDetailPage } from './pages/TripDetailPage';
import { SearchResultsPage } from './pages/SearchResultsPage';
import { LoginPage } from './pages/LoginPage';
import { SignupPage } from './pages/SignupPage';
import { ProfilePage } from './pages/ProfilePage';
import './index.css';

function App() {
  return (
    <ErrorBoundary>
      <Router>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/ai-planner" element={<TripWizardPage />} />
          <Route path="/ai-progress" element={<AgentProgressPage />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/trip/:id" element={<TripDetailPage />} />
          <Route path="/search" element={<SearchResultsPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/profile" element={<ProfilePage />} />
        </Routes>
      </Router>
    </ErrorBoundary>
  );
}

export default App;
