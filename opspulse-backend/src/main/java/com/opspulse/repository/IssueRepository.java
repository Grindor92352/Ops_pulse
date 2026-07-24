package com.opspulse.repository;

import com.opspulse.entity.Issue;
import com.opspulse.entity.IssueSeverity;
import com.opspulse.entity.IssueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, String> {
    Page<Issue> findByProjectId(String projectId, Pageable pageable);
    List<Issue> findByProjectIdAndStatus(String projectId, IssueStatus status);
    List<Issue> findByProjectIdAndSeverity(String projectId, IssueSeverity severity);
    long countByProjectId(String projectId);
}
