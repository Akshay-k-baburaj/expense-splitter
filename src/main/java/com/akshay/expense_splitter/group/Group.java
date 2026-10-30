package com.akshay.expense_splitter.group;

import com.akshay.expense_splitter.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_name",nullable = false,length = 100)
    private String groupName;

    @Column(name = "description")
    private String description;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "INR";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "created_at",nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "invite_code_hash", length = 64)
    private String inviteCodeHash;

    @Column(name = "invite_expires_at")
    private OffsetDateTime inviteExpiresAt;

    @PrePersist
    protected void onCreate(){
        this.createdAt = OffsetDateTime.now();
    }
}
