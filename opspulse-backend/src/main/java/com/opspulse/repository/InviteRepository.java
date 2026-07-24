package com.opspulse.repository;

import com.opspulse.entity.Invite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InviteRepository extends JpaRepository<Invite, String> {
    Optional<Invite> findByToken(String token);
}
