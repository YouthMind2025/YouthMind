import React, { useState, useEffect } from 'react';
import { adminApi } from '../../services/api';
import { User, RecommendationStats } from '../../types';
import './Settings.css';

const Settings: React.FC = () => {
  const [stats, setStats] = useState<RecommendationStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Calculate statistics from users data
  const calculateStats = (users: User[]): RecommendationStats => {
    const totalUsers = users.length;
    const enabledCount = users.filter(u => u.recommendationEngineEnabled === true).length;
    const disabledCount = users.filter(u => u.recommendationEngineEnabled === false).length;
    
    return {
      totalUsers,
      enabledCount,
      disabledCount,
      enabledPercentage: totalUsers > 0 ? (enabledCount / totalUsers) * 100 : 0,
      disabledPercentage: totalUsers > 0 ? (disabledCount / totalUsers) * 100 : 0
    };
  };

  const loadStats = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const users = await adminApi.getRecommendationStats();
      const calculatedStats = calculateStats(users);
      setStats(calculatedStats);
    } catch (err) {
      console.error('Error loading recommendation stats:', err);
      setError('Failed to load recommendation statistics. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStats();
  }, []);

  const formatNumber = (num: number) => {
    return num.toLocaleString();
  };

  const formatPercentage = (num: number) => {
    return num.toFixed(1);
  };

  if (loading) {
    return (
      <div className="page-container">
        <div className="page-header">
          <h1 className="page-title">Settings</h1>
          <p className="page-subtitle">Manage system configuration</p>
        </div>
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>Loading settings...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <h1 className="page-title">Settings</h1>
        <p className="page-subtitle">Manage system configuration</p>
      </div>

      {error && (
        <div className="error-message">
          <div className="error-icon">⚠️</div>
          <p>{error}</p>
          <button onClick={loadStats} className="retry-button">
            Retry
          </button>
        </div>
      )}

      {stats && (
        <div className="settings-content">
          {/* Recommendation Engine Statistics */}
          <div className="stats-section">
            <div className="section-header">
              <h2>Recommendation Engine Statistics</h2>
              <button onClick={loadStats} className="refresh-button">
                <span className="refresh-icon">🔄</span>
                Refresh
              </button>
            </div>

            <div className="stats-grid">
              {/* Total Users Card */}
              <div className="stat-card total-card">
                <div className="stat-icon">📊</div>
                <div className="stat-content">
                  <div className="stat-label">Total Users</div>
                  <div className="stat-value">{formatNumber(stats.totalUsers)}</div>
                </div>
              </div>

              {/* Engine Enabled Card */}
              <div className="stat-card enabled-card">
                <div className="stat-icon">✅</div>
                <div className="stat-content">
                  <div className="stat-label">Engine Enabled</div>
                  <div className="stat-value">{formatNumber(stats.enabledCount)}</div>
                  <div className="stat-percentage">({formatPercentage(stats.enabledPercentage)}%)</div>
                  <div className="progress-bar">
                    <div 
                      className="progress-fill enabled"
                      style={{ width: `${stats.enabledPercentage}%` }}
                    ></div>
                  </div>
                </div>
              </div>

              {/* Engine Disabled Card */}
              <div className="stat-card disabled-card">
                <div className="stat-icon">⭕</div>
                <div className="stat-content">
                  <div className="stat-label">Engine Disabled</div>
                  <div className="stat-value">{formatNumber(stats.disabledCount)}</div>
                  <div className="stat-percentage">({formatPercentage(stats.disabledPercentage)}%)</div>
                  <div className="progress-bar">
                    <div 
                      className="progress-fill disabled"
                      style={{ width: `${stats.disabledPercentage}%` }}
                    ></div>
                  </div>
                </div>
              </div>
            </div>

            {/* Summary Information */}
            <div className="summary-info">
              <div className="summary-item">
                <span className="summary-label">Engine Adoption Rate:</span>
                <span className="summary-value">{formatPercentage(stats.enabledPercentage)}%</span>
              </div>
              <div className="summary-item">
                <span className="summary-label">Disabled Users:</span>
                <span className="summary-value">{formatNumber(stats.disabledCount)} users</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Settings;
