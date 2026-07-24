package com.opspulse.repository;

import com.opspulse.entity.IssueActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueActivityRepository extends JpaRepository<IssueActivity, String> {
    List<IssueActivity> findByIssueIdOrderByCreatedAtDesc(String issueId);
}
