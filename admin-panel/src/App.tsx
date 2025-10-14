import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import ReCAPTCHA from 'react-google-recaptcha';
import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './scenes/global/ProtectedRoute';
import Sidebar from './scenes/global/Sidebar';
import FlaggedPosts from './scenes/pages/FlaggedPosts';
import Posts from './scenes/pages/Posts';
import Articles from './scenes/pages/Articles';
import Users from './scenes/pages/Users';
import SystemHealth from './scenes/pages/SystemHealth';
import JWTDecoder from './scenes/pages/JWTDecoder';
import RecommendationAnalytics from './scenes/pages/RecommendationAnalytics';
import MappingRules from './scenes/pages/MappingRules';
import FailedEvents from './scenes/pages/FailedEvents';
import Settings from './scenes/pages/Settings';
import Login from './scenes/pages/Login';
import { RECAPTCHA_CONFIG } from './config/recaptcha';
import './App.css';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          {/* Public route */}
          <Route path="/login" element={<Login />} />
          
          {/* Protected routes */}
          <Route
            path="/*"
            element={
              <ProtectedRoute>
                <div className="app-container">
                  <Sidebar />
                  <main className="main-content">
                    <Routes>
                      <Route path="/posts" element={<Posts />} />
                      <Route path="/flagged-posts" element={<FlaggedPosts />} />
                      <Route path="/articles" element={<Articles />} />
                      <Route path="/users" element={<Users />} />
                      <Route path="/system-health" element={<SystemHealth />} />
                      <Route path="/recommendation-analytics" element={<RecommendationAnalytics />} />
                      <Route path="/mapping-rules" element={<MappingRules />} />
                      <Route path="/failed-events" element={<FailedEvents />} />
                      <Route path="/settings" element={<Settings />} />
                      <Route path="/jwt-decoder" element={<JWTDecoder />} />
                      <Route path="/" element={<FlaggedPosts />} />
                    </Routes>
                  </main>
                </div>
              </ProtectedRoute>
            }
          />
        </Routes>
        
        {/* Global reCAPTCHA for sensitive actions */}
        <ReCAPTCHA
          sitekey={RECAPTCHA_CONFIG.siteKey}
          size="invisible"
          badge={RECAPTCHA_CONFIG.badge}
        />
      </Router>
    </AuthProvider>
  );
}

export default App;
