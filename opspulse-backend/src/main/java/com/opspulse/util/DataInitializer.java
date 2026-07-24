package com.opspulse.util;

import com.opspulse.entity.*;
import com.opspulse.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final IssueRepository issueRepository;
    private final IssueActivityRepository activityRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            logger.info("Database already initialized with existing data. Skipping seed.");
            return;
        }

        logger.info("Initializing database with demo organization, users, projects, and incidents...");

        Organization org = Organization.builder()
                .name("OpsPulse Enterprise")
                .plan(PlanType.ENTERPRISE)
                .build();
        org = organizationRepository.save(org);

        User admin = User.builder()
                .email("admin@opspulse.io")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .name("System Admin")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(org)
                .build();
        admin = userRepository.save(admin);

        User manager = User.builder()
                .email("manager@opspulse.io")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .name("Engineering Manager")
                .role(Role.MANAGER)
                .status(UserStatus.ACTIVE)
                .organization(org)
                .build();
        manager = userRepository.save(manager);

        User dev = User.builder()
                .email("dev@opspulse.io")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .name("Senior Developer")
                .role(Role.DEVELOPER)
                .status(UserStatus.ACTIVE)
                .organization(org)
                .build();
        dev = userRepository.save(dev);

        Project project = Project.builder()
                .name("Payment Gateway Microservice")
                .description("Core transaction processing engine and checkout API")
                .techStack(List.of("Java 21", "Spring Boot 3", "PostgreSQL", "Redis"))
                .plan(PlanType.ENTERPRISE)
                .sdkApiKey("sdk_demo_key_12345")
                .organization(org)
                .creator(admin)
                .build();
        project = projectRepository.save(project);

        Team backendTeam = Team.builder()
                .name("Backend Core Team")
                .project(project)
                .build();
        backendTeam = teamRepository.save(backendTeam);

        Issue issue = Issue.builder()
                .title("NullPointerException in PaymentCallbackHandler")
                .description("Transaction status callback failed due to unhandled null reference during token verification.")
                .source(IssueSource.SDK)
                .severity(IssueSeverity.CRITICAL)
                .priority(IssuePriority.URGENT)
                .status(IssueStatus.OPEN)
                .environment(EnvironmentType.PRODUCTION)
                .project(project)
                .team(backendTeam)
                .assignedTo(dev)
                .rootCause("PaymentCallbackHandler.java line 87 accessed callbackToken without null verification.")
                .suggestedFixes("Add Optional check: Optional.ofNullable(token).ifPresentOrElse(...) before executing callback processing.")
                .responseSlaDeadline(LocalDateTime.now().plusHours(1))
                .resolutionSlaDeadline(LocalDateTime.now().plusHours(4))
                .build();
        issue = issueRepository.save(issue);

        IssueActivity activity = IssueActivity.builder()
                .issue(issue)
                .user(admin)
                .action("Demo incident seeded into OpsPulse system.")
                .build();
        activityRepository.save(activity);

        logger.info("------------------------------------------------------------------");
        logger.info("OpsPulse Initialized Successfully!");
        logger.info("Admin Credentials:      admin@opspulse.io / Password123!");
        logger.info("Manager Credentials:    manager@opspulse.io / Password123!");
        logger.info("Developer Credentials:  dev@opspulse.io / Password123!");
        logger.info("Demo Project SDK API Key: sdk_demo_key_12345");
        logger.info("------------------------------------------------------------------");
    }
}
