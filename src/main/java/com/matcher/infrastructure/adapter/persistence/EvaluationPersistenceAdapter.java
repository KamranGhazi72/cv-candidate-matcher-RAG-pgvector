package com.matcher.infrastructure.adapter.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matcher.domain.model.EvaluationResult;
import com.matcher.infrastructure.adapter.persistence.entity.CandidateEntity;
import com.matcher.infrastructure.adapter.persistence.entity.CandidateEvaluationEntity;
import com.matcher.infrastructure.adapter.persistence.repository.SpringDataCandidateRepository;
import com.matcher.infrastructure.adapter.persistence.repository.SpringDataEvaluationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class EvaluationPersistenceAdapter {

    private final SpringDataEvaluationRepository evaluationRepository;
    private final SpringDataCandidateRepository candidateRepository;
    private final ObjectMapper objectMapper;

    public EvaluationPersistenceAdapter(
            SpringDataEvaluationRepository evaluationRepository,
            SpringDataCandidateRepository candidateRepository,
            ObjectMapper objectMapper) {
        this.evaluationRepository = evaluationRepository;
        this.candidateRepository = candidateRepository;
        this.objectMapper = objectMapper;
    }

    public CandidateEvaluationEntity saveEvaluation(EvaluationResult result) {

        UUID candidateId = result.candidateId();

        if (candidateId != null && !candidateRepository.existsById(candidateId)) {
            String fullIdStr = candidateId.toString();
            CandidateEntity candidate = new CandidateEntity(
                    candidateId,
                    "Candidate",
                    fullIdStr.substring(0, 8),
                    "candidate_" + fullIdStr + "@example.com" 
            );
            candidateRepository.save(candidate);
        }

        CandidateEvaluationEntity entity = new CandidateEvaluationEntity();
        entity.setCandidateId(result.candidateId());
        entity.setJobPostingId(result.jobPostingId());
        entity.setOverallMatchScore(result.overallMatchScore());
        entity.setEvaluationSummary(result.summary());

        try {
            Map<String, Object> feedbackMap = Map.of(
                    "matchingSkills", result.matchingSkills() != null ? result.matchingSkills() : List.of(),
                    "missingSkills", result.missingSkills() != null ? result.missingSkills() : List.of(),
                    "recommendations", result.recommendations() != null ? result.recommendations() : List.of());
            entity.setStructuredFeedback(objectMapper.writeValueAsString(feedbackMap));
        } catch (Exception e) {
            entity.setStructuredFeedback("{}");
        }

        return evaluationRepository.save(entity);
    }
}