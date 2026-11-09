

package com.matcher.domain.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Candidate(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    Integer yearsOfExperience,
    String educationLevel,
    List<String> skills,
    String resumeFilePath,
    OffsetDateTime createdAt
) {}
