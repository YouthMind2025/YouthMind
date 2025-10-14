import React, { useState, useEffect } from 'react';
import { psychologicalApi } from '../../services/api';
import './PsychologicalAnalysis.css';

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

interface PsychologicalInsights {
  primaryEmotion: string;
  secondaryEmotions: string[];
  emotionalIntensity: number;
  emotionalStability: number;
  overallAssessment: string;
  recommendations: string[];
  insights: string[];
  riskAssessment: {
    level: string;
    factors: string[];
    needsHumanReview: boolean;
    reviewReason: string;
  };
  timestamp: number;
}

interface BatchStatistics {
  queueSize: number;
  processedCount: number;
  errorCount: number;
  batchSize: number;
  maxQueueSize: number;
  batchDelayMs: number;
  timestamp: string;
}

interface CacheStatistics {
  psychologicalStateTTL: number;
  buddhistFactorsTTL: number;
  personalityProfileTTL: number;
  stateHistoryTTL: number;
  timestamp: string;
}

const PsychologicalAnalysisPage: React.FC = () => {
  const [selectedUserId, setSelectedUserId] = useState<string>('');
  const [testMessage, setTestMessage] = useState<string>('');
  const [psychologicalState, setPsychologicalState] = useState<PsychologicalState | null>(null);
  const [buddhistFactors, setBuddhistFactors] = useState<BuddhistFactors | null>(null);
  const [psychologicalInsights, setPsychologicalInsights] = useState<PsychologicalInsights | null>(null);
  const [batchStatistics, setBatchStatistics] = useState<BatchStatistics | null>(null);
  const [cacheStatistics, setCacheStatistics] = useState<CacheStatistics | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'analysis' | 'statistics' | 'settings'>('analysis');

  // Load statistics on component mount
  useEffect(() => {
    loadStatistics();
  }, []);

  const loadStatistics = async () => {
    try {
      setLoading(true);
      const [batchStats, cacheStats] = await Promise.all([
        psychologicalApi.getBatchStatistics(),
        psychologicalApi.getCacheStatistics()
      ]);
      setBatchStatistics(batchStats.data);
      setCacheStatistics(cacheStats.data);
    } catch (err: any) {
      setError(`Failed to load statistics: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const analyzeUserMessage = async () => {
    if (!selectedUserId || !testMessage.trim()) {
      setError('Please enter both User ID and test message');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      // Analyze message for psychological insights
      const analysisResponse = await psychologicalApi.analyzeMessage(selectedUserId, testMessage);
      setPsychologicalInsights(analysisResponse.data.psychologicalState);

      // Get updated psychological state
      const stateResponse = await psychologicalApi.getPsychologicalState(selectedUserId);
      setPsychologicalState(stateResponse.data);

      // Generate psychological response
      const responseData = await psychologicalApi.generatePsychologicalResponse(selectedUserId, testMessage);
      setBuddhistFactors(responseData.data.buddhistFactors);

    } catch (err: any) {
      setError(`Analysis failed: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const getRiskLevelColor = (level: string) => {
    switch (level.toLowerCase()) {
      case 'low': return '#10b981';
      case 'medium': return '#f59e0b';
      case 'high': return '#ef4444';
      case 'critical': return '#dc2626';
      default: return '#6b7280';
    }
  };

  const getEmotionColor = (emotion: string) => {
    switch (emotion.toLowerCase()) {
      case 'anxious': return '#f59e0b';
      case 'peaceful': return '#10b981';
      case 'empathetic': return '#8b5cf6';
      case 'mindful': return '#3b82f6';
      case 'neutral': return '#6b7280';
      case 'happy': return '#fbbf24';
      case 'sad': return '#3b82f6';
      case 'angry': return '#ef4444';
      case 'confused': return '#8b5cf6';
      case 'calm': return '#06b6d4';
      default: return '#6b7280';
    }
  };

  const formatTimestamp = (timestamp: number) => {
    return new Date(timestamp).toLocaleString();
  };

  const formatPercentage = (value: number) => {
    return `${Math.round(value * 100)}%`;
  };

  return (
    <div className="psychological-analysis-page">
      <div className="page-header">
        <h1>Psychological Analysis Dashboard</h1>
        <p>Monitor and analyze user psychological states using AI-powered insights</p>
      </div>

      {/* Tab Navigation */}
      <div className="tab-navigation">
        <button
          className={`tab-button ${activeTab === 'analysis' ? 'active' : ''}`}
          onClick={() => setActiveTab('analysis')}
        >
          Analysis
        </button>
        <button
          className={`tab-button ${activeTab === 'statistics' ? 'active' : ''}`}
          onClick={() => setActiveTab('statistics')}
        >
          Statistics
        </button>
        <button
          className={`tab-button ${activeTab === 'settings' ? 'active' : ''}`}
          onClick={() => setActiveTab('settings')}
        >
          Settings
        </button>
      </div>

      {/* Analysis Tab */}
      {activeTab === 'analysis' && (
        <div className="analysis-tab">
          {/* Input Section */}
          <div className="input-section">
            <div className="input-group">
              <label htmlFor="userId">User ID:</label>
              <input
                id="userId"
                type="text"
                value={selectedUserId}
                onChange={(e) => setSelectedUserId(e.target.value)}
                placeholder="Enter user ID to analyze"
                className="input-field"
              />
            </div>
            <div className="input-group">
              <label htmlFor="testMessage">Test Message:</label>
              <textarea
                id="testMessage"
                value={testMessage}
                onChange={(e) => setTestMessage(e.target.value)}
                placeholder="Enter a message to analyze psychologically"
                className="textarea-field"
                rows={3}
              />
            </div>
            <button
              onClick={analyzeUserMessage}
              disabled={loading || !selectedUserId || !testMessage.trim()}
              className="analyze-button"
            >
              {loading ? 'Analyzing...' : 'Analyze Message'}
            </button>
          </div>

          {/* Error Display */}
          {error && (
            <div className="error-message">
              <span className="error-icon">⚠️</span>
              {error}
            </div>
          )}

          {/* Results Section */}
          {(psychologicalState || psychologicalInsights || buddhistFactors) && (
            <div className="results-section">
              {/* Psychological State */}
              {psychologicalState && (
                <div className="result-card">
                  <h3>Current Psychological State</h3>
                  <div className="metrics-grid">
                    <div className="metric-item">
                      <span className="metric-label">Stress Level:</span>
                      <span className="metric-value" style={{ color: psychologicalState.stress > 0.7 ? '#ef4444' : psychologicalState.stress > 0.4 ? '#f59e0b' : '#10b981' }}>
                        {formatPercentage(psychologicalState.stress)}
                      </span>
                    </div>
                    <div className="metric-item">
                      <span className="metric-label">Mindfulness:</span>
                      <span className="metric-value" style={{ color: psychologicalState.mindfulness > 0.6 ? '#10b981' : psychologicalState.mindfulness > 0.3 ? '#f59e0b' : '#ef4444' }}>
                        {formatPercentage(psychologicalState.mindfulness)}
                      </span>
                    </div>
                    <div className="metric-item">
                      <span className="metric-label">Compassion:</span>
                      <span className="metric-value" style={{ color: psychologicalState.compassion > 0.6 ? '#10b981' : psychologicalState.compassion > 0.3 ? '#f59e0b' : '#ef4444' }}>
                        {formatPercentage(psychologicalState.compassion)}
                      </span>
                    </div>
                    <div className="metric-item">
                      <span className="metric-label">Equanimity:</span>
                      <span className="metric-value" style={{ color: psychologicalState.equanimity > 0.6 ? '#10b981' : psychologicalState.equanimity > 0.3 ? '#f59e0b' : '#ef4444' }}>
                        {formatPercentage(psychologicalState.equanimity)}
                      </span>
                    </div>
                    <div className="metric-item">
                      <span className="metric-label">Intensity Factor:</span>
                      <span className="metric-value" style={{ color: psychologicalState.intensityFactor > 0.7 ? '#ef4444' : psychologicalState.intensityFactor > 0.4 ? '#f59e0b' : '#10b981' }}>
                        {formatPercentage(psychologicalState.intensityFactor)}
                      </span>
                    </div>
                    <div className="metric-item">
                      <span className="metric-label">Last Updated:</span>
                      <span className="metric-value">{formatTimestamp(psychologicalState.timestamp)}</span>
                    </div>
                  </div>
                </div>
              )}

              {/* Buddhist Factors */}
              {buddhistFactors && (
                <div className="result-card">
                  <h3>Buddhist Mental Factors</h3>
                  <div className="factors-grid">
                    {Object.entries(buddhistFactors).map(([key, value]) => {
                      if (key === 'timestamp') return null;
                      return (
                        <div key={key} className="factor-item">
                          <span className="factor-label">{key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())}:</span>
                          <span className="factor-value" style={{ color: value > 0.6 ? '#10b981' : value > 0.3 ? '#f59e0b' : '#ef4444' }}>
                            {formatPercentage(value)}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Psychological Insights */}
              {psychologicalInsights && (
                <div className="result-card">
                  <h3>Psychological Insights</h3>
                  <div className="insights-content">
                    <div className="insight-section">
                      <h4>Overall Assessment</h4>
                      <p>{psychologicalInsights.overallAssessment}</p>
                    </div>

                    {psychologicalInsights.recommendations.length > 0 && (
                      <div className="insight-section">
                        <h4>Recommendations</h4>
                        <ul>
                          {psychologicalInsights.recommendations.map((rec, index) => (
                            <li key={index}>{rec}</li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {psychologicalInsights.riskAssessment.needsHumanReview && (
                      <div className="insight-section risk-section">
                        <h4 style={{ color: getRiskLevelColor(psychologicalInsights.riskAssessment.level) }}>
                          Risk Assessment - {psychologicalInsights.riskAssessment.level.toUpperCase()}
                        </h4>
                        <p>{psychologicalInsights.riskAssessment.reviewReason}</p>
                        {psychologicalInsights.riskAssessment.factors.length > 0 && (
                          <div>
                            <strong>Risk Factors:</strong>
                            <ul>
                              {psychologicalInsights.riskAssessment.factors.map((factor, index) => (
                                <li key={index}>{factor}</li>
                              ))}
                            </ul>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Statistics Tab */}
      {activeTab === 'statistics' && (
        <div className="statistics-tab">
          <div className="stats-grid">
            {/* Batch Processing Statistics */}
            {batchStatistics && (
              <div className="stat-card">
                <h3>Batch Processing Statistics</h3>
                <div className="stat-items">
                  <div className="stat-item">
                    <span className="stat-label">Queue Size:</span>
                    <span className="stat-value">{batchStatistics.queueSize}</span>
                  </div>
                  <div className="stat-item">
                    <span className="stat-label">Processed Count:</span>
                    <span className="stat-value">{batchStatistics.processedCount}</span>
                  </div>
                  <div className="stat-item">
                    <span className="stat-label">Error Count:</span>
                    <span className="stat-value" style={{ color: batchStatistics.errorCount > 0 ? '#ef4444' : '#10b981' }}>
                      {batchStatistics.errorCount}
                    </span>
                  </div>
                  <div className="stat-item">
                    <span className="stat-label">Batch Size:</span>
                    <span className="stat-value">{batchStatistics.batchSize}</span>
                  </div>
                  <div className="stat-item">
                    <span className="stat-label">Max Queue Size:</span>
                    <span className="stat-value">{batchStatistics.maxQueueSize}</span>
                  </div>
                  <div className="stat-item">
                    <span className="stat-label">Batch Delay:</span>
                    <span className="stat-value">{batchStatistics.batchDelayMs}ms</span>
                  </div>
                </div>
              </div>
            )}

            {/* Cache Statistics */}
            {cacheStatistics && (
              <div className="stat-card">
                <h3>Cache Statistics</h3>
                <div className="stat-items">
                  <div className="stat-item">
                    <span className="stat-label">Psychological State TTL:</span>
                    <span className="stat-value">{cacheStatistics.psychologicalStateTTL} min</span>
                  </div>
                  <div className="stat-item">
                    <span className="stat-label">Buddhist Factors TTL:</span>
                    <span className="stat-value">{cacheStatistics.buddhistFactorsTTL} min</span>
                  </div>
                  <div className="stat-item">
                    <span className="stat-label">Personality Profile TTL:</span>
                    <span className="stat-value">{cacheStatistics.personalityProfileTTL} min</span>
                  </div>
                  <div className="stat-item">
                    <span className="stat-label">State History TTL:</span>
                    <span className="stat-value">{cacheStatistics.stateHistoryTTL} min</span>
                  </div>
                </div>
              </div>
            )}
          </div>

          <button onClick={loadStatistics} className="refresh-button">
            Refresh Statistics
          </button>
        </div>
      )}

      {/* Settings Tab */}
      {activeTab === 'settings' && (
        <div className="settings-tab">
          <div className="settings-section">
            <h3>Cache Management</h3>
            <div className="settings-group">
              <label htmlFor="cacheUserId">User ID for Cache Operations:</label>
              <input
                id="cacheUserId"
                type="text"
                value={selectedUserId}
                onChange={(e) => setSelectedUserId(e.target.value)}
                placeholder="Enter user ID"
                className="input-field"
              />
              <div className="button-group">
                <button
                  onClick={async () => {
                    if (!selectedUserId) {
                      setError('Please enter a User ID');
                      return;
                    }
                    try {
                      await psychologicalApi.warmUpCache(selectedUserId);
                      setError(null);
                      alert('Cache warmed up successfully');
                    } catch (err: any) {
                      setError(`Cache warmup failed: ${err.message}`);
                    }
                  }}
                  className="action-button"
                >
                  Warm Up Cache
                </button>
                <button
                  onClick={async () => {
                    if (!selectedUserId) {
                      setError('Please enter a User ID');
                      return;
                    }
                    try {
                      await psychologicalApi.invalidateUserCache(selectedUserId);
                      setError(null);
                      alert('Cache invalidated successfully');
                    } catch (err: any) {
                      setError(`Cache invalidation failed: ${err.message}`);
                    }
                  }}
                  className="action-button danger"
                >
                  Invalidate Cache
                </button>
              </div>
            </div>
          </div>

          <div className="settings-section">
            <h3>System Health</h3>
            <button
              onClick={async () => {
                try {
                  const response = await psychologicalApi.getHealth();
                  alert(`System Health: ${JSON.stringify(response.data, null, 2)}`);
                } catch (err: any) {
                  setError(`Health check failed: ${err.message}`);
                }
              }}
              className="action-button"
            >
              Check System Health
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default PsychologicalAnalysisPage;
