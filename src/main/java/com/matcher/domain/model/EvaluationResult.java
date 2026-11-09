

package com.matcher.domain.model;

import java.util.List;
import java.util.UUID;

public record EvaluationResult(
    UUID candidateId,
    UUID jobPostingId,
    double overallMatchScore,
    String summary,
    List<String> matchingSkills,
    List<String> missingSkills,
    List<String> recommendations
) {}
