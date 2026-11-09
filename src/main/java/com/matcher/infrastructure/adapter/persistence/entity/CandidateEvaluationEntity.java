


package com.matcher.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "candidate_evaluations")
@Getter
@Setter
public class CandidateEvaluationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "job_posting_id", nullable = false)
    private UUID jobPostingId;

    @Column(name = "overall_match_score", nullable = false, columnDefinition = "NUMERIC")
    private Double overallMatchScore;

    @Column(name = "evaluation_summary", nullable = false, columnDefinition = "TEXT")
    private String evaluationSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "structured_feedback", nullable = false, columnDefinition = "jsonb")
    private String structuredFeedback;

    @Column(name = "evaluated_at")
    private OffsetDateTime evaluatedAt = OffsetDateTime.now();
}