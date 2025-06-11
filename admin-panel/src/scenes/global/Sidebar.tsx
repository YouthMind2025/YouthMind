import React from 'react';
import { Link } from 'react-router-dom';
import './Sidebar.css';

const Sidebar: React.FC = () => {
  return (
    <div className="sidebar">
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
    </div>
  );
};

export default Sidebar;
