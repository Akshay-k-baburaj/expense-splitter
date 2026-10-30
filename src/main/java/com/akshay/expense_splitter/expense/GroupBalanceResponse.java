package com.akshay.expense_splitter.expense;

import java.util.List;

public record GroupBalanceResponse(
        Long groupId,
        Long currentUserId,
        String currency,
        List<MemberBalanceResponse> members,
        List<SettlementSuggestionResponse> suggestedSettlements) { }
