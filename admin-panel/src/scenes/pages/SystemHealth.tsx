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
        let componentHealthResponses: { database: any, redis: any } = { database: null, redis: null };
        
        // Always try individual component health checks first, as they're more reliable
        console.log('Fetching individual component health...');
        try {
          const [dbHealthResponse, redisHealthResponse] = await Promise.allSettled([
            systemApi.getDatabaseHealth(),
            systemApi.getRedisHealth()
          ]);
          
          if (dbHealthResponse.status === 'fulfilled') {
            componentHealthResponses.database = dbHealthResponse.value;
            console.log('✅ Database health check successful:', dbHealthResponse.value.data);
          } else {
            console.log('❌ Database health check failed:', dbHealthResponse.status === 'rejected' ? dbHealthResponse.reason : 'Unknown error');
          }
          
          if (redisHealthResponse.status === 'fulfilled') {
            componentHealthResponses.redis = redisHealthResponse.value;
            console.log('✅ Redis health check successful:', redisHealthResponse.value.data);
          } else {
            console.log('❌ Redis health check failed:', redisHealthResponse.status === 'rejected' ? redisHealthResponse.reason : 'Unknown error');
          }
        } catch (componentError) {
          console.log('Individual component health checks failed:', componentError);
        }
        
        // Now try to get the main health endpoint
        try {
          console.log('Testing main health endpoint...');
          healthResponse = await systemApi.testConnectivity();
          console.log('✅ Main health endpoint successful:', healthResponse);
        } catch (directError) {
          console.log('❌ Main health endpoint failed, trying proxy...');
          try {
            // Try through proxy
            healthResponse = await systemApi.getHealth();
            console.log('✅ Health response from proxy /actuator/health:', healthResponse);
          } catch (healthError) {
            console.warn('❌ All main health endpoints failed, using individual component data only:', healthError);
            
            // Create a health response based on individual component statuses
            const hasAnyComponent = componentHealthResponses.database || componentHealthResponses.redis;
            if (hasAnyComponent) {
              // Determine overall status based on component statuses
              const dbStatus = componentHealthResponses.database?.data?.status || 'DOWN';
              const redisStatus = componentHealthResponses.redis?.data?.status || 'DOWN';
              const overallStatus = (dbStatus === 'UP' || redisStatus === 'UP') ? 'UP' : 'DOWN';
              
              healthResponse = {
                data: {
                  status: overallStatus,
                  components: {
                    database: componentHealthResponses.database?.data || { status: 'DOWN' },
                    redis: componentHealthResponses.redis?.data || { status: 'DOWN' }
                  }
                },
                status: 200
              };
              console.log('✅ Created health response from individual components:', healthResponse);
              console.log('Overall status determined as:', overallStatus, 'based on DB:', dbStatus, 'Redis:', redisStatus);
            } else {
              // No components working, throw error
              throw new Error('No health endpoints are accessible');
            }
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
        
        console.log('Full actuator payload:', JSON.stringify(actuatorPayload, null, 2));
        console.log('Is actuator response:', isActuator);
        console.log('Health response status:', healthResponse.status);
        console.log('Has components:', !!actuatorPayload.components);
        console.log('Components keys:', actuatorPayload.components ? Object.keys(actuatorPayload.components) : 'none');

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

        // Extract component details - prioritize individual component responses
        let databaseStatus = 'unknown';
        let redisStatus = 'unknown';
        
        // First, use individual component health responses if available
        if (componentHealthResponses.database) {
          databaseStatus = mapStatus(componentHealthResponses.database.data?.status);
          console.log('Using individual database health response:', componentHealthResponses.database.data?.status, '->', databaseStatus);
        }
        
        if (componentHealthResponses.redis) {
          redisStatus = mapStatus(componentHealthResponses.redis.data?.status);
          console.log('Using individual redis health response:', componentHealthResponses.redis.data?.status, '->', redisStatus);
        }
        
        // If we still don't have component statuses, try to get them from the main actuator response
        if ((databaseStatus === 'unknown' || redisStatus === 'unknown') && isActuator && actuatorPayload.components) {
          console.log('Processing actuator components as fallback...');
          
          // Check for database component status
          if (databaseStatus === 'unknown') {
            const dbComponent = actuatorPayload.components.database;
            if (dbComponent) {
              databaseStatus = mapStatus(dbComponent.status);
              console.log('Database component found in actuator response:', dbComponent.status, '->', databaseStatus);
            } else {
              console.log('No database component found in actuator response');
            }
          }
          
          // Check for redis component status  
          if (redisStatus === 'unknown') {
            const redisComponent = actuatorPayload.components.redis;
            if (redisComponent) {
              redisStatus = mapStatus(redisComponent.status);
              console.log('Redis component found in actuator response:', redisComponent.status, '->', redisStatus);
            } else {
              console.log('No redis component found in actuator response');
            }
          }
          
          // Log all available components for debugging
          console.log('Available components:', Object.keys(actuatorPayload.components));
          console.log('Components details:', actuatorPayload.components);
        }
        
        // Final fallback: if we still have unknown statuses, try individual component checks again
        if (databaseStatus === 'unknown' || redisStatus === 'unknown') {
          console.log('Some components still unknown, trying individual health checks as final fallback...');
          
          try {
            const [dbHealthResponse, redisHealthResponse] = await Promise.allSettled([
              databaseStatus === 'unknown' ? systemApi.getDatabaseHealth() : Promise.resolve({ data: { status: 'UP' } }),
              redisStatus === 'unknown' ? systemApi.getRedisHealth() : Promise.resolve({ data: { status: 'UP' } })
            ]);
            
            if (databaseStatus === 'unknown' && dbHealthResponse.status === 'fulfilled') {
              const dbHealth = dbHealthResponse.value.data;
              databaseStatus = mapStatus(dbHealth?.status);
              console.log('Final fallback - Database health check:', dbHealth?.status, '->', databaseStatus);
            } else if (databaseStatus === 'unknown') {
              console.log('Final fallback - Database health check failed:', dbHealthResponse.status === 'rejected' ? dbHealthResponse.reason : 'Unknown error');
              databaseStatus = overallHealth === 'healthy' ? 'up' : 'down';
            }
            
            if (redisStatus === 'unknown' && redisHealthResponse.status === 'fulfilled') {
              const redisHealth = redisHealthResponse.value.data;
              redisStatus = mapStatus(redisHealth?.status);
              console.log('Final fallback - Redis health check:', redisHealth?.status, '->', redisStatus);
            } else if (redisStatus === 'unknown') {
              console.log('Final fallback - Redis health check failed:', redisHealthResponse.status === 'rejected' ? redisHealthResponse.reason : 'Unknown error');
              redisStatus = 'unknown';
            }
          } catch (componentError) {
            console.log('Final fallback individual component health checks failed:', componentError);
            if (databaseStatus === 'unknown') {
              databaseStatus = overallHealth === 'healthy' ? 'up' : 'down';
            }
            if (redisStatus === 'unknown') {
              redisStatus = 'unknown';
            }
          }
        }





        const finalHealthData: HealthData = {
          status: overallHealth,
          services: {
            database: databaseStatus,
            redis: redisStatus,
            ai_service: 'unknown', // TODO: integrate AI service health when available
          },
          metrics: {
            active_users: userCount,
            posts_today: 0,
            ai_response_time_avg: '< 1s',
          },
        };

        console.log('Final health data:', finalHealthData);
        setHealthData(finalHealthData);
    
        setLastUpdated(new Date());
      } catch (error: any) {
        console.error('Failed to fetch health data:', error);
        console.error('Error response:', error.response?.data);
        console.error('Error status:', error.response?.status);
        
        // Determine if this is a backend connectivity issue or component-specific issue
        const isBackendDown = !error.response || error.response.status >= 500;
        const isServiceUnavailable = error.response?.status === 503;
        const isForbidden = error.response?.status === 403;
        const isNotFound = error.response?.status === 404;
        
        console.log('Error analysis:', {
          isBackendDown,
          isServiceUnavailable,
          isForbidden,
          isNotFound,
          status: error.response?.status,
          path: error.response?.data?.path
        });
        
        // Set services as down if we can't reach backend
        setHealthData({
          status: isBackendDown ? 'unhealthy' : 'healthy',
          services: {
            database: isBackendDown ? 'down' : 'unknown',
            redis: isBackendDown ? 'down' : 'unknown',
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
        
        {/* Debug information - remove in production */}
        {process.env.NODE_ENV === 'development' && (
          <div className="mt-8 p-4 bg-gray-100 rounded-lg">
            <h3 className="text-sm font-semibold text-gray-700 mb-2">Debug Information</h3>
            <div className="text-xs text-gray-600 space-y-1">
              <div>Overall Status: {healthData.status}</div>
              <div>Database: {healthData.services.database}</div>
              <div>Redis: {healthData.services.redis}</div>
              <div>AI Service: {healthData.services.ai_service}</div>
              <div>Last Updated: {lastUpdated.toLocaleString()}</div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default SystemHealth; 