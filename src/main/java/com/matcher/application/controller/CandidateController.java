

package com.matcher.application.controller;

import com.matcher.domain.model.EvaluationResult;
import com.matcher.domain.model.JobPosting;
import com.matcher.domain.model.ResumeChunk;
import com.matcher.domain.port.out.LlmEvaluationPort;
import com.matcher.domain.port.out.VectorStorePort;
// import com.matcher.infrastructure.adapter.email.EmailService;
import com.matcher.infrastructure.adapter.parser.PdfDocumentParser;
import com.matcher.infrastructure.adapter.persistence.EvaluationPersistenceAdapter;
import com.matcher.infrastructure.adapter.persistence.entity.CandidateEvaluationEntity;
import com.matcher.infrastructure.adapter.persistence.repository.SpringDataEvaluationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates")
public class CandidateController {

    private final VectorStorePort vectorStorePort;
    private final LlmEvaluationPort llmEvaluationPort;
    private final EvaluationPersistenceAdapter persistenceAdapter;
    // private final EmailService emailService;
    private final SpringDataEvaluationRepository evaluationRepository;
    private final PdfDocumentParser pdfDocumentParser;

    public CandidateController(
            VectorStorePort vectorStorePort,
            LlmEvaluationPort llmEvaluationPort,
            EvaluationPersistenceAdapter persistenceAdapter,
            // EmailService emailService,
            SpringDataEvaluationRepository evaluationRepository,
            PdfDocumentParser pdfDocumentParser
    ) {
        this.vectorStorePort = vectorStorePort;
        this.llmEvaluationPort = llmEvaluationPort;
        this.persistenceAdapter = persistenceAdapter;
        // this.emailService = emailService;
        this.evaluationRepository = evaluationRepository;
        this.pdfDocumentParser = pdfDocumentParser;
    }

    
    @PostMapping("/{candidateId}/resume")
    public ResponseEntity<Map<String, Object>> uploadResume(
            @PathVariable UUID candidateId,
            @RequestPart("file") MultipartFile file) throws IOException {
        try {
            List<ResumeChunk> chunks = pdfDocumentParser.parseAndChunk(
                    candidateId,
                    file.getInputStream(),
                    file.getOriginalFilename()
            );

            vectorStorePort.saveChunks(chunks);

            return ResponseEntity.ok(Map.of(
                    "message", "Resume uploaded, chunked, and vector-indexed successfully",
                    "candidateId", candidateId.toString(),
                    "totalChunks", String.valueOf(chunks.size())
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process resume PDF: " + e.getMessage()));
        }
    }

    
    @PostMapping("/{candidateId}/evaluate")
    public ResponseEntity<EvaluationResult> evaluateCandidate(
            @PathVariable UUID candidateId,
            @RequestBody JobPosting jobPosting,
            @RequestParam(defaultValue = "your-email@gmail.com", required = false) String hrEmail
    ) {
      
        String searchQuery = jobPosting.title() + " " + String.join(" ", jobPosting.requiredSkills());
        List<ResumeChunk> relevantChunks = vectorStorePort.findSimilarChunks(searchQuery, 4, candidateId);

        // Llama 3.2
        EvaluationResult result = llmEvaluationPort.evaluateMatch(jobPosting, relevantChunks);

        
        persistenceAdapter.saveEvaluation(result);

        // try {
        //     if (hrEmail != null && !hrEmail.isBlank()) {
        //         emailService.sendEvaluationReport(
        //                 hrEmail,
        //                 candidateId.toString(),
        //                 String.valueOf(result.overallMatchScore()),
        //                 result.summary()
        //         );
        //     }
        // } catch (Exception e) {
        //     System.err.println("Failed to send HR notification email: " + e.getMessage());
        // }

        return ResponseEntity.ok(result);
    }

    
    @GetMapping("/{candidateId}/evaluations")
    public ResponseEntity<List<CandidateEvaluationEntity>> getCandidateEvaluations(@PathVariable UUID candidateId) {
        List<CandidateEvaluationEntity> evaluations = 
                evaluationRepository.findByCandidateIdOrderByEvaluatedAtDesc(candidateId);
        return ResponseEntity.ok(evaluations);
    }
}