

package com.matcher.infrastructure.adapter.persistence.repository;

import com.matcher.infrastructure.adapter.persistence.entity.CandidateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataCandidateRepository extends JpaRepository<CandidateEntity, UUID> {
}