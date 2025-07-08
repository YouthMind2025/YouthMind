package ai.youthmind.repository;

import ai.youthmind.entity.AIAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AIAnalysisRepository extends JpaRepository<AIAnalysis, UUID> {
} 