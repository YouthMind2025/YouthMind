import React, { useState, useEffect } from 'react';
import { usersApi, rVectorApi, adminApi } from '../../services/api';
import { User, RVector, RVectorHistory } from '../../types';
import './Users.css';

const Users: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [filteredUsers, setFilteredUsers] = useState<User[]>([]);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  // R Vector state
  const [rVector, setRVector] = useState<RVector | null>(null);
  const [rVectorHistory, setRVectorHistory] = useState<RVectorHistory | null>(null);
  const [rVectorLoading, setRVectorLoading] = useState(false);
  const [rVectorError, setRVectorError] = useState<string | null>(null);
  
  // Reset dialog state
  const [showResetDialog, setShowResetDialog] = useState(false);
  const [resetReason, setResetReason] = useState('');
  const [resetting, setResetting] = useState(false);
  
  // Recommendation engine toggle state
  const [showToggleDialog, setShowToggleDialog] = useState(false);
  const [toggleReason, setToggleReason] = useState('');
  const [toggling, setToggling] = useState(false);

  // Load users on component mount
  useEffect(() => {
    loadUsers();
  }, []);

  // Filter users based on search term
  useEffect(() => {
    if (!searchTerm.trim()) {
      setFilteredUsers(users);
    } else {
      const filtered = users.filter(user => 
        user.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        user.lastName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        user.email.toLowerCase().includes(searchTerm.toLowerCase())
      );
      setFilteredUsers(filtered);
    }
  }, [searchTerm, users]);

  const loadUsers = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await usersApi.getAllUsers();
      
      // Handle different response structures
      let usersData: User[] = [];
      if (response.data && response.data.success && Array.isArray(response.data.data)) {
        usersData = response.data.data;
      } else if (Array.isArray(response.data)) {
        usersData = response.data;
      }
      
      setUsers(usersData);
      setFilteredUsers(usersData);
    } catch (err) {
      console.error('Error loading users:', err);
      setError('Failed to load users. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const loadUserRVector = async (userId: string) => {
    try {
      setRVectorLoading(true);
      setRVectorError(null);
      
      // Load both current R vector and history
      const [rVectorResponse, historyResponse] = await Promise.all([
        rVectorApi.getRVector(userId),
        rVectorApi.getRVectorHistory(userId, 20)
      ]);
      
      // Handle R vector response
      let rVectorData: RVector | null = null;
      if (rVectorResponse.data) {
        rVectorData = rVectorResponse.data;
      }
      
      // Handle history response
      let historyData: RVectorHistory | null = null;
      if (historyResponse.data) {
        historyData = historyResponse.data;
      }
      
      setRVector(rVectorData);
      setRVectorHistory(historyData);
    } catch (err) {
      console.error('Error loading R vector:', err);
      setRVectorError('Failed to load R vector data. Please try again.');
    } finally {
      setRVectorLoading(false);
    }
  };

  const handleUserSelect = (user: User) => {
    setSelectedUser(user);
    loadUserRVector(user.id);
  };

  const handleResetRVector = async () => {
    if (!selectedUser) return;
    
    try {
      setResetting(true);
      await rVectorApi.resetRVector(selectedUser.id, resetReason || undefined);
      
      // Reload R vector data
      await loadUserRVector(selectedUser.id);
      
      setShowResetDialog(false);
      setResetReason('');
      alert('R vector reset successfully');
    } catch (err) {
      console.error('Error resetting R vector:', err);
      alert('Failed to reset R vector. Please try again.');
    } finally {
      setResetting(false);
    }
  };

  const handleToggleRecommendationEngine = async () => {
    if (!selectedUser) return;
    
    try {
      setToggling(true);
      const newValue = !selectedUser.recommendationEngineEnabled;
      
      await adminApi.updateUserRecommendationToggle(
        selectedUser.id, 
        newValue
      );
      
      // Update the selected user's data
      const updatedUser = { ...selectedUser, recommendationEngineEnabled: newValue };
      setSelectedUser(updatedUser);
      
      // Update the user in the users list
      setUsers(prevUsers => 
        prevUsers.map(user => 
          user.id === selectedUser.id 
            ? { ...user, recommendationEngineEnabled: newValue }
            : user
        )
      );
      
      setShowToggleDialog(false);
      setToggleReason('');
      alert(`Recommendation engine ${newValue ? 'enabled' : 'disabled'} successfully`);
    } catch (err) {
      console.error('Error toggling recommendation engine:', err);
      alert('Failed to toggle recommendation engine. Please try again.');
    } finally {
      setToggling(false);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  const getEmotionColor = (emotion: string) => {
    const colors: Record<string, string> = {
      happy: '#10b981',
      sad: '#3b82f6',
      angry: '#ef4444',
      fear: '#8b5cf6',
      surprised: '#fbbf24',
      conflicted: '#6b7280'
    };
    return colors[emotion] || '#6b7280';
  };

  const isTopEmotion = (emotion: string) => {
    return selectedUser && rVector && rVector.topEmotions.includes(emotion);
  };

  const renderBarChart = () => {
    if (!rVector) return null;

    const emotions = ['happy', 'sad', 'angry', 'fear', 'surprised', 'conflicted'];
    const maxValue = Math.max(...emotions.map(emotion => rVector.weights[emotion] || 0));

    return (
      <div className="bar-chart">
        <div className="chart-title">Current R Vector</div>
        <div className="chart-container">
          {emotions.map(emotion => {
            const value = rVector.weights[emotion] || 0;
            const percentage = maxValue > 0 ? (value / maxValue) * 100 : 0;
            const isTop = isTopEmotion(emotion);
            
            return (
              <div key={emotion} className="bar-item">
                <div className="bar-label">{emotion}</div>
                <div className="bar-wrapper">
                  <div 
                    className={`bar ${isTop ? 'top-emotion' : ''}`}
                    style={{
                      height: `${percentage}%`,
                      backgroundColor: getEmotionColor(emotion)
                    }}
                  >
                    <div className="bar-value">
                      {(value * 100).toFixed(1)}%
                    </div>
                  </div>
                </div>
                {isTop && (
                  <div className="top-badge">⭐</div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    );
  };

  const renderLineChart = () => {
    if (!rVectorHistory || !rVectorHistory.history.length) return null;

    const emotions = ['happy', 'sad', 'angry', 'fear', 'surprised', 'conflicted'];
    const maxValue = 1.0; // R vector values are normalized to 0-1

    return (
      <div className="line-chart">
        <div className="chart-title">R Vector History</div>
        <div className="chart-container">
          <svg width="100%" height="300" viewBox="0 0 800 300">
            {/* Grid lines */}
            {[0, 0.25, 0.5, 0.75, 1.0].map(value => (
              <line
                key={value}
                x1="50"
                y1={250 - (value * 200)}
                x2="750"
                y2={250 - (value * 200)}
                stroke="#e5e7eb"
                strokeWidth="1"
              />
            ))}
            
            {/* Emotion lines */}
            {emotions.map((emotion, emotionIndex) => {
              const isTop = rVector && rVector.topEmotions.includes(emotion);
              const points = rVectorHistory.history.map((item, index) => {
                const x = 50 + (index * (700 / (rVectorHistory.history.length - 1)));
                const y = 250 - ((item.vector[emotionIndex] || 0) * 200);
                return `${x},${y}`;
              }).join(' ');

              return (
                <polyline
                  key={emotion}
                  points={points}
                  fill="none"
                  stroke={getEmotionColor(emotion)}
                  strokeWidth={isTop ? 3 : 1.5}
                  opacity={isTop ? 1.0 : 0.5}
                />
              );
            })}
            
            {/* Data points */}
            {rVectorHistory.history.map((item, index) => {
              const x = 50 + (index * (700 / (rVectorHistory.history.length - 1)));
              return (
                <circle
                  key={index}
                  cx={x}
                  cy={250 - ((item.vector[0] || 0) * 200)}
                  r="3"
                  fill="#3b82f6"
                />
              );
            })}
          </svg>
          
          {/* Legend */}
          <div className="chart-legend">
            {emotions.map(emotion => {
              const isTop = rVector && rVector.topEmotions.includes(emotion);
              return (
                <div key={emotion} className={`legend-item ${isTop ? 'top-emotion' : ''}`}>
                  <div 
                    className="legend-color"
                    style={{ backgroundColor: getEmotionColor(emotion) }}
                  />
                  <span>{emotion}</span>
                  {isTop && <span className="top-indicator">⭐</span>}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    );
  };

  if (loading) {
    return (
      <div className="page-container">
        <div className="page-header">
          <h1 className="page-title">Users</h1>
        </div>
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>Loading users...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <h1 className="page-title">Users</h1>
        <p className="page-subtitle">Manage users and view their recommendation vectors</p>
      </div>

      {error && (
        <div className="error-message">
          <div className="error-icon">⚠️</div>
          <p>{error}</p>
          <button onClick={loadUsers} className="retry-button">
            Retry
          </button>
        </div>
      )}

      <div className="users-layout">
        {/* User List Sidebar */}
        <div className="users-sidebar">
          <div className="search-section">
            <input
              type="text"
              placeholder="Search users..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="search-input"
            />
          </div>
          
          <div className="users-list">
            {filteredUsers.map(user => (
              <div
                key={user.id}
                className={`user-item ${selectedUser?.id === user.id ? 'selected' : ''}`}
                onClick={() => handleUserSelect(user)}
              >
                <div className="user-info">
                  <div className="user-name">
                    {user.firstName} {user.lastName}
                  </div>
                  <div className="user-email">{user.email}</div>
                  <div className="user-joined">
                    Joined: {formatDate(user.createdAt || '')}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Main Content Area */}
        <div className="main-content">
          {selectedUser ? (
            <div className="user-details">
              <div className="user-header">
                <div className="user-info">
                  <h2>{selectedUser.firstName} {selectedUser.lastName}</h2>
                  <p>{selectedUser.email}</p>
                  <p className="user-joined">Joined: {formatDate(selectedUser.createdAt || '')}</p>
                </div>
                <div className="user-actions">
                  <button
                    onClick={() => setShowResetDialog(true)}
                    className="reset-button"
                    disabled={rVectorLoading}
                  >
                    Reset R Vector
                  </button>
                </div>
              </div>

              {/* Recommendation Engine Status Section */}
              <div className="recommendation-engine-section">
                <div className="section-header">
                  <h3>Recommendation Engine</h3>
                  <div className="status-badge">
                    <span className={`status-indicator ${selectedUser.recommendationEngineEnabled ? 'enabled' : 'disabled'}`}>
                      {selectedUser.recommendationEngineEnabled ? '✓' : '⭕'}
                    </span>
                    <span className="status-text">
                      {selectedUser.recommendationEngineEnabled ? 'Enabled' : 'Disabled'}
                    </span>
                  </div>
                </div>
                <div className="toggle-section">
                  <button
                    onClick={() => setShowToggleDialog(true)}
                    className="toggle-button"
                    disabled={toggling}
                  >
                    {toggling ? 'Updating...' : `Toggle Recommendation Engine`}
                  </button>
                </div>
              </div>

              {rVectorLoading ? (
                <div className="loading-container">
                  <div className="loading-spinner"></div>
                  <p>Loading R vector data...</p>
                </div>
              ) : rVectorError ? (
                <div className="error-message">
                  <div className="error-icon">⚠️</div>
                  <p>{rVectorError}</p>
                  <button onClick={() => loadUserRVector(selectedUser.id)} className="retry-button">
                    Retry
                  </button>
                </div>
              ) : rVector ? (
                <div className="r-vector-section">
                  <div className="r-vector-info">
                    <div className="info-item">
                      <span className="info-label">Version:</span>
                      <span className="info-value">{rVector.version}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Last Updated:</span>
                      <span className="info-value">{formatDate(rVector.updatedAt)}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Total Weight:</span>
                      <span className="info-value">{rVector.totalWeight.toFixed(4)}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Top Emotions:</span>
                      <span className="info-value">
                        {rVector.topEmotions.map(emotion => (
                          <span key={emotion} className="emotion-tag" style={{ backgroundColor: getEmotionColor(emotion) }}>
                            {emotion}
                          </span>
                        ))}
                      </span>
                    </div>
                  </div>

                  {renderBarChart()}
                  {renderLineChart()}
                </div>
              ) : (
                <div className="no-data">
                  <p>No R vector data available for this user.</p>
                </div>
              )}
            </div>
          ) : (
            <div className="no-selection">
              <div className="no-selection-icon">👤</div>
              <h3>Select a User</h3>
              <p>Choose a user from the list to view their R vector data and history.</p>
            </div>
          )}
        </div>
      </div>

      {/* Reset Dialog */}
      {showResetDialog && (
        <div className="modal-overlay" onClick={() => setShowResetDialog(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Reset R Vector</h2>
              <button className="close-btn" onClick={() => setShowResetDialog(false)}>×</button>
            </div>
            <div className="modal-body">
              <p>Are you sure you want to reset <strong>{selectedUser?.firstName} {selectedUser?.lastName}</strong>'s R vector?</p>
              <p>This will reset their recommendation preferences to default values.</p>
              <div className="form-group">
                <label>Reason (optional):</label>
                <input
                  type="text"
                  value={resetReason}
                  onChange={(e) => setResetReason(e.target.value)}
                  placeholder="Enter reason for reset..."
                />
              </div>
            </div>
            <div className="modal-footer">
              <button 
                className="btn-secondary" 
                onClick={() => setShowResetDialog(false)}
                disabled={resetting}
              >
                Cancel
              </button>
              <button 
                className="btn-danger" 
                onClick={handleResetRVector}
                disabled={resetting}
              >
                {resetting ? 'Resetting...' : 'Reset R Vector'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Recommendation Engine Toggle Dialog */}
      {showToggleDialog && (
        <div className="modal-overlay" onClick={() => setShowToggleDialog(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Toggle Recommendation Engine</h2>
              <button className="close-btn" onClick={() => setShowToggleDialog(false)}>×</button>
            </div>
            <div className="modal-body">
              <div className="warning-icon">⚠️</div>
              <p><strong>Confirm Action</strong></p>
              <p>Are you sure you want to change the recommendation engine for:</p>
              
              <div className="user-info-modal">
                <p><strong>User:</strong> {selectedUser?.firstName} {selectedUser?.lastName}</p>
                <p><strong>Email:</strong> {selectedUser?.email}</p>
              </div>
              
              <div className="status-change">
                <div className="status-item">
                  <span className="status-label">Current:</span>
                  <span className={`status-value ${selectedUser?.recommendationEngineEnabled ? 'enabled' : 'disabled'}`}>
                    {selectedUser?.recommendationEngineEnabled ? '✅ Enabled' : '⭕ Disabled'}
                  </span>
                </div>
                <div className="status-item">
                  <span className="status-label">New:</span>
                  <span className={`status-value ${!selectedUser?.recommendationEngineEnabled ? 'enabled' : 'disabled'}`}>
                    {!selectedUser?.recommendationEngineEnabled ? '✅ Enabled' : '⭕ Disabled'}
                  </span>
                </div>
              </div>
              
              <div className="form-group">
                <label>Reason (optional):</label>
                <input
                  type="text"
                  value={toggleReason}
                  onChange={(e) => setToggleReason(e.target.value)}
                  placeholder="Enter reason for toggle..."
                />
              </div>
            </div>
            <div className="modal-footer">
              <button 
                className="btn-secondary" 
                onClick={() => setShowToggleDialog(false)}
                disabled={toggling}
              >
                Cancel
              </button>
              <button 
                className="btn-primary" 
                onClick={handleToggleRecommendationEngine}
                disabled={toggling}
              >
                {toggling ? 'Updating...' : 'Confirm Toggle'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Users;
