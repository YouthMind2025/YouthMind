import React, { useState, useEffect } from 'react';
import { psychologicalApi, usersApi } from '../services/api';
import './UserPsychologicalState.css';

interface User {
  id: string;
  email: string;
  username: string;
  firstName?: string;
  lastName?: string;
  createdAt: string;
}

interface PsychologicalState {
  stress: number;
  mindfulness: number;
  compassion: number;
  equanimity: number;
  intensityFactor: number;
  timestamp: number;
  status?: string;
}

interface BuddhistFactors {
  mindfulness: number;
  compassion: number;
  equanimity: number;
  nonAttachment: number;
  wisdom: number;
  vigor: number;
  faith: number;
  innerShame: number;
  shame: number;
  absenceOfCraving: number;
  absenceOfHatred: number;
  absenceOfIgnorance: number;
  lightEase: number;
  nonLaxness: number;
  renunciation: number;
  nonHarming: number;
  timestamp: number;
}

interface StateHistoryEntry {
  vector: number[];
  buddhistFactors: BuddhistFactors;
  timestamp: string;
}

const UserPsychologicalStatePage: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [psychologicalState, setPsychologicalState] = useState<PsychologicalState | null>(null);
  const [buddhistFactors, setBuddhistFactors] = useState<BuddhistFactors | null>(null);
  const [stateHistory, setStateHistory] = useState<StateHistoryEntry[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'current' | 'history' | 'trends'>('current');

  // Load users on component mount
  useEffect(() => {
    loadUsers();
  }, []);

  // Load psychological state when user is selected
  useEffect(() => {
    if (selectedUser) {
      loadUserPsychologicalState();
    }
  }, [selectedUser]);

  const loadUsers = async () => {
    try {
      setLoading(true);
      const response = await usersApi.getAllUsers();
      setUsers(response.data.content || response.data || []);
    } catch (err: any) {
      setError(`Failed to load users: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const loadUserPsychologicalState = async () => {
    if (!selectedUser) return;

    try {
      setLoading(true);
      setError(null);

      // Get current psychological state
      const stateResponse = await psychologicalApi.getPsychologicalState(selectedUser.id);
      setPsychologicalState(stateResponse.data);

      // Get Buddhist factors (if available in the response)
      if (stateResponse.data.buddhistFactors) {
        setBuddhistFactors(stateResponse.data.buddhistFactors);
      }

      // Load state history (this would need to be implemented in the backend)
      // For now, we'll simulate it
      setStateHistory([]);

    } catch (err: any) {
      setError(`Failed to load psychological state: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const formatTimestamp = (timestamp: number) => {
    return new Date(timestamp).toLocaleString();
  };

  const formatPercentage = (value: number) => {
    return `${Math.round(value * 100)}%`;
  };

  const getRiskLevel = (stress: number, intensity: number) => {
    if (stress > 0.8 || intensity > 0.8) return { level: 'High', color: '#ef4444' };
    if (stress > 0.6 || intensity > 0.6) return { level: 'Medium', color: '#f59e0b' };
    return { level: 'Low', color: '#10b981' };
  };

  const getEmotionalState = (stress: number, mindfulness: number, compassion: number) => {
    if (stress > 0.7) return { state: 'Anxious', color: '#f59e0b' };
    if (mindfulness > 0.6 && compassion > 0.5) return { state: 'Peaceful', color: '#10b981' };
    if (compassion > 0.7) return { state: 'Empathetic', color: '#8b5cf6' };
    if (mindfulness > 0.5) return { state: 'Mindful', color: '#3b82f6' };
    return { state: 'Neutral', color: '#6b7280' };
  };

  const getCommunicationStyle = (mindfulness: number, compassion: number, equanimity: number) => {
    if (mindfulness > 0.6 && compassion > 0.5) return 'Gentle';
    if (equanimity > 0.6) return 'Balanced';
    if (compassion > 0.7) return 'Warm';
    return 'Supportive';
  };

  const getPersonalityType = (stress: number, mindfulness: number, compassion: number) => {
    if (stress > 0.7) return 'Empathetic';
    if (mindfulness > 0.6 && compassion > 0.5) return 'Reflective';
    if (compassion > 0.7) return 'Empathetic';
    if (mindfulness > 0.5) return 'Mindful';
    return 'Supportive';
  };

  return (
    <div className="user-psychological-state-page">
      <div className="page-header">
        <h1>User Psychological States</h1>
        <p>Monitor and analyze individual user psychological states and trends</p>
      </div>

      <div className="content-layout">
        {/* User Selection Sidebar */}
        <div className="user-sidebar">
          <div className="sidebar-header">
            <h3>Users</h3>
            <button onClick={loadUsers} className="refresh-button" disabled={loading}>
              {loading ? 'Loading...' : 'Refresh'}
            </button>
          </div>
          
          <div className="user-list">
            {users.map((user) => (
              <div
                key={user.id}
                className={`user-item ${selectedUser?.id === user.id ? 'selected' : ''}`}
                onClick={() => setSelectedUser(user)}
              >
                <div className="user-info">
                  <div className="user-name">
                    {user.firstName && user.lastName 
                      ? `${user.firstName} ${user.lastName}` 
                      : user.username}
                  </div>
                  <div className="user-email">{user.email}</div>
                  <div className="user-joined">
                    Joined: {new Date(user.createdAt).toLocaleDateString()}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Main Content */}
        <div className="main-content">
          {selectedUser ? (
            <>
              {/* User Header */}
              <div className="user-header">
                <div className="user-details">
                  <h2>
                    {selectedUser.firstName && selectedUser.lastName 
                      ? `${selectedUser.firstName} ${selectedUser.lastName}` 
                      : selectedUser.username}
                  </h2>
                  <p>{selectedUser.email}</p>
                </div>
                <div className="user-actions">
                  <button
                    onClick={() => psychologicalApi.warmUpCache(selectedUser.id)}
                    className="action-button"
                  >
                    Warm Cache
                  </button>
                  <button
                    onClick={() => psychologicalApi.invalidateUserCache(selectedUser.id)}
                    className="action-button danger"
                  >
                    Clear Cache
                  </button>
                </div>
              </div>

              {/* Tab Navigation */}
              <div className="tab-navigation">
                <button
                  className={`tab-button ${activeTab === 'current' ? 'active' : ''}`}
                  onClick={() => setActiveTab('current')}
                >
                  Current State
                </button>
                <button
                  className={`tab-button ${activeTab === 'history' ? 'active' : ''}`}
                  onClick={() => setActiveTab('history')}
                >
                  History
                </button>
                <button
                  className={`tab-button ${activeTab === 'trends' ? 'active' : ''}`}
                  onClick={() => setActiveTab('trends')}
                >
                  Trends
                </button>
              </div>

              {/* Error Display */}
              {error && (
                <div className="error-message">
                  <span className="error-icon">⚠️</span>
                  {error}
                </div>
              )}

              {/* Current State Tab */}
              {activeTab === 'current' && psychologicalState && (
                <div className="current-state-tab">
                  {/* Overview Cards */}
                  <div className="overview-cards">
                    <div className="overview-card">
                      <div className="card-header">
                        <h4>Emotional State</h4>
                        <div 
                          className="state-indicator"
                          style={{ 
                            backgroundColor: getEmotionalState(
                              psychologicalState.stress, 
                              psychologicalState.mindfulness, 
                              psychologicalState.compassion
                            ).color 
                          }}
                        />
                      </div>
                      <div className="card-content">
                        <div className="state-name">
                          {getEmotionalState(
                            psychologicalState.stress, 
                            psychologicalState.mindfulness, 
                            psychologicalState.compassion
                          ).state}
                        </div>
                        <div className="state-details">
                          <div>Communication: {getCommunicationStyle(
                            psychologicalState.mindfulness, 
                            psychologicalState.compassion, 
                            psychologicalState.equanimity
                          )}</div>
                          <div>Personality: {getPersonalityType(
                            psychologicalState.stress, 
                            psychologicalState.mindfulness, 
                            psychologicalState.compassion
                          )}</div>
                        </div>
                      </div>
                    </div>

                    <div className="overview-card">
                      <div className="card-header">
                        <h4>Risk Assessment</h4>
                        <div 
                          className="risk-indicator"
                          style={{ 
                            backgroundColor: getRiskLevel(
                              psychologicalState.stress, 
                              psychologicalState.intensityFactor
                            ).color 
                          }}
                        />
                      </div>
                      <div className="card-content">
                        <div className="risk-level">
                          {getRiskLevel(
                            psychologicalState.stress, 
                            psychologicalState.intensityFactor
                          ).level} Risk
                        </div>
                        <div className="risk-factors">
                          <div>Stress: {formatPercentage(psychologicalState.stress)}</div>
                          <div>Intensity: {formatPercentage(psychologicalState.intensityFactor)}</div>
                        </div>
                      </div>
                    </div>

                    <div className="overview-card">
                      <div className="card-header">
                        <h4>Last Updated</h4>
                        <div className="time-icon">🕒</div>
                      </div>
                      <div className="card-content">
                        <div className="timestamp">
                          {formatTimestamp(psychologicalState.timestamp)}
                        </div>
                        <div className="time-ago">
                          {Math.round((Date.now() - psychologicalState.timestamp) / (1000 * 60))} minutes ago
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Detailed Metrics */}
                  <div className="metrics-section">
                    <h3>Detailed Psychological Metrics</h3>
                    <div className="metrics-grid">
                      <div className="metric-card">
                        <div className="metric-header">
                          <span className="metric-label">Stress Level</span>
                          <span 
                            className="metric-value"
                            style={{ 
                              color: psychologicalState.stress > 0.7 ? '#ef4444' : 
                                     psychologicalState.stress > 0.4 ? '#f59e0b' : '#10b981' 
                            }}
                          >
                            {formatPercentage(psychologicalState.stress)}
                          </span>
                        </div>
                        <div className="metric-bar">
                          <div 
                            className="metric-fill"
                            style={{ 
                              width: `${psychologicalState.stress * 100}%`,
                              backgroundColor: psychologicalState.stress > 0.7 ? '#ef4444' : 
                                             psychologicalState.stress > 0.4 ? '#f59e0b' : '#10b981'
                            }}
                          />
                        </div>
                      </div>

                      <div className="metric-card">
                        <div className="metric-header">
                          <span className="metric-label">Mindfulness</span>
                          <span 
                            className="metric-value"
                            style={{ 
                              color: psychologicalState.mindfulness > 0.6 ? '#10b981' : 
                                     psychologicalState.mindfulness > 0.3 ? '#f59e0b' : '#ef4444' 
                            }}
                          >
                            {formatPercentage(psychologicalState.mindfulness)}
                          </span>
                        </div>
                        <div className="metric-bar">
                          <div 
                            className="metric-fill"
                            style={{ 
                              width: `${psychologicalState.mindfulness * 100}%`,
                              backgroundColor: psychologicalState.mindfulness > 0.6 ? '#10b981' : 
                                             psychologicalState.mindfulness > 0.3 ? '#f59e0b' : '#ef4444'
                            }}
                          />
                        </div>
                      </div>

                      <div className="metric-card">
                        <div className="metric-header">
                          <span className="metric-label">Compassion</span>
                          <span 
                            className="metric-value"
                            style={{ 
                              color: psychologicalState.compassion > 0.6 ? '#10b981' : 
                                     psychologicalState.compassion > 0.3 ? '#f59e0b' : '#ef4444' 
                            }}
                          >
                            {formatPercentage(psychologicalState.compassion)}
                          </span>
                        </div>
                        <div className="metric-bar">
                          <div 
                            className="metric-fill"
                            style={{ 
                              width: `${psychologicalState.compassion * 100}%`,
                              backgroundColor: psychologicalState.compassion > 0.6 ? '#10b981' : 
                                             psychologicalState.compassion > 0.3 ? '#f59e0b' : '#ef4444'
                            }}
                          />
                        </div>
                      </div>

                      <div className="metric-card">
                        <div className="metric-header">
                          <span className="metric-label">Equanimity</span>
                          <span 
                            className="metric-value"
                            style={{ 
                              color: psychologicalState.equanimity > 0.6 ? '#10b981' : 
                                     psychologicalState.equanimity > 0.3 ? '#f59e0b' : '#ef4444' 
                            }}
                          >
                            {formatPercentage(psychologicalState.equanimity)}
                          </span>
                        </div>
                        <div className="metric-bar">
                          <div 
                            className="metric-fill"
                            style={{ 
                              width: `${psychologicalState.equanimity * 100}%`,
                              backgroundColor: psychologicalState.equanimity > 0.6 ? '#10b981' : 
                                             psychologicalState.equanimity > 0.3 ? '#f59e0b' : '#ef4444'
                            }}
                          />
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Buddhist Factors */}
                  {buddhistFactors && (
                    <div className="buddhist-factors-section">
                      <h3>Buddhist Mental Factors</h3>
                      <div className="factors-grid">
                        {Object.entries(buddhistFactors).map(([key, value]) => {
                          if (key === 'timestamp') return null;
                          return (
                            <div key={key} className="factor-item">
                              <span className="factor-label">
                                {key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())}
                              </span>
                              <span 
                                className="factor-value"
                                style={{ 
                                  color: value > 0.6 ? '#10b981' : 
                                         value > 0.3 ? '#f59e0b' : '#ef4444' 
                                }}
                              >
                                {formatPercentage(value)}
                              </span>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* History Tab */}
              {activeTab === 'history' && (
                <div className="history-tab">
                  <h3>Psychological State History</h3>
                  {stateHistory.length > 0 ? (
                    <div className="history-list">
                      {stateHistory.map((entry, index) => (
                        <div key={index} className="history-entry">
                          <div className="history-timestamp">
                            {formatTimestamp(new Date(entry.timestamp).getTime())}
                          </div>
                          <div className="history-metrics">
                            <div className="history-metric">
                              <span>Stress:</span>
                              <span>{formatPercentage(entry.buddhistFactors.mindfulness)}</span>
                            </div>
                            <div className="history-metric">
                              <span>Mindfulness:</span>
                              <span>{formatPercentage(entry.buddhistFactors.mindfulness)}</span>
                            </div>
                            <div className="history-metric">
                              <span>Compassion:</span>
                              <span>{formatPercentage(entry.buddhistFactors.compassion)}</span>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="empty-state">
                      <p>No psychological state history available for this user.</p>
                    </div>
                  )}
                </div>
              )}

              {/* Trends Tab */}
              {activeTab === 'trends' && (
                <div className="trends-tab">
                  <h3>Psychological Trends</h3>
                  <div className="empty-state">
                    <p>Trend analysis will be available once sufficient data is collected.</p>
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="empty-state">
              <h3>Select a User</h3>
              <p>Choose a user from the sidebar to view their psychological state.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default UserPsychologicalStatePage;
