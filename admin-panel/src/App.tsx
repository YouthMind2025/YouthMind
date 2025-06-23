import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './scenes/global/ProtectedRoute';
import Sidebar from './scenes/global/Sidebar';
import FlaggedPosts from './scenes/pages/FlaggedPosts';
import Posts from './scenes/pages/Posts';
import SystemHealth from './scenes/pages/SystemHealth';
import Login from './scenes/pages/Login';
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
                      <Route path="/system-health" element={<SystemHealth />} />
                      <Route path="/" element={<FlaggedPosts />} />
                    </Routes>
                  </main>
                </div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
