package ai.youthmind.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WelcomeController {

    @GetMapping("/")
    @ResponseBody
    public String welcome() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>YouthMind Backend API</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; background-color: #f5f5f5; }
                    .container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    h1 { color: #2c3e50; }
                    .endpoint { background: #ecf0f1; padding: 15px; margin: 10px 0; border-radius: 5px; }
                    .endpoint a { color: #3498db; text-decoration: none; font-weight: bold; }
                    .endpoint a:hover { text-decoration: underline; }
                    .status { color: #27ae60; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🚀 YouthMind Backend API</h1>
                    <p class="status">✅ Server is running successfully!</p>
                    
                    <h2>Available Endpoints:</h2>
                    
                    <div class="endpoint">
                        <strong>API Documentation:</strong><br>
                        <a href="/swagger-ui.html" target="_blank">📚 Swagger UI - Interactive API Documentation</a>
                    </div>
                    
                    <div class="endpoint">
                        <strong>Test Endpoints:</strong><br>
                        <a href="/api/test/hello" target="_blank">👋 Hello Endpoint</a><br>
                        <a href="/api/test/health" target="_blank">🏥 Health Check</a>
                    </div>
                    
                    <div class="endpoint">
                        <strong>Health Monitoring (DevOps):</strong><br>
                        <a href="/actuator/health" target="_blank">🏥 System Health</a><br>
                        <a href="/actuator/health/redis" target="_blank">📡 Redis Health</a><br>
                        <a href="/actuator/health/database" target="_blank">🗄️ Database Health</a><br>
                        <a href="/actuator/info" target="_blank">ℹ️ Application Info</a><br>
                        <a href="/actuator/metrics" target="_blank">📊 Metrics</a><br>
                        <a href="/actuator/prometheus" target="_blank">📈 Prometheus Metrics</a>
                    </div>
                    
                    <div class="endpoint">
                        <strong>Development Tools:</strong><br>
                        <a href="/h2-console" target="_blank">🗄️ H2 Database Console</a> (Dev Mode)
                    </div>
                    
                    <div class="endpoint">
                        <strong>API Documentation JSON:</strong><br>
                        <a href="/api-docs" target="_blank">📄 OpenAPI JSON</a>
                    </div>
                    
                    <h3>ℹ️ Information:</h3>
                    <ul>
                        <li><strong>Framework:</strong> Spring Boot 3.2.5</li>
                        <li><strong>Security:</strong> JWT Authentication (configured)</li>
                        <li><strong>Database:</strong> H2 (Development) / PostgreSQL (Production)</li>
                        <li><strong>Documentation:</strong> OpenAPI 3 / Swagger</li>
                    </ul>
                </div>
            </body>
            </html>
            """;
    }
} 