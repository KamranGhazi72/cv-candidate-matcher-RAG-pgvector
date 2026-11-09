

package com.matcher.domain.model;

import java.util.List;
import java.util.UUID;

public record JobPosting(
    UUID id,
    String title,
    String department,
    Integer minYearsExperience,
    String requiredEducation,
    List<String> requiredSkills,
    String description
) {}
