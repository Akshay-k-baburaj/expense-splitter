package com.akshay.expense_splitter.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group,Long> {
    Optional<Group> findByInviteCodeHashAndInviteExpiresAtAfter(String inviteCodeHash, OffsetDateTime now);
    boolean existsByInviteCodeHash(String inviteCodeHash);
}
