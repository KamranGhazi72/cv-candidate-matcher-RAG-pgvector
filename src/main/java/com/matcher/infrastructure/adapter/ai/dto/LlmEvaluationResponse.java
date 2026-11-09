

package com.matcher.infrastructure.adapter.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record LlmEvaluationResponse(
    @JsonPropertyDescription("Overall match percentage score between 0.0 and 100.0 based on candidate qualifications")
    double overallMatchScore,
    
    @JsonPropertyDescription("A concise executive summary evaluating the candidate fit")
    String summary,
    
    @JsonPropertyDescription("JSON array of strings representing matching skills found in resume, e.g. [\"Java\", \"Spring\"]")
    List<String> matchingSkills,
    
    @JsonPropertyDescription("JSON array of strings representing missing skills, e.g. [\"Docker\", \"Kubernetes\"]")
    List<String> missingSkills,
    
    @JsonPropertyDescription("JSON array of strings representing recommendation steps for HR, e.g. [\"Conduct technical interview\"]")
    List<String> recommendations
) {}