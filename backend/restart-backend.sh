#!/bin/bash

echo "🔄 Restarting Backend Service..."
echo "================================"

# Stop existing backend process
echo "1️⃣ Stopping existing backend process on port 8080..."
lsof -ti:8080 | xargs kill -9 2>/dev/null && echo "   ✅ Process killed" || echo "   ℹ️  No process running on port 8080"

# Wait a moment
sleep 2

# Check Java version
echo ""
echo "2️⃣ Checking Java version..."
java --version

# Compile the project
echo ""
echo "3️⃣ Compiling project..."
echo "   This may take a few minutes..."

# Try to compile without tests
mvn clean install -DskipTests -e 2>&1 | tail -20

if [ $? -eq 0 ]; then
    echo "   ✅ Compilation successful!"
    
    # Start the backend
    echo ""
    echo "4️⃣ Starting backend service..."
    echo "   Backend will run on http://localhost:8080"
    echo "   Press Ctrl+C to stop"
    echo ""
    
    # Run the application
    mvn spring-boot:run
else
    echo ""
    echo "❌ Compilation failed!"
    echo ""
    echo "📝 Alternative options:"
    echo ""
    echo "Option 1: Use IntelliJ IDEA or Eclipse"
    echo "   1. Open the project in your IDE"
    echo "   2. Find YouthmindBackendApplication.java"
    echo "   3. Right-click and select 'Run'"
    echo ""
    echo "Option 2: Use existing JAR file"
    echo "   java -jar target/youthmind-backend-0.0.1-SNAPSHOT.jar"
    echo ""
    echo "Option 3: Fix Lombok compatibility"
    echo "   Update pom.xml to use compatible Lombok version"
    echo ""
fi

