package com.akshay.expense_splitter.group;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class GroupMemberResponse {
    private Long groupId;
    private Long userId;
    private OffsetDateTime joinedAt;
}
