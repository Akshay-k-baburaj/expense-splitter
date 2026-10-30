package com.akshay.expense_splitter.group;

import java.time.OffsetDateTime;

public record GroupInviteCodeResponse(String code, OffsetDateTime expiresAt) { }
