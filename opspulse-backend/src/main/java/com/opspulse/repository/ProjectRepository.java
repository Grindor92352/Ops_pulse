package com.opspulse.repository;

import com.opspulse.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    Optional<Project> findBySdkApiKey(String sdkApiKey);
    List<Project> findByOrganizationId(String orgId);
}
