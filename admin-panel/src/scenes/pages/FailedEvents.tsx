import React, { useState, useEffect } from 'react';
import { queueApi } from '../../services/api';
import { QueueStatistics } from '../../types';
import './FailedEvents.css';

const FailedEvents: React.FC = () => {
  const [statistics, setStatistics] = useState<QueueStatistics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

  // Fetch data on mount and every 30 seconds
  useEffect(() => {
    fetchStatistics();
    const interval = setInterval(fetchStatistics, 30000);
    return () => clearInterval(interval);
  }, []);

  const fetchStatistics = async () => {
    try {
      setError('');
      const response = await queueApi.getStatistics();
      
      if (response.data) {
        setStatistics(response.data);
        setLastUpdated(new Date());
      } else {
        setError('Failed to fetch queue statistics');
      }
    } catch (err) {
      console.error('Error fetching queue statistics:', err);
      setError('Failed to fetch queue statistics. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (value: number, thresholds: { green: number; yellow: number }) => {
    if (value < thresholds.green) return 'green';
    if (value < thresholds.yellow) return 'yellow';
    return 'red';
  };

  const formatNumber = (num: number) => {
    return num.toLocaleString();
  };

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleString();
  };

  const getQueueDisplayName = (queueName: string) => {
    const nameMap: Record<string, string> = {
      'dead_letter': 'Dead Letter Queue',
      'retry': 'Retry Queue',
      'high_priority': 'High Priority',
      'normal_priority': 'Normal Priority',
      'low_priority': 'Low Priority',
      'batch': 'Batch Queue',
      'ai': 'AI Queue',
      'vector_search': 'Vector Search',
      'state_update_high': 'State Update (High)',
      'state_update_normal': 'State Update (Normal)',
      'state_update_low': 'State Update (Low)',
      'state_update_batch': 'State Update (Batch)'
    };
    return nameMap[queueName] || queueName.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
  };

  if (loading) {
    return (
      <div className="failed-events-container">
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>Loading queue statistics...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="failed-events-container">
        <div className="error-message">
          <div className="error-icon">⚠️</div>
          <h3>Error</h3>
          <p>{error}</p>
          <button onClick={fetchStatistics} className="retry-button">
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (!statistics) {
    return (
      <div className="failed-events-container">
        <div className="error-message">
          <div className="error-icon">📊</div>
          <h3>No Data</h3>
          <p>No queue statistics available.</p>
        </div>
      </div>
    );
  }

  const dlqSize = statistics.queueSizes.dead_letter || 0;
  const retrySize = statistics.queueSizes.retry || 0;
  const totalFailed = statistics.totalFailedRequests || 0;

  return (
    <div className="failed-events-container">
      <div className="page-header">
        <h1>Failed Events Dashboard</h1>
        <p>Monitor dead letter queue and failed event statistics</p>
        <div className="last-updated">
          Last updated: {lastUpdated.toLocaleString()}
        </div>
      </div>

      <div className="page-content">
        {/* Main Metrics Cards */}
        <div className="metrics-grid">
          <div className={`metric-card ${getStatusColor(dlqSize, { green: 10, yellow: 50 })}`}>
            <div className="metric-header">
              <h3>Dead Letter Queue</h3>
              <div className="status-indicator">
                {dlqSize === 0 ? '✅' : dlqSize < 10 ? '🟡' : '🔴'}
              </div>
            </div>
            <div className="metric-value">{formatNumber(dlqSize)}</div>
            <div className="metric-description">Failed events requiring attention</div>
          </div>

          <div className={`metric-card ${getStatusColor(totalFailed, { green: 100, yellow: 500 })}`}>
            <div className="metric-header">
              <h3>Total Failed Requests</h3>
              <div className="status-indicator">
                {totalFailed === 0 ? '✅' : totalFailed < 100 ? '🟡' : '🔴'}
              </div>
            </div>
            <div className="metric-value">{formatNumber(totalFailed)}</div>
            <div className="metric-description">Cumulative failed requests</div>
          </div>

          <div className={`metric-card ${getStatusColor(retrySize, { green: 5, yellow: 25 })}`}>
            <div className="metric-header">
              <h3>Retry Queue</h3>
              <div className="status-indicator">
                {retrySize === 0 ? '✅' : retrySize < 5 ? '🟡' : '🔴'}
              </div>
            </div>
            <div className="metric-value">{formatNumber(retrySize)}</div>
            <div className="metric-description">Events pending retry</div>
          </div>
        </div>

        {/* Queue Details Section */}
        <div className="queue-details-section">
          <h2>Queue Details</h2>
          <div className="queue-details-grid">
            {Object.entries(statistics.queueSizes).map(([queueName, size]) => (
              <div key={queueName} className="queue-detail-item">
                <div className="queue-name">{getQueueDisplayName(queueName)}</div>
                <div className="queue-size">{formatNumber(size)}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Processing Statistics */}
        <div className="processing-stats-section">
          <h2>Processing Statistics</h2>
          <div className="stats-grid">
            <div className="stat-item">
              <div className="stat-label">Total Queued</div>
              <div className="stat-value">{formatNumber(statistics.totalQueuedRequests)}</div>
            </div>
            <div className="stat-item">
              <div className="stat-label">Total Processed</div>
              <div className="stat-value">{formatNumber(statistics.totalProcessedRequests)}</div>
            </div>
            <div className="stat-item">
              <div className="stat-label">Success Rate</div>
              <div className="stat-value">
                {statistics.totalQueuedRequests > 0 
                  ? `${((statistics.totalProcessedRequests / statistics.totalQueuedRequests) * 100).toFixed(1)}%`
                  : 'N/A'
                }
              </div>
            </div>
            <div className="stat-item">
              <div className="stat-label">Avg Processing Time</div>
              <div className="stat-value">{statistics.averageProcessingTime.toFixed(2)}ms</div>
            </div>
          </div>
        </div>

        {/* Refresh Button */}
        <div className="refresh-section">
          <button onClick={fetchStatistics} className="refresh-button">
            <span className="refresh-icon">🔄</span>
            Refresh Data
          </button>
        </div>
      </div>
    </div>
  );
};

export default FailedEvents;
