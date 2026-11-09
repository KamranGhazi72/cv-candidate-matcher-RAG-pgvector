

package com.matcher.infrastructure.adapter.persistence.repository;

import com.matcher.infrastructure.adapter.persistence.entity.CandidateEvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataEvaluationRepository extends JpaRepository<CandidateEvaluationEntity, UUID> {
    
    List<CandidateEvaluationEntity> findByCandidateIdOrderByEvaluatedAtDesc(UUID candidateId);

    List<CandidateEvaluationEntity> findByJobPostingIdOrderByOverallMatchScoreDesc(UUID jobPostingId);
}