package com.akshay.expense_splitter.group;

import com.akshay.expense_splitter.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "group_members")
@Getter
@Setter
@NoArgsConstructor
public class GroupMembers {

    @EmbeddedId
    private GroupMemberId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("groupId")
    @JoinColumn(name = "group_id",nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @Column(name = "joined_at",nullable = false,updatable = false)
    private OffsetDateTime joinedAt;

    @PrePersist
    protected void onCreate(){
        if(joinedAt == null){
            joinedAt = OffsetDateTime.now();
        }
    }

}
