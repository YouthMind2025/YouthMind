import React, { useState, useEffect } from 'react';
import { recommendationApi } from '../../services/api';
import { RecommendationPerformance, EmotionDistribution } from '../../types';
import './RecommendationAnalytics.css';

const RecommendationAnalytics: React.FC = () => {
  const [performance, setPerformance] = useState<RecommendationPerformance | null>(null);
  const [emotionDist, setEmotionDist] = useState<EmotionDistribution | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

  // Fetch data on mount and every 30 seconds
  useEffect(() => {
    fetchAnalytics();
    const interval = setInterval(fetchAnalytics, 30000);
    return () => clearInterval(interval);
  }, []);

  const fetchAnalytics = async () => {
    try {
      setError('');
      
      // Call both endpoints in parallel
      const [perfResponse, emotionResponse] = await Promise.all([
        recommendationApi.getPerformanceMetrics(),
        recommendationApi.getEmotionDistribution()
      ]);
      
      // Extract data from ApiResponse wrapper
      if (perfResponse.data.success && emotionResponse.data.success) {
        setPerformance(perfResponse.data.data);
        setEmotionDist(emotionResponse.data.data);
        setLastUpdated(new Date());
      } else {
        setError('Failed to fetch analytics data');
      }
    } catch (err) {
      console.error('Error fetching analytics:', err);
      setError('Failed to fetch analytics data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const exportToCSV = () => {
    if (!performance || !emotionDist) return;

    const totalUsers = Object.values(emotionDist.userSegments)
      .reduce((sum, count) => sum + count, 0);

    const csvData = [
      ['Recommendation Analytics Report'],
      ['Generated: ' + new Date().toISOString()],
      [''],
      ['=== Overview ==='],
      ['Total Users with R Vectors', totalUsers.toString()],
      ['Cache Hit Rate', performance.cacheHitRate.toFixed(1) + '%'],
      ['Avg Block Generation Time', performance.avgBlockGenerationTimeMs.toFixed(0) + 'ms'],
      ['Event Processing Rate', performance.eventProcessingRate.toFixed(1) + ' events/min'],
      ['R Vector Update Success Rate', performance.rVectorUpdateSuccessRate.toFixed(1) + '%'],
      [''],
      ['=== Average R Vector Distribution ==='],
      ['Emotion', 'Percentage'],
      ...Object.entries(emotionDist.overallEmotionDistribution).map(([emotion, value]) => [
        emotion.charAt(0).toUpperCase() + emotion.slice(1),
        (value * 100).toFixed(1) + '%'
      ]),
      [''],
      ['=== Top-3 Emotion Combinations ==='],
      ['Combination', 'User Count', 'Percentage'],
      ...emotionDist.topEmotionCombinations.map(combo => [
        combo.combination,
        combo.userCount.toString(),
        combo.percentage.toFixed(1) + '%'
      ]),
      [''],
      ['=== Performance Metrics ==='],
      ['Metric', 'Value'],
      ['Block Generation Time', performance.avgBlockGenerationTimeMs.toFixed(0) + 'ms'],
      ['Cache Hit Rate', performance.cacheHitRate.toFixed(1) + '%'],
      ['Event Processing Rate', performance.eventProcessingRate.toFixed(1) + ' events/min'],
      ['R Vector Update Success Rate', performance.rVectorUpdateSuccessRate.toFixed(1) + '%'],
      [''],
      ['=== Stream Metrics ==='],
      ['Metric', 'Value'],
      ['R Vector Updates Stream Length', performance.streamMetrics.rvectorUpdatesStreamLength.toString()],
      ['Block Precalc Stream Length', performance.streamMetrics.blockPrecalcStreamLength.toString()],
      ['Consumer Lag (ms)', performance.streamMetrics.consumerLagMs.toString()],
      ['DLQ Size', performance.streamMetrics.dlqSize.toString()],
      [''],
      ['=== Consumer Health ==='],
      ['Consumer', 'Status'],
      ...Object.entries(performance.consumerHealth).map(([consumer, healthy]) => [
        consumer,
        healthy ? 'Healthy' : 'Unhealthy'
      ])
    ];

    const csv = csvData.map(row => row.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `recommendation-analytics-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleString();
  };

  const getHealthStatus = (healthy: boolean) => {
    return healthy ? 'Healthy' : 'Unhealthy';
  };

  const getHealthClass = (healthy: boolean) => {
    return healthy ? 'healthy' : 'unhealthy';
  };

  if (loading) {
    return (
      <div className="page-container">
        <div className="page-header">
          <div>
            <h1 className="page-title">Recommendation Analytics</h1>
            <p className="page-subtitle">Real-time analytics for the recommendation engine</p>
          </div>
        </div>
        <div className="loading">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p>Loading analytics data...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page-container">
        <div className="page-header">
          <div>
            <h1 className="page-title">Recommendation Analytics</h1>
            <p className="page-subtitle">Real-time analytics for the recommendation engine</p>
          </div>
        </div>
        <div className="error">
          <span className="error-icon">⚠️</span>
          {error}
        </div>
        <div className="text-center">
          <button onClick={fetchAnalytics} className="refresh-button">
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (!performance || !emotionDist) {
    return (
      <div className="page-container">
        <div className="page-header">
          <div>
            <h1 className="page-title">Recommendation Analytics</h1>
            <p className="page-subtitle">Real-time analytics for the recommendation engine</p>
          </div>
        </div>
        <div className="error">
          <span className="error-icon">⚠️</span>
          No data available
        </div>
      </div>
    );
  }

  const totalUsers = Object.values(emotionDist.userSegments)
    .reduce((sum, count) => sum + count, 0);

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Recommendation Analytics</h1>
          <p className="page-subtitle">Real-time analytics for the recommendation engine</p>
        </div>
        <div className="header-actions">
          <div className="last-updated">
            Last updated: {lastUpdated.toLocaleTimeString()}
          </div>
          <button 
            onClick={fetchAnalytics} 
            className="refresh-button"
            disabled={loading}
          >
            🔄 Refresh
          </button>
          <button onClick={exportToCSV} className="export-button">
            📊 Export CSV
          </button>
        </div>
      </div>

      <div className="page-content">
        {/* Overview Cards */}
        <div className="metrics-grid">
          <div className="stat-card">
            <h3>Total Users with R Vectors</h3>
            <p className="stat-value">{totalUsers.toLocaleString()}</p>
            <p className="stat-description">Users with active recommendation vectors</p>
          </div>
          
          <div className="stat-card">
            <h3>Cache Hit Rate</h3>
            <p className="stat-value">{performance.cacheHitRate.toFixed(1)}%</p>
            <p className="stat-description">Redis cache performance</p>
          </div>
          
          <div className="stat-card">
            <h3>Block Generation Time</h3>
            <p className="stat-value">{performance.avgBlockGenerationTimeMs.toFixed(0)}ms</p>
            <p className="stat-description">Average generation time</p>
          </div>
          
          <div className="stat-card">
            <h3>Event Processing Rate</h3>
            <p className="stat-value">{performance.eventProcessingRate.toFixed(0)}/min</p>
            <p className="stat-description">Events processed per minute</p>
          </div>
        </div>

        {/* Average R Vector Distribution */}
        <div className="section">
          <h2>Average R Vector Distribution</h2>
          <div className="emotion-distribution">
            {Object.entries(emotionDist.overallEmotionDistribution).map(([emotion, value]) => (
              <div key={emotion} className="emotion-item">
                <div className="emotion-name">
                  {emotion.charAt(0).toUpperCase() + emotion.slice(1)}
                </div>
                <div className="emotion-percentage">
                  {(value * 100).toFixed(1)}%
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Top-3 Emotion Combinations */}
        <div className="section">
          <h2>Most Common Top-3 Emotion Combinations</h2>
          <table className="combinations-table">
            <thead>
              <tr>
                <th>Combination</th>
                <th>User Count</th>
                <th>Percentage</th>
              </tr>
            </thead>
            <tbody>
              {emotionDist.topEmotionCombinations.map((combo, index) => (
                <tr key={index}>
                  <td className="combination-name">{combo.combination}</td>
                  <td className="user-count">{combo.userCount.toLocaleString()}</td>
                  <td className="percentage">{combo.percentage.toFixed(1)}%</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Performance Metrics */}
        <div className="section">
          <h2>Performance Metrics</h2>
          <div className="performance-metrics">
            <div className="performance-item">
              <div className="performance-label">Block Generation Time</div>
              <div className="performance-value">{performance.avgBlockGenerationTimeMs.toFixed(0)}ms</div>
            </div>
            <div className="performance-item">
              <div className="performance-label">Cache Hit Rate</div>
              <div className="performance-value">{performance.cacheHitRate.toFixed(1)}%</div>
            </div>
            <div className="performance-item">
              <div className="performance-label">Event Processing Rate</div>
              <div className="performance-value">{performance.eventProcessingRate.toFixed(1)} events/min</div>
            </div>
            <div className="performance-item">
              <div className="performance-label">R Vector Update Success Rate</div>
              <div className="performance-value">{performance.rVectorUpdateSuccessRate.toFixed(1)}%</div>
            </div>
          </div>
        </div>

        {/* Stream Metrics */}
        <div className="section">
          <h2>Stream Metrics</h2>
          <div className="stream-metrics">
            <div className="stream-item">
              <div className="stream-label">R Vector Updates Stream</div>
              <div className="stream-value">{performance.streamMetrics.rvectorUpdatesStreamLength.toLocaleString()}</div>
            </div>
            <div className="stream-item">
              <div className="stream-label">Block Precalc Stream</div>
              <div className="stream-value">{performance.streamMetrics.blockPrecalcStreamLength.toLocaleString()}</div>
            </div>
            <div className="stream-item">
              <div className="stream-label">Consumer Lag</div>
              <div className="stream-value">{performance.streamMetrics.consumerLagMs.toLocaleString()}ms</div>
            </div>
            <div className="stream-item">
              <div className="stream-label">Dead Letter Queue</div>
              <div className="stream-value">{performance.streamMetrics.dlqSize.toLocaleString()}</div>
            </div>
          </div>
        </div>

        {/* Consumer Health */}
        <div className="section">
          <h2>Consumer Health Status</h2>
          <div className="performance-metrics">
            {Object.entries(performance.consumerHealth).map(([consumer, healthy]) => (
              <div key={consumer} className="performance-item">
                <div className="performance-label">{consumer}</div>
                <div className={`health-indicator ${getHealthClass(healthy)}`}>
                  {healthy ? '✅' : '❌'} {getHealthStatus(healthy)}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Data Timestamps */}
        <div className="section">
          <h2>Data Information</h2>
          <div className="performance-metrics">
            <div className="performance-item">
              <div className="performance-label">Performance Data Generated</div>
              <div className="performance-value">{formatTimestamp(performance.generatedAt)}</div>
            </div>
            <div className="performance-item">
              <div className="performance-label">Emotion Distribution Generated</div>
              <div className="performance-value">{formatTimestamp(emotionDist.generatedAt)}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RecommendationAnalytics;
