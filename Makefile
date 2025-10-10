# YouthMind Recommendation Engine Makefile

.PHONY: help validate validate-quick clean build test docker-up docker-down

# Default target
help:
	@echo "YouthMind Recommendation Engine - Available targets:"
	@echo ""
	@echo "  validate        - Run full validation suite"
	@echo "  validate-quick  - Run quick validation (skip long waits/JMH)"
	@echo "  clean          - Clean build artifacts and validation output"
	@echo "  build          - Build the application"
	@echo "  test           - Run tests"
	@echo "  docker-up      - Start docker-compose services"
	@echo "  docker-down    - Stop docker-compose services"
	@echo ""
	@echo "Validation Configuration:"
	@echo "  Set environment variables or create .env file:"
	@echo "    BASE_URL     - Application base URL (default: http://localhost:8080)"
	@echo "    TOK          - User authentication token"
	@echo "    ADMIN_TOK    - Admin authentication token"
	@echo "    USER_ID      - Test user ID (auto-generated if not set)"
	@echo ""
	@echo "Example:"
	@echo "  make validate TOK=your_token ADMIN_TOK=your_admin_token"

# Full validation suite
validate:
	@echo "Running full validation suite..."
	@chmod +x tools/validation/validate.sh
	@tools/validation/validate.sh

# Quick validation (skip long waits and JMH)
validate-quick:
	@echo "Running quick validation suite..."
	@chmod +x tools/validation/validate.sh
	@QUICK_MODE=true tools/validation/validate.sh

# Clean build artifacts and validation output
clean:
	@echo "Cleaning build artifacts and validation output..."
	@rm -rf target/
	@rm -rf build/
	@rm -rf tools/validation/output/
	@rm -rf .gradle/
	@rm -rf .mvn/

# Build the application
build:
	@echo "Building application..."
	@if [ -f "pom.xml" ]; then \
		if [ -f "mvnw" ]; then ./mvnw clean compile; else mvn clean compile; fi \
	elif [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then \
		if [ -f "gradlew" ]; then ./gradlew clean build; else gradle clean build; fi \
	else \
		echo "No build tool detected (pom.xml or build.gradle)"; exit 1; \
	fi

# Run tests
test:
	@echo "Running tests..."
	@if [ -f "pom.xml" ]; then \
		if [ -f "mvnw" ]; then ./mvnw test; else mvn test; fi \
	elif [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then \
		if [ -f "gradlew" ]; then ./gradlew test; else gradle test; fi \
	else \
		echo "No build tool detected (pom.xml or build.gradle)"; exit 1; \
	fi

# Start docker-compose services
docker-up:
	@echo "Starting docker-compose services..."
	@if [ -f "docker-compose.yml" ]; then \
		docker-compose up -d; \
	else \
		echo "docker-compose.yml not found"; exit 1; \
	fi

# Stop docker-compose services
docker-down:
	@echo "Stopping docker-compose services..."
	@if [ -f "docker-compose.yml" ]; then \
		docker-compose down; \
	else \
		echo "docker-compose.yml not found"; exit 1; \
	fi

# Development setup
dev-setup:
	@echo "Setting up development environment..."
	@cp tools/validation/config.env.example .env
	@echo "Created .env file - please update with your tokens"
	@echo "Run 'make docker-up' to start services"
	@echo "Run 'make validate' to run validation suite"
