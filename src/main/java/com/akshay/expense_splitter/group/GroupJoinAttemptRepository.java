package com.akshay.expense_splitter.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;

public interface GroupJoinAttemptRepository extends JpaRepository<GroupJoinAttempt, Long> {
    long countByUser_IdAndAttemptedAtAfter(Long userId, OffsetDateTime cutoff);
    long deleteByAttemptedAtBefore(OffsetDateTime cutoff);
}
