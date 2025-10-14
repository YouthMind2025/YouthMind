#!/bin/bash

# Performance Test Runner for Recommendation Engine
# This script runs JMH benchmarks and generates performance reports

set -e

echo "🚀 Starting Recommendation Engine Performance Tests..."

# Check if JMH is available
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed or not in PATH"
    exit 1
fi

# Set up environment
export JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseStringDeduplication"
export SPRING_PROFILES_ACTIVE=test

# Create results directory
mkdir -p target/performance-results
RESULTS_DIR="target/performance-results"

# Run benchmarks
echo "📊 Running JMH benchmarks..."

# R Vector Update Benchmark
echo "  - R Vector Update Benchmark..."
java $JAVA_OPTS -cp target/classes:target/test-classes:$(find ~/.m2/repository -name "*.jar" | tr '\n' ':') \
    org.openjdk.jmh.Main \
    -rf json \
    -rff "$RESULTS_DIR/rvector-update-benchmark.json" \
    -f 1 \
    ai.youthmind.benchmark.RecommendationEngineBenchmark.benchmarkRVectorUpdate

# R Vector Retrieval Benchmark
echo "  - R Vector Retrieval Benchmark..."
java $JAVA_OPTS -cp target/classes:target/test-classes:$(find ~/.m2/repository -name "*.jar" | tr '\n' ':') \
    org.openjdk.jmh.Main \
    -rf json \
    -rff "$RESULTS_DIR/rvector-retrieval-benchmark.json" \
    -f 1 \
    ai.youthmind.benchmark.RecommendationEngineBenchmark.benchmarkRVectorRetrieval

# Feed Block Generation Benchmark
echo "  - Feed Block Generation Benchmark..."
java $JAVA_OPTS -cp target/classes:target/test-classes:$(find ~/.m2/repository -name "*.jar" | tr '\n' ':') \
    org.openjdk.jmh.Main \
    -rf json \
    -rff "$RESULTS_DIR/feed-block-generation-benchmark.json" \
    -f 1 \
    ai.youthmind.benchmark.RecommendationEngineBenchmark.benchmarkFeedBlockGeneration

# Event Publishing Benchmark
echo "  - Event Publishing Benchmark..."
java $JAVA_OPTS -cp target/classes:target/test-classes:$(find ~/.m2/repository -name "*.jar" | tr '\n' ':') \
    org.openjdk.jmh.Main \
    -rf json \
    -rff "$RESULTS_DIR/event-publishing-benchmark.json" \
    -f 1 \
    ai.youthmind.benchmark.RecommendationEngineBenchmark.benchmarkEventPublishing

# Multiple Event Publishing Benchmark
echo "  - Multiple Event Publishing Benchmark..."
java $JAVA_OPTS -cp target/classes:target/test-classes:$(find ~/.m2/repository -name "*.jar" | tr '\n' ':') \
    org.openjdk.jmh.Main \
    -rf json \
    -rff "$RESULTS_DIR/multiple-event-publishing-benchmark.json" \
    -f 1 \
    ai.youthmind.benchmark.RecommendationEngineBenchmark.benchmarkMultipleEventPublishing

# Feed Block Generation with Caching Benchmark
echo "  - Feed Block Generation with Caching Benchmark..."
java $JAVA_OPTS -cp target/classes:target/test-classes:$(find ~/.m2/repository -name "*.jar" | tr '\n' ':') \
    org.openjdk.jmh.Main \
    -rf json \
    -rff "$RESULTS_DIR/feed-block-caching-benchmark.json" \
    -f 1 \
    ai.youthmind.benchmark.RecommendationEngineBenchmark.benchmarkFeedBlockGenerationWithCaching

# Generate performance report
echo "📈 Generating performance report..."

cat > "$RESULTS_DIR/performance-report.md" << EOF
# Recommendation Engine Performance Report

Generated on: $(date)

## Benchmark Results

### R Vector Update
- **Target**: < 5ms
- **Result**: See rvector-update-benchmark.json

### R Vector Retrieval
- **Target**: < 1ms
- **Result**: See rvector-retrieval-benchmark.json

### Feed Block Generation
- **Target**: < 200ms
- **Result**: See feed-block-generation-benchmark.json

### Event Publishing
- **Target**: < 10ms
- **Result**: See event-publishing-benchmark.json

### Multiple Event Publishing
- **Target**: < 100ms for 10 events
- **Result**: See multiple-event-publishing-benchmark.json

### Feed Block Generation with Caching
- **Target**: < 50ms (cached)
- **Result**: See feed-block-caching-benchmark.json

## Performance Targets

| Operation | Target | Status |
|-----------|--------|--------|
| R Vector Update | < 5ms | ⏳ |
| R Vector Retrieval | < 1ms | ⏳ |
| Feed Block Generation | < 200ms | ⏳ |
| Event Publishing | < 10ms | ⏳ |
| Multiple Event Publishing | < 100ms | ⏳ |
| Feed Block Caching | < 50ms | ⏳ |

## Recommendations

1. **R Vector Updates**: Optimize Redis operations and reduce serialization overhead
2. **Feed Block Generation**: Implement parallel processing for large blocks
3. **Event Publishing**: Use connection pooling and batch operations
4. **Caching**: Implement local caching layer for frequently accessed data

## Next Steps

1. Analyze JSON benchmark results
2. Identify performance bottlenecks
3. Implement optimizations
4. Re-run benchmarks to validate improvements
EOF

echo "✅ Performance tests completed!"
echo "📊 Results saved to: $RESULTS_DIR"
echo "📈 Report generated: $RESULTS_DIR/performance-report.md"
echo ""
echo "To view results:"
echo "  cat $RESULTS_DIR/performance-report.md"
echo "  ls -la $RESULTS_DIR/*.json"
