package com.akshay.expense_splitter.group;

import com.akshay.expense_splitter.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "group_join_attempts")
@Getter
@Setter
@NoArgsConstructor
public class GroupJoinAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private OffsetDateTime attemptedAt;

    @PrePersist
    protected void onCreate() {
        if (attemptedAt == null) attemptedAt = OffsetDateTime.now();
    }
}
