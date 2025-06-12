import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Sidebar from './scenes/global/Sidebar';
import FlaggedPosts from './scenes/pages/FlaggedPosts';
import SystemHealth from './scenes/pages/SystemHealth';
import './App.css';

function App() {
  return (
    <Router>
      <div className="app-container">
        <Sidebar />
        <main className="main-content">
          <Routes>
            <Route path="/flagged-posts" element={<FlaggedPosts />} />
            <Route path="/system-health" element={<SystemHealth />} />
            <Route path="/" element={<FlaggedPosts />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
