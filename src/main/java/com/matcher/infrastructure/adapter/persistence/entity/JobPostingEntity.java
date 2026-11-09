

package com.matcher.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_postings")
@Getter
@Setter
public class JobPostingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String department;

    @Column(name = "min_years_experience", nullable = false)
    private Integer minYearsExperience = 0;

    @Column(name = "required_education")
    private String requiredEducation;

    @Column(name = "required_skills", nullable = false)
    private List<String> requiredSkills;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}