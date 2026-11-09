

package com.matcher.application.dto;

import com.matcher.domain.model.EvaluationResult;

import java.util.UUID;

public record CandidateRankDto(
    UUID candidateId,
    int rank,
    EvaluationResult evaluation
) {}