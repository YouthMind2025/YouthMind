import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import './Sidebar.css';

const Sidebar: React.FC = () => {
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
  };

  return (
    <div className="sidebar">
      {/* User Info Section */}
      <div className="sidebar-section user-info">
        <div className="user-details">
          <div className="user-avatar">
            <span className="avatar-text">
              {user?.username?.charAt(0).toUpperCase() || 'A'}
            </span>
          </div>
          <div className="user-text">
            <p className="user-name">{user?.username || 'Admin'}</p>
            <p className="user-role">{user?.role || 'Administrator'}</p>
          </div>
        </div>
      </div>

      {/* Navigation Sections */}
      <div className="sidebar-section">
        <h2>Content Moderation</h2>
        <nav>
          <Link to="/flagged-posts" className="sidebar-link">
            Flagged Posts
          </Link>
        </nav>
      </div>

      <div className="sidebar-section">
        <h2>System Monitoring</h2>
        <nav>
          <Link to="/system-health" className="sidebar-link">
            System Health
          </Link>
        </nav>
      </div>

      {/* Logout Section */}
      <div className="sidebar-section logout-section">
        <button
          onClick={handleLogout}
          className="logout-button"
        >
          <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
          </svg>
          Logout
        </button>
      </div>
    </div>
  );
};

export default Sidebar;
