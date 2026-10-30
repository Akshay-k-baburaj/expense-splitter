package com.akshay.expense_splitter.expense;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByGroup_IdOrderByCreatedAtDesc(Long groupId);
    Optional<Settlement> findByIdAndGroup_Id(Long id, Long groupId);
}
