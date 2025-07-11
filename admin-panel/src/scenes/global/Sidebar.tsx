import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import './Sidebar.css';

const Sidebar: React.FC = () => {
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
  };

  // Helper to get user display name
  const getUserDisplayName = () => {
    if (user?.firstName && user?.lastName) {
      return `${user.firstName} ${user.lastName}`;
    }
    return user?.email?.split('@')[0] || 'Admin';
  };

  // Helper to get user initials for avatar
  const getUserInitials = () => {
    if (user?.firstName && user?.lastName) {
      return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
    }
    if (user?.email) {
      return user.email.charAt(0).toUpperCase();
    }
    return 'A';
  };

  return (
    <div className="sidebar">
      {/* User Info Section */}
      <div className="sidebar-section user-info">
        <div className="user-details">
          <div className="user-avatar">
            <span className="avatar-text">
              {getUserInitials()}
            </span>
          </div>
          <div className="user-text">
            <p className="user-name">{getUserDisplayName()}</p>
            <p className="user-role">Administrator</p>
            {user?.email && (
              <p className="user-email" style={{ fontSize: '0.75rem', color: '#666', marginTop: '4px' }}>
                {user.email}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Navigation Sections */}
      <div className="sidebar-section">
        <h2>Content Moderation</h2>
        <nav>
          <Link to="/posts" className="sidebar-link">
            All Posts
          </Link>
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

      <div className="sidebar-section">
        <h2>Developer Tools</h2>
        <nav>
          <Link to="/jwt-decoder" className="sidebar-link">
            JWT Decoder
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
