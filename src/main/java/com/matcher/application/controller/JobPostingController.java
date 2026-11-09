

package com.matcher.application.controller;

import com.matcher.application.dto.CandidateRankDto;
import com.matcher.domain.model.EvaluationResult;
import com.matcher.domain.model.JobPosting;
import com.matcher.domain.model.ResumeChunk;
import com.matcher.domain.port.out.LlmEvaluationPort;
import com.matcher.domain.port.out.VectorStorePort;
import com.matcher.infrastructure.adapter.persistence.EvaluationPersistenceAdapter;
import com.matcher.infrastructure.adapter.persistence.entity.CandidateEvaluationEntity;
import com.matcher.infrastructure.adapter.persistence.entity.JobPostingEntity;
import com.matcher.infrastructure.adapter.persistence.repository.SpringDataEvaluationRepository;
import com.matcher.infrastructure.adapter.persistence.repository.SpringDataJobPostingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobPostingController {

    private final SpringDataJobPostingRepository jobPostingRepository;
    private final SpringDataEvaluationRepository evaluationRepository;
    private final VectorStorePort vectorStorePort;
    private final LlmEvaluationPort llmEvaluationPort;
    private final EvaluationPersistenceAdapter persistenceAdapter;

    public JobPostingController(
            SpringDataJobPostingRepository jobPostingRepository,
            SpringDataEvaluationRepository evaluationRepository,
            VectorStorePort vectorStorePort,
            LlmEvaluationPort llmEvaluationPort,
            EvaluationPersistenceAdapter persistenceAdapter
    ) {
        this.jobPostingRepository = jobPostingRepository;
        this.evaluationRepository = evaluationRepository;
        this.vectorStorePort = vectorStorePort;
        this.llmEvaluationPort = llmEvaluationPort;
        this.persistenceAdapter = persistenceAdapter;
    }

    
    @PostMapping
    public ResponseEntity<JobPostingEntity> createJobPosting(@RequestBody JobPostingEntity jobPosting) {
        JobPostingEntity savedJob = jobPostingRepository.save(jobPosting);
        return ResponseEntity.ok(savedJob);
    }

    
    @GetMapping
    public ResponseEntity<List<JobPostingEntity>> getAllJobPostings() {
        return ResponseEntity.ok(jobPostingRepository.findAll());
    }

    
    @GetMapping("/{jobId}/top-candidates")
    public ResponseEntity<List<CandidateRankDto>> getTopCandidatesForJob(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "3") int limit
    ) {
        JobPostingEntity jobEntity = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job posting not found with ID: " + jobId));

        JobPosting jobPosting = new JobPosting(
                jobEntity.getId(),
                jobEntity.getTitle(),
                jobEntity.getDepartment(),
                jobEntity.getMinYearsExperience(),
                jobEntity.getRequiredEducation(),
                jobEntity.getRequiredSkills(),
                jobEntity.getDescription()
        );

        String searchQuery = jobPosting.title() + " " + String.join(" ", jobPosting.requiredSkills());
        List<ResumeChunk> topChunks = vectorStorePort.findGlobalSimilarChunks(searchQuery, limit * 4);

        Map<UUID, List<ResumeChunk>> chunksByCandidate = topChunks.stream()
                .filter(chunk -> chunk.candidateId() != null)
                .collect(Collectors.groupingBy(ResumeChunk::candidateId));

        List<CandidateRankDto> rankedCandidates = new ArrayList<>();
        int rankCounter = 1;

        for (Map.Entry<UUID, List<ResumeChunk>> entry : chunksByCandidate.entrySet()) {
            UUID candidateId = entry.getKey();
            List<ResumeChunk> candidateChunks = entry.getValue();

           
            EvaluationResult evaluation = llmEvaluationPort.evaluateMatch(jobPosting, candidateChunks);

         
            persistenceAdapter.saveEvaluation(evaluation);

            rankedCandidates.add(new CandidateRankDto(candidateId, rankCounter++, evaluation));

            if (rankedCandidates.size() >= limit) {
                break;
            }
        }

        rankedCandidates.sort((a, b) -> Double.compare(b.evaluation().overallMatchScore(), a.evaluation().overallMatchScore()));

        return ResponseEntity.ok(rankedCandidates);
    }

   
    @GetMapping("/{jobId}/evaluations")
    public ResponseEntity<List<CandidateEvaluationEntity>> getSavedEvaluationsForJob(@PathVariable UUID jobId) {
        List<CandidateEvaluationEntity> evaluations = 
                evaluationRepository.findByJobPostingIdOrderByOverallMatchScoreDesc(jobId);
        return ResponseEntity.ok(evaluations);
    }
}