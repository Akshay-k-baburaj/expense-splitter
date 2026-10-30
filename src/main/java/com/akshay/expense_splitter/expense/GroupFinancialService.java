package com.akshay.expense_splitter.expense;

import com.akshay.expense_splitter.exception.ExpenseNotFoundException;
import com.akshay.expense_splitter.exception.FinancialOperationException;
import com.akshay.expense_splitter.exception.GroupNotFoundException;
import com.akshay.expense_splitter.group.Group;
import com.akshay.expense_splitter.group.GroupMemberId;
import com.akshay.expense_splitter.group.GroupMemberRepository;
import com.akshay.expense_splitter.group.GroupMembers;
import com.akshay.expense_splitter.group.GroupRepository;
import com.akshay.expense_splitter.user.User;
import com.akshay.expense_splitter.user.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GroupFinancialService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final UserService userService;
    private final EqualExpenseSplitCalculator splitCalculator;

    public GroupFinancialService(
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            ExpenseRepository expenseRepository,
            SettlementRepository settlementRepository,
            UserService userService,
            EqualExpenseSplitCalculator splitCalculator) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.userService = userService;
        this.splitCalculator = splitCalculator;
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpenses(Long groupId, Jwt principal) {
        requireMember(groupId, principal);
        return expenseRepository.findByGroup_IdOrderByCreatedAtDesc(groupId).stream().map(this::toExpenseResponse).toList();
    }

    @Transactional
    public ExpenseResponse createExpense(Long groupId, ExpenseRequest request, Jwt principal) {
        User actor = requireMember(groupId, principal);
        Group group = requireGroup(groupId);
        List<Long> participantIds = request.participantUserIds().stream().distinct().sorted().toList();
        if (participantIds.size() != request.participantUserIds().size()) {
            throw new FinancialOperationException("Select each participant only once.");
        }
        Map<Long, User> participants = activeGroupMembers(groupId).stream()
                .map(GroupMembers::getUser)
                .filter(User::isActive)
                .collect(Collectors.toMap(User::getId, Function.identity()));
        if (!participants.keySet().containsAll(participantIds)) {
            throw new FinancialOperationException("Every expense participant must be an active member of this group.");
        }

        Expense expense = new Expense();
        expense.setGroup(group);
        expense.setPayer(actor);
        expense.setDescription(request.description().trim());
        expense.setAmount(CurrencySupport.amount(request.amount(), group.getCurrencyCode(), "Expense amount"));
        setExpenseSplits(expense, participantIds, participants, allocationsFor(request, participantIds, group.getCurrencyCode(), expense));
        return toExpenseResponse(expenseRepository.saveAndFlush(expense));
    }

    @Transactional
    public ExpenseResponse updateExpense(Long groupId, Long expenseId, ExpenseRequest request, Jwt principal) {
        User actor = requireMember(groupId, principal);
        Expense expense = expenseRepository.findByIdAndGroup_Id(expenseId, groupId).orElseThrow(ExpenseNotFoundException::new);
        if (!expense.getPayer().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Only the person who added an expense can edit it.");
        }
        Group group = expense.getGroup();
        List<Long> participantIds = request.participantUserIds().stream().distinct().sorted().toList();
        if (participantIds.size() != request.participantUserIds().size()) {
            throw new FinancialOperationException("Select each participant only once.");
        }
        Map<Long, User> participants = activeGroupMembers(groupId).stream()
                .map(GroupMembers::getUser)
                .filter(User::isActive)
                .collect(Collectors.toMap(User::getId, Function.identity()));
        if (!participants.keySet().containsAll(participantIds)) {
            throw new FinancialOperationException("Every expense participant must be an active member of this group.");
        }
        expense.setDescription(request.description().trim());
        expense.setAmount(CurrencySupport.amount(request.amount(), group.getCurrencyCode(), "Expense amount"));
        expense.getSplits().clear();
        setExpenseSplits(expense, participantIds, participants, allocationsFor(request, participantIds, group.getCurrencyCode(), expense));
        return toExpenseResponse(expenseRepository.saveAndFlush(expense));
    }

    @Transactional
    public void deleteExpense(Long groupId, Long expenseId, Jwt principal) {
        User actor = requireMember(groupId, principal);
        Expense expense = expenseRepository.findByIdAndGroup_Id(expenseId, groupId).orElseThrow(ExpenseNotFoundException::new);
        if (!expense.getPayer().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Only the person who added an expense can delete it.");
        }
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public GroupBalanceResponse getBalances(Long groupId, Jwt principal) {
        User viewer = requireMember(groupId, principal);
        return calculateBalances(groupId, viewer);
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> getSettlements(Long groupId, Jwt principal) {
        requireMember(groupId, principal);
        return settlementRepository.findByGroup_IdOrderByCreatedAtDesc(groupId).stream().map(this::toSettlementResponse).toList();
    }

    @Transactional
    public SettlementResponse createSettlement(Long groupId, SettlementRequest request, Jwt principal) {
        User actor = requireMember(groupId, principal);
        Group group = requireGroup(groupId);
        User receiver = activeGroupMembers(groupId).stream().map(GroupMembers::getUser)
                .filter(user -> user.isActive() && user.getId().equals(request.receiverId()))
                .findFirst().orElseThrow(() -> new FinancialOperationException("Choose an active member of this group."));
        if (actor.getId().equals(receiver.getId())) {
            throw new FinancialOperationException("You cannot record a payment to yourself.");
        }

        GroupBalanceResponse balances = calculateBalances(groupId, actor);
        Map<Long, BigDecimal> netBalances = balances.members().stream()
                .collect(Collectors.toMap(MemberBalanceResponse::userId, MemberBalanceResponse::netBalance));
        BigDecimal actorBalance = netBalances.getOrDefault(actor.getId(), ZERO);
        BigDecimal receiverBalance = netBalances.getOrDefault(receiver.getId(), ZERO);
        BigDecimal amount = CurrencySupport.amount(request.amount(), group.getCurrencyCode(), "Payment amount");
        if (actorBalance.signum() >= 0 || receiverBalance.signum() <= 0
                || amount.compareTo(actorBalance.negate()) > 0
                || amount.compareTo(receiverBalance) > 0) {
            throw new FinancialOperationException("The payment must be between a member you owe and an amount that settles part of the current balance.");
        }

        Settlement settlement = new Settlement();
        settlement.setGroup(group);
        settlement.setPayer(actor);
        settlement.setReceiver(receiver);
        settlement.setAmount(amount);
        return toSettlementResponse(settlementRepository.saveAndFlush(settlement));
    }

    @Transactional
    public void deleteSettlement(Long groupId, Long settlementId, Jwt principal) {
        User actor = requireMember(groupId, principal);
        Settlement settlement = settlementRepository.findByIdAndGroup_Id(settlementId, groupId)
                .orElseThrow(() -> new FinancialOperationException("Settlement not found."));
        if (!settlement.getPayer().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Only the person who recorded this payment can remove it.");
        }
        settlementRepository.delete(settlement);
    }

    private GroupBalanceResponse calculateBalances(Long groupId, User viewer) {
        List<GroupMembers> currentMemberships = activeGroupMembers(groupId);
        Set<Long> currentMemberIds = currentMemberships.stream().map(member -> member.getUser().getId()).collect(Collectors.toSet());
        Map<Long, User> users = new HashMap<>();
        Map<Long, BigDecimal> balances = new HashMap<>();
        currentMemberships.forEach(member -> addUser(users, balances, member.getUser()));

        List<Expense> expenses = expenseRepository.findByGroup_IdOrderByCreatedAtDesc(groupId);
        for (Expense expense : expenses) {
            addUser(users, balances, expense.getPayer());
            balances.compute(expense.getPayer().getId(), (id, balance) -> balance.add(expense.getAmount()));
            for (ExpenseSplit split : expense.getSplits()) {
                addUser(users, balances, split.getUser());
                balances.compute(split.getUser().getId(), (id, balance) -> balance.subtract(split.getAmount()));
            }
        }

        List<Settlement> settlements = settlementRepository.findByGroup_IdOrderByCreatedAtDesc(groupId);
        for (Settlement settlement : settlements) {
            addUser(users, balances, settlement.getPayer());
            addUser(users, balances, settlement.getReceiver());
            balances.compute(settlement.getPayer().getId(), (id, balance) -> balance.add(settlement.getAmount()));
            balances.compute(settlement.getReceiver().getId(), (id, balance) -> balance.subtract(settlement.getAmount()));
        }

        Map<Long, String> names = users.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> displayUsername(entry.getValue(), currentMemberIds.contains(entry.getKey()))));
        String currencyCode = requireGroup(groupId).getCurrencyCode();
        int scale = CurrencySupport.fractionDigits(currencyCode);
        List<MemberBalanceResponse> memberBalances = balances.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new MemberBalanceResponse(entry.getKey(), names.get(entry.getKey()), entry.getValue().setScale(scale)))
                .toList();
        return new GroupBalanceResponse(groupId, viewer.getId(), currencyCode, memberBalances, suggestSettlements(memberBalances));
    }

    private List<SettlementSuggestionResponse> suggestSettlements(List<MemberBalanceResponse> memberBalances) {
        List<BalanceCursor> debtors = memberBalances.stream().filter(member -> member.netBalance().signum() < 0)
                .map(member -> new BalanceCursor(member.userId(), member.username(), member.netBalance().negate()))
                .sorted(Comparator.comparing(BalanceCursor::userId)).collect(Collectors.toCollection(ArrayList::new));
        List<BalanceCursor> creditors = memberBalances.stream().filter(member -> member.netBalance().signum() > 0)
                .map(member -> new BalanceCursor(member.userId(), member.username(), member.netBalance()))
                .sorted(Comparator.comparing(BalanceCursor::userId)).collect(Collectors.toCollection(ArrayList::new));
        List<SettlementSuggestionResponse> suggestions = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            BalanceCursor debtor = debtors.stream()
                    .max(Comparator.<BalanceCursor, BigDecimal>comparing(BalanceCursor::remaining)
                            .thenComparing(cursor -> -cursor.userId()))
                    .orElseThrow();
            BalanceCursor creditor = creditors.stream()
                    .max(Comparator.<BalanceCursor, BigDecimal>comparing(BalanceCursor::remaining)
                            .thenComparing(cursor -> -cursor.userId()))
                    .orElseThrow();
            BigDecimal amount = debtor.remaining().min(creditor.remaining());
            suggestions.add(new SettlementSuggestionResponse(debtor.userId(), debtor.username(), creditor.userId(), creditor.username(), amount));
            debtor.remaining(debtor.remaining().subtract(amount));
            creditor.remaining(creditor.remaining().subtract(amount));
            if (debtor.remaining().signum() == 0) debtors.remove(debtor);
            if (creditor.remaining().signum() == 0) creditors.remove(creditor);
        }
        return suggestions;
    }

    private List<GroupMembers> activeGroupMembers(Long groupId) {
        return groupMemberRepository.findByGroup_Id(groupId);
    }

    private Map<Long, BigDecimal> allocationsFor(ExpenseRequest request, List<Long> participantIds, String currencyCode, Expense expense) {
        int scale = CurrencySupport.fractionDigits(currencyCode);
        if (request.participantAmounts() == null) {
            expense.setSplitType(ExpenseSplitType.EQUAL);
            return splitCalculator.allocate(expense.getAmount(), participantIds, scale);
        }
        if (!request.participantAmounts().keySet().equals(new HashSet<>(participantIds))) {
            throw new FinancialOperationException("Enter one share amount for every selected participant.");
        }
        Map<Long, BigDecimal> allocations = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Long userId : participantIds) {
            BigDecimal requestedShare = request.participantAmounts().get(userId);
            if (requestedShare == null || requestedShare.signum() <= 0) {
                throw new FinancialOperationException("Each participant's share must be greater than zero.");
            }
            BigDecimal share = CurrencySupport.amount(requestedShare, currencyCode, "Each share");
            allocations.put(userId, share);
            total = total.add(share);
        }
        if (total.compareTo(expense.getAmount()) != 0) {
            throw new FinancialOperationException("Participant shares must add up to the expense amount.");
        }
        expense.setSplitType(ExpenseSplitType.CUSTOM);
        return allocations;
    }

    private void setExpenseSplits(Expense expense, List<Long> participantIds, Map<Long, User> participants, Map<Long, BigDecimal> allocations) {
        allocations.forEach((userId, amount) -> {
            ExpenseSplit split = new ExpenseSplit();
            split.setExpense(expense);
            split.setUser(participants.get(userId));
            split.setAmount(amount);
            expense.getSplits().add(split);
        });
    }

    private User requireMember(Long groupId, Jwt principal) {
        User actor = userService.requireCurrentUser(principal);
        Group group = requireGroup(groupId);
        if (!groupMemberRepository.existsById(new GroupMemberId(group.getId(), actor.getId()))) {
            throw new AccessDeniedException("Group membership is required for this action.");
        }
        return actor;
    }

    private Group requireGroup(Long groupId) {
        return groupRepository.findById(groupId).orElseThrow(() -> new GroupNotFoundException("Group not found"));
    }

    private void addUser(Map<Long, User> users, Map<Long, BigDecimal> balances, User user) {
        users.putIfAbsent(user.getId(), user);
        balances.putIfAbsent(user.getId(), ZERO);
    }

    private String displayUsername(User user, boolean isCurrentMember) {
        return user.isActive() && isCurrentMember ? user.getUsername() : "Former member";
    }

    private ExpenseResponse toExpenseResponse(Expense expense) {
        List<ExpenseSplitResponse> splits = expense.getSplits().stream()
                .sorted(Comparator.comparing(split -> split.getUser().getId()))
                .map(split -> new ExpenseSplitResponse(split.getUser().getId(),
                        displayUsername(split.getUser(), isCurrentMember(expense.getGroup().getId(), split.getUser().getId())),
                        split.getAmount()))
                .toList();
        return new ExpenseResponse(expense.getId(), expense.getGroup().getId(), expense.getDescription(), expense.getAmount(), expense.getGroup().getCurrencyCode(),
                expense.getPayer().getId(), displayUsername(expense.getPayer(), isCurrentMember(expense.getGroup().getId(), expense.getPayer().getId())),
                expense.getCreatedAt(), splits, expense.getSplitType());
    }

    private boolean isCurrentMember(Long groupId, Long userId) {
        return groupMemberRepository.existsById(new GroupMemberId(groupId, userId));
    }

    private SettlementResponse toSettlementResponse(Settlement settlement) {
        return new SettlementResponse(settlement.getId(), settlement.getGroup().getId(), settlement.getPayer().getId(),
                displayUsername(settlement.getPayer(), isCurrentMember(settlement.getGroup().getId(), settlement.getPayer().getId())),
                settlement.getReceiver().getId(), displayUsername(settlement.getReceiver(), isCurrentMember(settlement.getGroup().getId(), settlement.getReceiver().getId())),
                settlement.getAmount(), settlement.getGroup().getCurrencyCode(), settlement.getCreatedAt());
    }

    private static final class BalanceCursor {
        private final Long userId;
        private final String username;
        private BigDecimal remaining;

        private BalanceCursor(Long userId, String username, BigDecimal remaining) {
            this.userId = userId;
            this.username = username;
            this.remaining = remaining;
        }

        Long userId() { return userId; }
        String username() { return username; }
        BigDecimal remaining() { return remaining; }
        void remaining(BigDecimal value) { remaining = value; }
    }
}
