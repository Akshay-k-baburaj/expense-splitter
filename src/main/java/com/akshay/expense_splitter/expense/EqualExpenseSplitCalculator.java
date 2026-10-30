package com.akshay.expense_splitter.expense;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class EqualExpenseSplitCalculator {
    public Map<Long, BigDecimal> allocate(BigDecimal amount, List<Long> participantIds) {
        return allocate(amount, participantIds, 2);
    }

    public Map<Long, BigDecimal> allocate(BigDecimal amount, List<Long> participantIds, int scale) {
        if (participantIds.isEmpty()) throw new IllegalArgumentException("At least one participant is required.");
        long totalUnits = amount.setScale(scale, RoundingMode.UNNECESSARY).movePointRight(scale).longValueExact();
        long baseUnits = totalUnits / participantIds.size();
        long remainderUnits = totalUnits % participantIds.size();
        Map<Long, BigDecimal> allocations = new LinkedHashMap<>();
        List<Long> stableOrder = participantIds.stream().sorted().toList();
        for (int index = 0; index < stableOrder.size(); index++) {
            long units = baseUnits + (index < remainderUnits ? 1 : 0);
            allocations.put(stableOrder.get(index), BigDecimal.valueOf(units, scale));
        }
        return allocations;
    }
}
