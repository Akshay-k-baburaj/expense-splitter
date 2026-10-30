package com.akshay.expense_splitter.expense;

import com.akshay.expense_splitter.group.*;
import com.akshay.expense_splitter.user.User;
import com.akshay.expense_splitter.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GroupFinancialServiceTest {
    @Mock GroupRepository groupRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock ExpenseRepository expenseRepository;
    @Mock SettlementRepository settlementRepository;
    @Mock UserService userService;
    @Mock EqualExpenseSplitCalculator splitCalculator;
    @InjectMocks GroupFinancialService financialService;

    private final Jwt actorToken = Jwt.withTokenValue("actor-token").header("alg", "none").subject("auth0|one").build();
    private final User first = user(1L, "first");
    private final User second = user(2L, "second");
    private final User third = user(3L, "third");
    private Group group;

    @BeforeEach
    void setup() {
        group = new Group();
        group.setId(11L);
        group.setGroupName("Weekend");
        when(userService.requireCurrentUser(actorToken)).thenReturn(first);
        when(groupRepository.findById(11L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsById(new GroupMemberId(11L, 1L))).thenReturn(true);
    }

    @Test
    void balancesIncludePaidExpensesSplitsAndSettlements() {
        Expense expense = new Expense();
        expense.setGroup(group);
        expense.setPayer(first);
        expense.setAmount(new BigDecimal("10.00"));
        ExpenseSplit firstSplit = split(expense, first, "3.34");
        ExpenseSplit secondSplit = split(expense, second, "3.33");
        ExpenseSplit thirdSplit = split(expense, third, "3.33");
        expense.setSplits(List.of(firstSplit, secondSplit, thirdSplit));
        Settlement settlement = new Settlement();
        settlement.setGroup(group);
        settlement.setPayer(second);
        settlement.setReceiver(first);
        settlement.setAmount(new BigDecimal("1.00"));
        when(groupMemberRepository.findByGroup_Id(11L)).thenReturn(List.of(membership(first), membership(second), membership(third)));
        when(expenseRepository.findByGroup_IdOrderByCreatedAtDesc(11L)).thenReturn(List.of(expense));
        when(settlementRepository.findByGroup_IdOrderByCreatedAtDesc(11L)).thenReturn(List.of(settlement));

        GroupBalanceResponse result = financialService.getBalances(11L, actorToken);

        assertEquals(new BigDecimal("5.66"), result.members().get(0).netBalance());
        assertEquals(new BigDecimal("-2.33"), result.members().get(1).netBalance());
        assertEquals(new BigDecimal("-3.33"), result.members().get(2).netBalance());
        assertEquals("INR", result.currency());
        assertEquals(2, result.suggestedSettlements().size());
    }

    @Test
    void settlementPlanMatchesLargestBalancesFirstToReducePartialTransfers() {
        User fourth = user(4L, "fourth");
        User fifth = user(5L, "fifth");
        User sixth = user(6L, "sixth");
        Expense firstExpense = expense(fifth, "5.00", new User[]{first, fourth}, new String[]{"1.00", "4.00"});
        Expense secondExpense = expense(sixth, "5.00", new User[]{second, third}, new String[]{"2.00", "3.00"});
        when(groupMemberRepository.findByGroup_Id(11L)).thenReturn(List.of(
                membership(first), membership(second), membership(third), membership(fourth), membership(fifth), membership(sixth)));
        when(expenseRepository.findByGroup_IdOrderByCreatedAtDesc(11L)).thenReturn(List.of(firstExpense, secondExpense));
        when(settlementRepository.findByGroup_IdOrderByCreatedAtDesc(11L)).thenReturn(List.of());

        GroupBalanceResponse result = financialService.getBalances(11L, actorToken);

        assertEquals(4, result.suggestedSettlements().size());
        assertEquals(4L, result.suggestedSettlements().get(0).fromUserId());
        assertEquals(5L, result.suggestedSettlements().get(0).toUserId());
        assertEquals(new BigDecimal("4.00"), result.suggestedSettlements().get(0).amount());
        assertEquals(new BigDecimal("10.00"), result.suggestedSettlements().stream()
                .map(SettlementSuggestionResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void rejectsExpenseReadsForNonMembers() {
        when(groupMemberRepository.existsById(new GroupMemberId(11L, 1L))).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> financialService.getExpenses(11L, actorToken));
    }

    @Test
    void createsCustomExpenseWithExactParticipantAmountsInGroupCurrency() {
        group.setCurrencyCode("USD");
        when(groupMemberRepository.findByGroup_Id(11L)).thenReturn(List.of(membership(first), membership(second)));
        when(expenseRepository.saveAndFlush(any(Expense.class))).thenAnswer(invocation -> {
            Expense saved = invocation.getArgument(0);
            saved.setId(41L);
            saved.setCreatedAt(OffsetDateTime.now());
            return saved;
        });
        ExpenseRequest request = new ExpenseRequest("Dinner", new BigDecimal("10.00"), List.of(2L, 1L),
                Map.of(1L, new BigDecimal("7.25"), 2L, new BigDecimal("2.75")));

        ExpenseResponse response = financialService.createExpense(11L, request, actorToken);

        assertEquals("USD", response.currency());
        assertEquals(ExpenseSplitType.CUSTOM, response.splitType());
        assertEquals(new BigDecimal("7.25"), response.splits().get(0).amount());
        assertEquals(new BigDecimal("2.75"), response.splits().get(1).amount());
    }

    @Test
    void rejectsCustomSharesThatDoNotEqualTheExpenseTotal() {
        when(groupMemberRepository.findByGroup_Id(11L)).thenReturn(List.of(membership(first), membership(second)));
        ExpenseRequest request = new ExpenseRequest("Dinner", new BigDecimal("10.00"), List.of(1L, 2L),
                Map.of(1L, new BigDecimal("7.00"), 2L, new BigDecimal("2.00")));

        assertThrows(com.akshay.expense_splitter.exception.FinancialOperationException.class,
                () -> financialService.createExpense(11L, request, actorToken));
        verify(expenseRepository, never()).saveAndFlush(any(Expense.class));
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id); user.setUsername(username); user.setEmail(username + "@example.com"); user.setActive(true);
        return user;
    }

    private GroupMembers membership(User user) {
        GroupMembers membership = new GroupMembers();
        membership.setGroup(group); membership.setUser(user); membership.setJoinedAt(OffsetDateTime.now());
        return membership;
    }

    private ExpenseSplit split(Expense expense, User user, String amount) {
        ExpenseSplit split = new ExpenseSplit();
        split.setExpense(expense); split.setUser(user); split.setAmount(new BigDecimal(amount));
        return split;
    }

    private Expense expense(User payer, String amount, User[] participants, String[] shares) {
        Expense expense = new Expense();
        expense.setGroup(group); expense.setPayer(payer); expense.setAmount(new BigDecimal(amount));
        List<ExpenseSplit> splits = new java.util.ArrayList<>();
        for (int index = 0; index < participants.length; index++) splits.add(split(expense, participants[index], shares[index]));
        expense.setSplits(splits);
        return expense;
    }
}
