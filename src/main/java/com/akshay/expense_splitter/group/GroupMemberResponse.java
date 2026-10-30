package com.akshay.expense_splitter.group;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponse {
    private Long groupId;
    private Long userId;
    private String username;
    private OffsetDateTime joinedAt;
    private boolean owner;
}
