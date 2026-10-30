package com.akshay.expense_splitter.expense;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByGroup_IdOrderByCreatedAtDesc(Long groupId);
    Optional<Expense> findByIdAndGroup_Id(Long id, Long groupId);
}
