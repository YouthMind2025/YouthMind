import React, { useState, useEffect } from 'react';
import './SystemHealth.css';
import { systemApi, usersApi } from '../../services/api';


interface HealthData {
  status: string;
  services: {
    database: string;
    redis: string;
    ai_service: string;
  };
  metrics: {
    active_users: number;
    posts_today: number;
    ai_response_time_avg: string;
  };
}

const SystemHealth: React.FC = () => {
  const [healthData, setHealthData] = useState<HealthData>({
    status: 'unknown',
    services: {
      database: 'unknown',
      redis: 'unknown',
      ai_service: 'unknown',
    },
    metrics: {
      active_users: 0,
      posts_today: 0,
      ai_response_time_avg: 'N/A',
    },
  });
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

  useEffect(() => {
    const fetchHealthData = async () => {
      setLoading(true);
      try {
        console.log('Fetching system health data...');
        
        // Test backend connectivity using actuator health endpoint
        console.log('Calling health endpoint...');
        let healthResponse;
        try {
          // First try direct backend connection to test if backend is running
          console.log('Testing direct backend connection...');
          healthResponse = await systemApi.testBackendDirect();
          console.log('✅ Backend is running! Direct connection successful:', healthResponse);
        } catch (directError) {
          console.error('❌ Backend direct connection failed:', directError);
          console.log('This means either:');
          console.log('1. Backend is not running on port 8080');
          console.log('2. CORS is blocking the request');
          console.log('3. Backend health endpoint is not available');
          
          try {
            // Try through proxy
            healthResponse = await systemApi.getHealth();
            console.log('Health response from proxy /actuator/health:', healthResponse);
          } catch (healthError) {
            console.warn('Actuator health endpoint failed, trying fallback:', healthError);
            // Try fallback endpoint
            healthResponse = await systemApi.getHealthFallback();
            console.log('Health response from fallback:', healthResponse);
          }
        }
        
        // Get user count as a metric
        let userCount = 0;
        try {
          console.log('Fetching user count...');
          const usersResponse = await usersApi.getAllUsers();
          console.log('Users response:', usersResponse);
          
          // Backend /api/users returns direct array of UserDto objects (not wrapped in ApiResponse)
          if (Array.isArray(usersResponse.data)) {
            userCount = usersResponse.data.length;
            console.log('User count from direct array:', userCount);
          } else if (usersResponse.data && usersResponse.data.success && Array.isArray(usersResponse.data.data)) {
            userCount = usersResponse.data.data.length;
            console.log('User count from API response:', userCount);
          } else {
            console.log('Unexpected users response format:', usersResponse.data);
          }
        } catch (userError) {
          console.error('Could not fetch user count:', userError);
        }

        

        const actuatorPayload = (healthResponse.data ?? {}) as any;
        const isActuator = actuatorPayload && typeof actuatorPayload.status === 'string';
        
        console.log('Actuator payload:', actuatorPayload);
        console.log('Is actuator response:', isActuator);
        console.log('Health response status:', healthResponse.status);

        const mapStatus = (value?: string): string => {
          if (!value) return 'unknown';
          const normalized = value.toUpperCase();
          if (normalized === 'UP') return 'up';
          if (normalized === 'DOWN') return 'down';
          return 'unknown';
        };

        // Derive overall server health
        const overallHealth = isActuator
          ? actuatorPayload.status === 'UP'
            ? 'healthy'
            : 'unhealthy'
          : healthResponse.status === 200
          ? 'healthy'
          : 'unhealthy';
          
        console.log('Calculated overall health:', overallHealth);

        // Pull component details if available (Actuator) otherwise fall back to overall
        const dbComponent = actuatorPayload.components?.database;
        const redisComponent = actuatorPayload.components?.redis;

        const updatedHealthData: HealthData = {
          status: overallHealth,
          services: {
            database: isActuator ? mapStatus(dbComponent?.status) : mapStatus(overallHealth === 'healthy' ? 'UP' : 'DOWN'),
            redis: isActuator ? mapStatus(redisComponent?.status) : 'unknown',
            ai_service: 'unknown', // TODO: integrate AI service health when available
          },
          metrics: {
            active_users: userCount,
            posts_today: 0,
            ai_response_time_avg: '< 1s',
          },
        };

        console.log('Final health data:', updatedHealthData);
        setHealthData(updatedHealthData);
    
        setLastUpdated(new Date());
      } catch (error: any) {
        console.error('Failed to fetch health data:', error);
        console.error('Error response:', error.response?.data);
        console.error('Error status:', error.response?.status);
        
        // Set services as down if we can't reach backend
        setHealthData({
          status: 'DOWN',
          services: {
            database: 'down',
            redis: 'unknown',
            ai_service: 'unknown'
          },
          metrics: {
            active_users: 0,
            posts_today: 0,
            ai_response_time_avg: 'N/A'
          }
        });
      } finally {
        setLoading(false);
      }
    };

    fetchHealthData();

    // Refresh health data every 30 seconds
    const interval = setInterval(fetchHealthData, 30000);
    return () => clearInterval(interval);
  }, []);

  const getStatusColor = (status: string) => {
    if (status === 'up') return 'status-up';
    if (status === 'down') return 'status-down';
    return 'status-unknown';
  };

  const refreshHealth = () => {
    window.location.reload();
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <h1 className="page-title">System Health</h1>
        <div className="flex items-center space-x-4">
          <span className="text-sm text-gray-500">
            Last updated: {lastUpdated.toLocaleTimeString()}
          </span>
          <button
            onClick={refreshHealth}
            disabled={loading}
            className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded text-sm disabled:opacity-50"
          >
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>
      </div>
      <div className="page-content">
        <div className="server-status">
          <div className={`server-status-box ${healthData.status === 'healthy' ? 'status-up' : 'status-down'}`}>
            <h2>Server Status</h2>
            <div className="status-indicator">
              {healthData.status.toUpperCase()}
            </div>
          </div>

          <div className="services-grid">
            <div className={`status-box ${getStatusColor(healthData.services.database)}`}>
              <h2>Database</h2>
              <div className="status-indicator">
                {healthData.services.database.toUpperCase()}
              </div>
            </div>

            <div className={`status-box ${getStatusColor(healthData.services.redis)}`}>
              <h2>Redis</h2>
              <div className="status-indicator">
                {healthData.services.redis.toUpperCase()}
              </div>
            </div>

            <div className={`status-box ${getStatusColor(healthData.services.ai_service)}`}>
              <h2>AI Service</h2>
              <div className="status-indicator">
                {healthData.services.ai_service.toUpperCase()}
              </div>
            </div>
          </div>
        </div>
        <div className="metrics-section">
          <div className="metrics-grid">
            <div className="metric-box">
              <h2>Total Users</h2>
              <div className="metric-value">{healthData.metrics.active_users}</div>
              <div className="text-xs text-gray-500 mt-1">Registered users</div>
            </div>
            <div className="metric-box">
              <h2>Posts Today</h2>
              <div className="metric-value">{healthData.metrics.posts_today}</div>
              <div className="text-xs text-gray-500 mt-1">Not available</div>
            </div>
            <div className="metric-box">
              <h2>AI Response Time</h2>
              <div className="metric-value">{healthData.metrics.ai_response_time_avg}</div>
              <div className="text-xs text-gray-500 mt-1">Estimated</div>
            </div>
          </div>
        </div>
        
        {loading && (
          <div className="flex justify-center items-center mt-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            <span className="ml-2 text-gray-600">Checking system health...</span>
          </div>
        )}
      </div>
    </div>
  );
};

export default SystemHealth; 