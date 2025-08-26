#!/bin/bash

set -e

# Configuration
PROJECT_DIR="$(pwd)/scan"
REPORT_DIR="$PROJECT_DIR/security-reports"
LOG_FILE="$REPORT_DIR/scan.log"
SQLMAP_DIR="$PROJECT_DIR/sqlmap"
XSS_SCAN_DIR="$PROJECT_DIR/xss-scan"
CSRF_SCAN_DIR="$PROJECT_DIR/csrf-scan"
JWT_SCAN_DIR="$PROJECT_DIR/jwt-scan"
APPLICATION_URL=${APPLICATION_URL:-"https://localhost:8080"}
MAX_WAIT_TIME=${MAX_WAIT_TIME:-120} # Maximum wait time for application to start in seconds

# Create report directory
mkdir -p "$REPORT_DIR" "$SQLMAP_DIR" "$XSS_SCAN_DIR" "$CSRF_SCAN_DIR" "$JWT_SCAN_DIR"

# Initialize log file
echo "Security Scan started at $(date)" > "$LOG_FILE"
echo "Application URL: $APPLICATION_URL" >> "$LOG_FILE"

wait_for_application() {
    echo "Waiting for application to be ready at $APPLICATION_URL..." | tee -a "$LOG_FILE"
    local start_time=$(date +%s)

    while true; do
        if curl -s -f "$APPLICATION_URL/actuator/health" > /dev/null 2>&1; then
            echo "Application is ready!" | tee -a "$LOG_FILE"
            return 0
        fi

        local current_time=$(date +%s)
        local elapsed_time=$((current_time - start_time))

        if [ $elapsed_time -ge $MAX_WAIT_TIME ]; then
            echo "ERROR: Application did not become ready within $MAX_WAIT_TIME seconds" | tee -a "$LOG_FILE"
            return 1
        fi

        echo "Waiting for application... ($elapsed_time/$MAX_WAIT_TIME seconds)" | tee -a "$LOG_FILE"
        sleep 5
    done
}

# Wait for application to be ready
if ! wait_for_application; then
    echo "Failing security scan due to application not being ready" | tee -a "$LOG_FILE"
    exit 1
fi

# Check for required tools
check_tool() {
    if ! command -v "$1" &> /dev/null; then
        echo "Error: $1 is not installed. Please install it and try again."
        exit 1
    fi
}

check_tool curl
check_tool python3
check_tool jq  # For parsing JSON responses

# Get JWT token at the beginning
echo "Attempting to register and get JWT token..." | tee -a "$LOG_FILE"

# Generate a unique email to avoid duplicate registration issues
timestamp=$(date +%s)
email="testuser_${timestamp}@example.com"

# Register a new user to obtain JWT token
register_response=$(curl -k -s -X POST "https://localhost:8080/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"otp\":\"123456\"}" \
    -D "$JWT_SCAN_DIR/register_headers.txt")

# Extract token from response
JWT_TOKEN=""

# Try to get token from response body
if [ -n "$register_response" ]; then
    JWT_TOKEN=$(echo "$register_response" | jq -r '.token' 2>/dev/null || echo "")

    USER_ID=$(echo "$register_response" | jq -r '.userId' 2>/dev/null || echo "")
    USER_EMAIL=$email

    echo "User ID: $USER_ID" | tee -a "$LOG_FILE"
    echo "User Email: $USER_EMAIL" | tee -a "$LOG_FILE"
fi

if [ -n "$JWT_TOKEN" ]; then
    echo "JWT token obtained successfully through registration: ${JWT_TOKEN:0:20}..." | tee -a "$LOG_FILE"
    echo "$JWT_TOKEN" > "$JWT_SCAN_DIR/jwt_token.txt"
    echo "Registered with email: $email" | tee -a "$LOG_FILE"
else
    echo "ERROR: Failed to obtain JWT token through registration. Response: $register_response" | tee -a "$LOG_FILE"
    echo "ERROR: Failed to obtain JWT token through registration. Response: $register_response" > "$JWT_SCAN_DIR/registration_error.txt"
    echo ""
fi

# SQL Injection testing with sqlmap (if available)
if command -v sqlmap &> /dev/null; then
    echo "Running SQL Injection testing with sqlmap..."

    SQL_POST_ENDPOINTS=(
        "/v1/posts/create:content=test&emotionTag=stress"
    )

    # Test common SQL injection endpoints with proper parameters
    SQL_GET_ENDPOINTS=(
        "/v1/posts/feed"
        "/v1/posts/by-emotion/stress"
        "/v1/posts/by-user/$USER_ID"
        "/api/users"
        "/api/users/$USER_ID"
        "/api/users/email/$USER_EMAIL"
    )

    # Prepare headers file for sqlmap if we have a token
    if [ -n "$JWT_TOKEN" ]; then
        echo "Authorization: Bearer $JWT_TOKEN" > "$SQLMAP_DIR/headers.txt"
        SQLMAP_HEADERS="--headers=\"Authorization: Bearer $JWT_TOKEN\""
    else
        SQLMAP_HEADERS=""
    fi

    # Test POST endpoints
    for endpoint_data in "${SQL_POST_ENDPOINTS[@]}"; do
        # Split endpoint and data
        endpoint=$(echo "$endpoint_data" | cut -d':' -f1)
        data=$(echo "$endpoint_data" | cut -d':' -f2)

        full_url="${APPLICATION_URL}${endpoint}"
        echo "Testing $full_url (POST) for SQL injection vulnerabilities..." | tee -a "$LOG_FILE"

        # Create a safe filename for the report
        safe_filename=$(echo "$endpoint" | tr '/?=&' '____')

        # sqlmap command for POST endpoints with headers
        eval sqlmap -u "$full_url" \
            --data="$data" \
            --batch \
            --level=1 \
            --risk=1 \
            --output-dir="$SQLMAP_DIR" \
            $SQLMAP_HEADERS \
            --flush-session 2>&1 | tee "${SQLMAP_DIR}/sqlmap_post_${safe_filename}.txt"
    done

    # Test GET endpoints
    for endpoint in "${SQL_GET_ENDPOINTS[@]}"; do
        full_url="${APPLICATION_URL}${endpoint}"
        echo "Testing $full_url for SQL injection vulnerabilities..." | tee -a "$LOG_FILE"

        # Create a safe filename for the report
        safe_filename=$(echo "$endpoint" | tr '/?=&' '____')

        # Updated sqlmap command with proper parameters and headers
        eval sqlmap -u "$full_url" \
            --batch \
            --level=1 \
            --risk=1 \
            --output-dir="$SQLMAP_DIR" \
            $SQLMAP_HEADERS \
            --flush-session 2>&1 | tee "${SQLMAP_DIR}/sqlmap_${safe_filename}.txt"
    done

else
    echo "sqlmap not found, skipping SQL injection tests" | tee -a "$LOG_FILE"
fi

# XSS Vulnerability Testing
echo "Running XSS vulnerability testing..."

# Common XSS payloads
XSS_PAYLOADS=(
    "<script>alert('XSS')</script>"
    "\"><script>alert('XSS')</script>"
    "javascript:alert('XSS')"
    "\"><img src=x onerror=alert('XSS')>"
    "\"><svg onload=alert('XSS')>"
    "\"><iframe src=\"javascript:alert('XSS')\">"
)

# Endpoints to test for XSS
XSS_ENDPOINTS=(
    "/v1/posts/feed"
    "/v1/posts/by-emotion/stress"
    "/v1/posts/by-user/$USER_ID"
    "/api/users"
    "/api/users/$USER_ID"
    "/api/users/email/$USER_EMAIL"
)

# Function to test XSS on a URL
test_xss() {
    local url="$1"
    local payload="$2"
    local endpoint_name="$3"

    # Replace TEST with the payload
    local test_url="${url//TEST/$payload}"

    # URL encode the payload
    local encoded_payload=$(printf "%s" "$payload" | curl -Gso /dev/null -w %{url_effective} --data-urlencode @- "" | cut -c 3-)
    local encoded_test_url="${url//TEST/$encoded_payload}"

    echo "Testing URL: $test_url" >> "$XSS_SCAN_DIR/xss_test_${endpoint_name}.log"

    # Prepare curl command with authentication if we have a token
    local curl_cmd="curl -s -L \"$encoded_test_url\" -H \"User-Agent: Security-Scanner\""
    if [ -n "$JWT_TOKEN" ]; then
        curl_cmd="$curl_cmd -H \"Authorization: Bearer $JWT_TOKEN\""
    fi

    # Make the request and check if the payload is reflected
    local response=$(eval "$curl_cmd")

    # Check if the payload is reflected in the response
    if echo "$response" | grep -q "$payload"; then
        echo "POSSIBLE XSS VULNERABILITY: Payload reflected in response for $test_url" | tee -a "$LOG_FILE"
        echo "POSSIBLE XSS VULNERABILITY: Payload reflected in response for $test_url" >> "$XSS_SCAN_DIR/xss_test_${endpoint_name}.log"
        return 1
    else
        echo "No reflection detected for payload: $payload" >> "$XSS_SCAN_DIR/xss_test_${endpoint_name}.log"
        return 0
    fi
}

# Test each endpoint with each payload
for endpoint in "${XSS_ENDPOINTS[@]}"; do
    full_url="${APPLICATION_URL}${endpoint}"

    # Create a safe filename for the endpoint
    safe_filename=$(echo "$endpoint" | tr '/?=&' '____')

    echo "Testing $full_url for XSS vulnerabilities..." | tee -a "$LOG_FILE"
    echo "Testing $full_url for XSS vulnerabilities..." > "$XSS_SCAN_DIR/xss_test_${safe_filename}.log"

    for payload in "${XSS_PAYLOADS[@]}"; do
        test_xss "$full_url" "$payload" "$safe_filename"
    done
done

# CSRF Protection Validation
echo "Running CSRF protection validation..."

# Endpoints to test for CSRF protection
CSRF_ENDPOINTS=(
    "/v1/auth/register"
    "/v1/auth/login"
    "/v1/auth/logout"
    "/v1/auth/refresh"
    "/v1/posts/create"
)

# Function to check CSRF protection
check_csrf() {
    local url="$1"
    local endpoint_name="$2"

    echo "Testing CSRF protection for $url" | tee -a "$LOG_FILE"
    echo "Testing CSRF protection for $url" > "$CSRF_SCAN_DIR/csrf_test_${endpoint_name}.log"

    # First, get the page to check for CSRF token
    local curl_cmd="curl -s -c \"$CSRF_SCAN_DIR/cookies.txt\" \"$APPLICATION_URL/login\""
    if [ -n "$JWT_TOKEN" ]; then
        curl_cmd="$curl_cmd -H \"Authorization: Bearer $JWT_TOKEN\""
    fi

    local response=$(eval "$curl_cmd")

    # Check if there's a CSRF token in the response
    if echo "$response" | grep -q -i "csrf\|_csrf\|token"; then
        echo "CSRF token found in page" >> "$CSRF_SCAN_DIR/csrf_test_${endpoint_name}.log"

        # Try to extract the token
        local token=$(echo "$response" | grep -o -i 'name="[^"]*csrf[^"]*" value="[^"]*"' | head -1 | sed 's/.*value="\([^"]*\)".*/\1/')

        if [ -n "$token" ]; then
            echo "Extracted CSRF token: $token" >> "$CSRF_SCAN_DIR/csrf_test_${endpoint_name}.log"

            # Try to make a POST request without the token
            local post_cmd="curl -s -X POST -H \"Content-Type: application/x-www-form-urlencoded\" \
                -b \"$CSRF_SCAN_DIR/cookies.txt\" \
                -d \"test=data\""

            if [ -n "$JWT_TOKEN" ]; then
                post_cmd="$post_cmd -H \"Authorization: Bearer $JWT_TOKEN\""
            fi

            post_cmd="$post_cmd \"$url\""
            local post_response=$(eval "$post_cmd")

            # Check if the request was rejected
            if echo "$post_response" | grep -q -i "invalid.*csrf\|csrf.*token\|forbidden\|403"; then
                echo "CSRF protection appears to be working: Request without token was rejected" | tee -a "$LOG_FILE"
                echo "CSRF protection appears to be working: Request without token was rejected" >> "$CSRF_SCAN_DIR/csrf_test_${endpoint_name}.log"
                return 0
            else
                echo "POSSIBLE CSRF VULNERABILITY: Request without token was accepted" | tee -a "$LOG_FILE"
                echo "POSSIBLE CSRF VULNERABILITY: Request without token was accepted" >> "$CSRF_SCAN_DIR/csrf_test_${endpoint_name}.log"
                return 1
            fi
        fi
    else
        echo "No CSRF token found in page" >> "$CSRF_SCAN_DIR/csrf_test_${endpoint_name}.log"

        # Try to make a POST request to check if it's accepted without token
        local post_cmd="curl -s -X POST -H \"Content-Type: application/x-www-form-urlencoded\" \
            -b \"$CSRF_SCAN_DIR/cookies.txt\" \
            -d \"test=data\""

        if [ -n "$JWT_TOKEN" ]; then
            post_cmd="$post_cmd -H \"Authorization: Bearer $JWT_TOKEN\""
        fi

        post_cmd="$post_cmd \"$url\""
        local post_response=$(eval "$post_cmd")

        # Check if the request was accepted
        if echo "$post_response" | grep -q -i "success\|ok\|200"; then
            echo "POSSIBLE CSRF VULNERABILITY: No CSRF protection detected and request was accepted" | tee -a "$LOG_FILE"
            echo "POSSIBLE CSRF VULNERABILITY: No CSRF protection detected and request was accepted" >> "$CSRF_SCAN_DIR/csrf_test_${endpoint_name}.log"
            return 1
        else
            echo "Request without CSRF token was rejected (might be protected by other means)" >> "$CSRF_SCAN_DIR/csrf_test_${endpoint_name}.log"
            return 0
        fi
    fi
}

# Test each endpoint for CSRF protection
for endpoint in "${CSRF_ENDPOINTS[@]}"; do
    full_url="${APPLICATION_URL}${endpoint}"

    # Create a safe filename for the endpoint
    safe_filename=$(echo "$endpoint" | tr '/?=&' '____')

    check_csrf "$full_url" "$safe_filename"
done

# JWT Token Security Audit
echo "Running JWT token security audit..."

if [ -n "$JWT_TOKEN" ]; then
    echo "JWT token found: ${JWT_TOKEN:0:20}..." | tee -a "$LOG_FILE"
    echo "Full token: $JWT_TOKEN" > "$JWT_SCAN_DIR/jwt_token.txt"

    # Function to decode JWT tokens
    decode_jwt() {
        local token="$1"
        # Split the token into parts
        local header=$(echo "$token" | cut -d'.' -f1)
        local payload=$(echo "$token" | cut -d'.' -f2)

        # Add padding if needed and decode
        local header_decoded=$(echo "$header" | awk '{print $0 substr("===", (4 - (length($0) % 4)) % 4, 4)}' | base64 -d 2>/dev/null || echo "INVALID")
        local payload_decoded=$(echo "$payload" | awk '{print $0 substr("===", (4 - (length($0) % 4)) % 4, 4)}' | base64 -d 2>/dev/null || echo "INVALID")

        echo "Header: $header_decoded"
        echo "Payload: $payload_decoded"
    }

    # Decode and analyze the token
    echo "Decoding JWT token..." | tee -a "$LOG_FILE"
    decode_jwt "$JWT_TOKEN" > "$JWT_SCAN_DIR/jwt_decoded.txt"

    # Check for common JWT vulnerabilities
    echo "Analyzing JWT for common vulnerabilities..." | tee -a "$LOG_FILE"

    # 1. Check if algorithm is none
    if echo "$JWT_TOKEN" | grep -q "eyJhbGciOiJub25l"; then
        echo "CRITICAL: JWT uses 'none' algorithm - vulnerable to algorithm confusion attacks" | tee -a "$LOG_FILE"
        echo "CRITICAL: JWT uses 'none' algorithm" >> "$JWT_SCAN_DIR/jwt_analysis.txt"
    fi

    # 2. Check if sensitive data is in the token
    if echo "$JWT_TOKEN" | base64 -d 2>/dev/null | grep -q -i "password\|secret\|key\|admin"; then
        echo "WARNING: JWT may contain sensitive data" | tee -a "$LOG_FILE"
        echo "WARNING: JWT may contain sensitive data" >> "$JWT_SCAN_DIR/jwt_analysis.txt"
    fi

    # 3. Check if token has long expiration
    expiration=$(echo "$JWT_TOKEN" | base64 -d 2>/dev/null | grep -o '"exp":[0-9]*' | cut -d':' -f2)
    if [ -n "$expiration" ]; then
        current_time=$(date +%s)
        if [ "$expiration" -gt "$((current_time + 86400 * 30))" ]; then
            echo "WARNING: JWT has long expiration time (more than 30 days)" | tee -a "$LOG_FILE"
            echo "WARNING: JWT has long expiration time (more than 30 days)" >> "$JWT_SCAN_DIR/jwt_analysis.txt"
        fi
    fi

    # 4. Check if token uses weak signature
    if echo "$JWT_TOKEN" | grep -q "HS256"; then
        echo "WARNING: JWT uses HS256 algorithm which may be vulnerable to brute force attacks if secret is weak" | tee -a "$LOG_FILE"
        echo "WARNING: JWT uses HS256 algorithm" >> "$JWT_SCAN_DIR/jwt_analysis.txt"
    fi

else
    echo "No JWT token available for security audit." | tee -a "$LOG_FILE"
fi

# Generate summary
echo "Generating scan summary..."
{
    echo "========================================"
    echo "Security Scan Completed at $(date)"
    echo "========================================"
    echo "Application URL: $APPLICATION_URL"
    echo "Reports available in: $REPORT_DIR"
    echo ""
    echo "Scan Components:"
    echo "1. SQL Injection Tests: $SQLMAP_DIR/"
    echo "2. XSS Tests: $XSS_SCAN_DIR/"
    echo "3. CSRF Protection Tests: $CSRF_SCAN_DIR/"
    echo "4. JWT Security Audit: $JWT_SCAN_DIR/"
    echo ""
    echo "Vulnerability Summary:"
    if [ -d "$SQLMAP_DIR" ]; then
        grep -r -i "sql injection" "$SQLMAP_DIR" | grep -c -i "vulnerable" | awk '{print "SQL injection vulnerabilities: " $1}'
    fi
    grep -r -i "POSSIBLE XSS VULNERABILITY" "$XSS_SCAN_DIR" | grep -c -i "vulnerable" | awk '{print "XSS vulnerabilities: " $1}'
    grep -r -i "POSSIBLE CSRF VULNERABILITY" "$CSRF_SCAN_DIR" | grep -c -i "vulnerable" | awk '{print "CSRF vulnerabilities: " $1}'
    if [ -f "$JWT_SCAN_DIR/jwt_analysis.txt" ]; then
        grep -c -i "CRITICAL\|WARNING" "$JWT_SCAN_DIR/jwt_analysis.txt" | awk '{print "JWT issues: " $1}'
    fi
} | tee -a "$LOG_FILE" "$REPORT_DIR/summary.txt"

# Check for vulnerabilities
check_vulnerabilities() {
    local exit_code=0

    # Check SQL injection results if sqlmap was used
    if [ -d "$SQLMAP_DIR" ]; then
        if grep -r -i "sql injection" "$SQLMAP_DIR" | grep -q -i "vulnerable"; then
            echo "CRITICAL: SQL injection vulnerabilities detected!" | tee -a "$LOG_FILE"
            exit_code=1
        fi
    fi

    # Check XSS results
    if grep -r -i "POSSIBLE XSS VULNERABILITY" "$XSS_SCAN_DIR" | grep -q -i "vulnerable"; then
        echo "CRITICAL: XSS vulnerabilities detected!" | tee -a "$LOG_FILE"
        exit_code=1
    fi

    # Check CSRF results
    if grep -r -i "POSSIBLE CSRF VULNERABILITY" "$CSRF_SCAN_DIR" | grep -q -i "vulnerable"; then
        echo "CRITICAL: CSRF vulnerabilities detected!" | tee -a "$LOG_FILE"
        exit_code=1
    fi

    # Check JWT results
    if [ -f "$JWT_SCAN_DIR/jwt_analysis.txt" ] && grep -q -i "CRITICAL" "$JWT_SCAN_DIR/jwt_analysis.txt"; then
        echo "CRITICAL: JWT security issues detected!" | tee -a "$LOG_FILE"
        exit_code=1
    fi

    return $exit_code
}

# Check for vulnerabilities
if check_vulnerabilities; then
    echo "Scan completed. No critical vulnerabilities detected." | tee -a "$LOG_FILE"
else
    echo "CRITICAL: Security vulnerabilities were detected!" | tee -a "$LOG_FILE"
    exit 1
fi

exit 0