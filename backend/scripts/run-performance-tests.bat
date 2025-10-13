@echo off
REM Performance Test Runner for Recommendation Engine
REM This script runs JMH benchmarks and generates performance reports

echo 🚀 Starting Recommendation Engine Performance Tests...

REM Check if Java is available
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java is not installed or not in PATH
    exit /b 1
)

REM Set up environment
set JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseStringDeduplication
set SPRING_PROFILES_ACTIVE=test

REM Create results directory
if not exist "target\performance-results" mkdir "target\performance-results"
set RESULTS_DIR=target\performance-results

REM Run benchmarks
echo 📊 Running JMH benchmarks...

REM R Vector Update Benchmark
echo   - R Vector Update Benchmark...
java %JAVA_OPTS% -cp target\classes;target\test-classes;%USERPROFILE%\.m2\repository\*\*.jar org.openjdk.jmh.Main -rf json -rff "%RESULTS_DIR%\rvector-update-benchmark.json" -f 1 ai.youthmind.benchmark.RecommendationEngineBenchmark.benchmarkRVectorUpdate

REM R Vector Retrieval Benchmark
echo   - R Vector Retrieval Benchmark...
java %JAVA_OPTS% -cp target\classes;target\test-classes;%USERPROFILE%\.m2\repository\*\*.jar org.openjdk.jmh.Main -rf json -rff "%RESULTS_DIR%\rvector-retrieval-benchmark.json" -f 1 ai.youthmind.benchmark.RecommendationEngineBenchmark.benchmarkRVectorRetrieval

REM Feed Block Generation Benchmark
echo   - Feed Block Generation Benchmark...
java %JAVA_OPTS% -cp target\classes;target\test-classes;%USERPROFILE%\.m2\repository\*\*.jar org.openjdk.jmh.Main -rf json -rff "%RESULTS_DIR%\feed-block-generation-benchmark.json" -f 1 ai.youthmind.benchmark.RecommendationEngineBenchmark.benchmarkFeedBlockGeneration

REM Event Publishing Benchmark
echo   - Event Publishing Benchmark...
java %JAVA_OPTS% -cp target\classes;target\test-classes;%USERPROFILE%\.m2\repository\*\*.jar org.openjdk.jmh.Main -rf json -rff "%RESULTS_DIR%\event-publishing-benchmark.json" -f 1 ai.youthmind.benchmark.RecommendationEngineBenchmark.benchmarkEventPublishing

REM Multiple Event Publishing Benchmark
echo   - Multiple Event Publishing Benchmark...
java %JAVA_OPTS% -cp target\classes;target\test-classes;%USERPROFILE%\.m2\repository\*\*.jar org.openjdk.jmh.Main -rf json -rff "%RESULTS_DIR%\multiple-event-publishing-benchmark.json" -f 1 ai.youthmind.benchmark.RecommendationEngineBenchmark.benchmarkMultipleEventPublishing

REM Feed Block Generation with Caching Benchmark
echo   - Feed Block Generation with Caching Benchmark...
java %JAVA_OPTS% -cp target\classes;target\test-classes;%USERPROFILE%\.m2\repository\*\*.jar org.openjdk.jmh.Main -rf json -rff "%RESULTS_DIR%\feed-block-caching-benchmark.json" -f 1 ai.youthmind.benchmark.RecommendationEngineBenchmark.benchmarkFeedBlockGenerationWithCaching

REM Generate performance report
echo 📈 Generating performance report...

echo # Recommendation Engine Performance Report > "%RESULTS_DIR%\performance-report.md"
echo. >> "%RESULTS_DIR%\performance-report.md"
echo Generated on: %date% %time% >> "%RESULTS_DIR%\performance-report.md"
echo. >> "%RESULTS_DIR%\performance-report.md"
echo ## Benchmark Results >> "%RESULTS_DIR%\performance-report.md"
echo. >> "%RESULTS_DIR%\performance-report.md"
echo ### R Vector Update >> "%RESULTS_DIR%\performance-report.md"
echo - **Target**: ^< 5ms >> "%RESULTS_DIR%\performance-report.md"
echo - **Result**: See rvector-update-benchmark.json >> "%RESULTS_DIR%\performance-report.md"
echo. >> "%RESULTS_DIR%\performance-report.md"
echo ### R Vector Retrieval >> "%RESULTS_DIR%\performance-report.md"
echo - **Target**: ^< 1ms >> "%RESULTS_DIR%\performance-report.md"
echo - **Result**: See rvector-retrieval-benchmark.json >> "%RESULTS_DIR%\performance-report.md"
echo. >> "%RESULTS_DIR%\performance-report.md"
echo ### Feed Block Generation >> "%RESULTS_DIR%\performance-report.md"
echo - **Target**: ^< 200ms >> "%RESULTS_DIR%\performance-report.md"
echo - **Result**: See feed-block-generation-benchmark.json >> "%RESULTS_DIR%\performance-report.md"
echo. >> "%RESULTS_DIR%\performance-report.md"
echo ### Event Publishing >> "%RESULTS_DIR%\performance-report.md"
echo - **Target**: ^< 10ms >> "%RESULTS_DIR%\performance-report.md"
echo - **Result**: See event-publishing-benchmark.json >> "%RESULTS_DIR%\performance-report.md"
echo. >> "%RESULTS_DIR%\performance-report.md"
echo ### Multiple Event Publishing >> "%RESULTS_DIR%\performance-report.md"
echo - **Target**: ^< 100ms for 10 events >> "%RESULTS_DIR%\performance-report.md"
echo - **Result**: See multiple-event-publishing-benchmark.json >> "%RESULTS_DIR%\performance-report.md"
echo. >> "%RESULTS_DIR%\performance-report.md"
echo ### Feed Block Generation with Caching >> "%RESULTS_DIR%\performance-report.md"
echo - **Target**: ^< 50ms (cached) >> "%RESULTS_DIR%\performance-report.md"
echo - **Result**: See feed-block-caching-benchmark.json >> "%RESULTS_DIR%\performance-report.md"
echo. >> "%RESULTS_DIR%\performance-report.md"
echo ## Performance Targets >> "%RESULTS_DIR%\performance-report.md"
echo. >> "%RESULTS_DIR%\performance-report.md"
echo ^| Operation ^| Target ^| Status ^| >> "%RESULTS_DIR%\performance-report.md"
echo ^|-----------^|--------^|--------^| >> "%RESULTS_DIR%\performance-report.md"
echo ^| R Vector Update ^| ^< 5ms ^| ⏳ ^| >> "%RESULTS_DIR%\performance-report.md"
echo ^| R Vector Retrieval ^| ^< 1ms ^| ⏳ ^| >> "%RESULTS_DIR%\performance-report.md"
echo ^| Feed Block Generation ^| ^< 200ms ^| ⏳ ^| >> "%RESULTS_DIR%\performance-report.md"
echo ^| Event Publishing ^| ^< 10ms ^| ⏳ ^| >> "%RESULTS_DIR%\performance-report.md"
echo ^| Multiple Event Publishing ^| ^< 100ms ^| ⏳ ^| >> "%RESULTS_DIR%\performance-report.md"
echo ^| Feed Block Caching ^| ^< 50ms ^| ⏳ ^| >> "%RESULTS_DIR%\performance-report.md"
echo. >> "%RESULTS_DIR%\performance-report.md"
echo ## Recommendations >> "%RESULTS_DIR%\performance-report.md"
echo. >> "%RESULTS_DIR%\performance-report.md"
echo 1. **R Vector Updates**: Optimize Redis operations and reduce serialization overhead >> "%RESULTS_DIR%\performance-report.md"
echo 2. **Feed Block Generation**: Implement parallel processing for large blocks >> "%RESULTS_DIR%\performance-report.md"
echo 3. **Event Publishing**: Use connection pooling and batch operations >> "%RESULTS_DIR%\performance-report.md"
echo 4. **Caching**: Implement local caching layer for frequently accessed data >> "%RESULTS_DIR%\performance-report.md"
echo. >> "%RESULTS_DIR%\performance-report.md"
echo ## Next Steps >> "%RESULTS_DIR%\performance-report.md"
echo. >> "%RESULTS_DIR%\performance-report.md"
echo 1. Analyze JSON benchmark results >> "%RESULTS_DIR%\performance-report.md"
echo 2. Identify performance bottlenecks >> "%RESULTS_DIR%\performance-report.md"
echo 3. Implement optimizations >> "%RESULTS_DIR%\performance-report.md"
echo 4. Re-run benchmarks to validate improvements >> "%RESULTS_DIR%\performance-report.md"

echo ✅ Performance tests completed!
echo 📊 Results saved to: %RESULTS_DIR%
echo 📈 Report generated: %RESULTS_DIR%\performance-report.md
echo.
echo To view results:
echo   type "%RESULTS_DIR%\performance-report.md"
echo   dir "%RESULTS_DIR%\*.json"
