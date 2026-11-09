package com.matcher.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "candidates")
@Getter
@Setter
@NoArgsConstructor
public class CandidateEntity {

    @Id
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience = 0;

    @Column(name = "education_level", length = 100)
    private String educationLevel;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "skills")
    private List<String> skills;

    @Column(name = "resume_file_path", length = 512)
    private String resumeFilePath;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public CandidateEntity(UUID id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }
}