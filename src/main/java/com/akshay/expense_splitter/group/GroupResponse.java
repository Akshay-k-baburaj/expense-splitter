package com.akshay.expense_splitter.group;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {
    private Long id;
    private String groupName;
    private String description;
    private String currencyCode;
    private Long ownerId;
    private boolean viewerIsOwner;

    public GroupResponse(Long id, String groupName, String description, Long ownerId, boolean viewerIsOwner) {
        this(id, groupName, description, "INR", ownerId, viewerIsOwner);
    }
}
