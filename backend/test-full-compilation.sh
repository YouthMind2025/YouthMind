#!/bin/bash

echo "========================================"
echo "Testing Full Project Compilation"
echo "========================================"
echo

echo "[1/2] Compiling main sources..."
mvn compile -q
if [ $? -ne 0 ]; then
    echo "❌ MAIN COMPILATION FAILED!"
    exit 1
fi
echo "✅ Main compilation successful"

echo
echo "[2/2] Compiling test sources..."
mvn test-compile -q
if [ $? -ne 0 ]; then
    echo "❌ TEST COMPILATION FAILED!"
    exit 1
fi
echo "✅ Test compilation successful"

echo
echo "========================================"
echo "🎉 FULL COMPILATION SUCCESSFUL!"
echo "========================================"
echo
echo "All compilation errors have been fixed:"
echo "- SecurityConfig Spring Security 6.x compatibility ✅"
echo "- CORS configuration implementation ✅"  
echo "- Security headers and CSP implementation ✅"
echo "- Test files @ActiveProfiles annotations ✅"
echo
echo "You can now run the application with:"
echo "  mvn spring-boot:run"
echo
echo "Or run the CORS tests with:"
echo "  ./run-cors-tests.sh"
echo
