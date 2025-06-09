package com.example.LoanAPIBackend.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum AllowedInstallmentCounts {
    SIX(6),
    NINE(9),
    TWELVE(12),
    TWENTY_FOUR(24);

    private final int count;

    AllowedInstallmentCounts(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    private static final Set<Integer> VALID_COUNTS = Arrays.stream(values())
            .map(AllowedInstallmentCounts::getCount)
            .collect(Collectors.toSet());

    public static boolean isValid(int number) {
        return VALID_COUNTS.contains(number);
    }
}
