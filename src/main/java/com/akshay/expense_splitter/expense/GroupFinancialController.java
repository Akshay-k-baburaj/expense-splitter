package com.akshay.expense_splitter.expense;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups/{groupId}")
public class GroupFinancialController {
    private final GroupFinancialService financialService;

    public GroupFinancialController(GroupFinancialService financialService) {
        this.financialService = financialService;
    }

    @GetMapping("/expenses")
    public ResponseEntity<List<ExpenseResponse>> getExpenses(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(financialService.getExpenses(groupId, principal));
    }

    @PostMapping("/expenses")
    public ResponseEntity<ExpenseResponse> createExpense(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financialService.createExpense(groupId, request, principal));
    }

    @PutMapping("/expenses/{expenseId}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long groupId,
            @PathVariable Long expenseId,
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(financialService.updateExpense(groupId, expenseId, request, principal));
    }

    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable Long groupId,
            @PathVariable Long expenseId,
            @AuthenticationPrincipal Jwt principal) {
        financialService.deleteExpense(groupId, expenseId, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/balances")
    public ResponseEntity<GroupBalanceResponse> getBalances(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(financialService.getBalances(groupId, principal));
    }

    @GetMapping("/settlements")
    public ResponseEntity<List<SettlementResponse>> getSettlements(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(financialService.getSettlements(groupId, principal));
    }

    @PostMapping("/settlements")
    public ResponseEntity<SettlementResponse> createSettlement(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody SettlementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financialService.createSettlement(groupId, request, principal));
    }

    @DeleteMapping("/settlements/{settlementId}")
    public ResponseEntity<Void> deleteSettlement(
            @PathVariable Long groupId,
            @PathVariable Long settlementId,
            @AuthenticationPrincipal Jwt principal) {
        financialService.deleteSettlement(groupId, settlementId, principal);
        return ResponseEntity.noContent().build();
    }
}
