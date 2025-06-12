import React from 'react';
import './SystemHealth.css';
import { systemmonitoringmockData } from '../../data/mockData';

const SystemHealth: React.FC = () => {
  const { status, services, metrics } = systemmonitoringmockData;

  const getStatusColor = (status: string) => {
    return status === 'up' ? 'status-up' : 'status-down';
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <h1 className="page-title">System Health</h1>
      </div>
      <div className="page-content">
        <div className="server-status">
          <div className={`server-status-box ${status === 'healthy' ? 'status-up' : 'status-down'}`}>
            <h2>Server Status</h2>
            <div className="status-indicator">
              {status.toUpperCase()}
            </div>
          </div>

          <div className="services-grid">
            <div className={`status-box ${getStatusColor(services.database)}`}>
              <h2>Database</h2>
              <div className="status-indicator">
                {services.database.toUpperCase()}
              </div>
            </div>

            <div className={`status-box ${getStatusColor(services.redis)}`}>
              <h2>Redis</h2>
              <div className="status-indicator">
                {services.redis.toUpperCase()}
              </div>
            </div>

            <div className={`status-box ${getStatusColor(services.ai_service)}`}>
              <h2>AI Service</h2>
              <div className="status-indicator">
                {services.ai_service.toUpperCase()}
              </div>
            </div>
          </div>
        </div>
        <div className="metrics-section">
          <div className="metrics-grid">
            <div className="metric-box">
              <h2>Active Users</h2>
              <div className="metric-value">{metrics.active_users}</div>
            </div>
            <div className="metric-box">
              <h2>Posts Today</h2>
              <div className="metric-value">{metrics.posts_today}</div>
            </div>
            <div className="metric-box">
              <h2>AI Response Time</h2>
              <div className="metric-value">{metrics.ai_response_time_avg}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SystemHealth; 