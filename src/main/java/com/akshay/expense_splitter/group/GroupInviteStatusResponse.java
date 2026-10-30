package com.akshay.expense_splitter.group;

import java.time.OffsetDateTime;

public record GroupInviteStatusResponse(boolean active, OffsetDateTime expiresAt) { }
