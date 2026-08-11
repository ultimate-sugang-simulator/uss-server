package uss.code.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PRIVATE;
import static uss.code.admin.domain.AdminRole.ADMIN;

@Getter
@Entity
@NoArgsConstructor
@Table(
        name = "admins",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"login_id"})
        }
)
public class Admin {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "login_id")
    private String loginId;

    @Column(nullable = false, name = "password")
    private String password;

    @Column(nullable = false, name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "role")
    private AdminRole role;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Builder(access = PRIVATE)
    private Admin(
            final String loginId,
            final String password,
            final String name
    ) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.role = ADMIN;
        this.createdAt = LocalDateTime.now();
    }

    public static Admin create(
            final String loginId,
            final String encodedPassword,
            final String name
    ) {
        return Admin.builder()
                .loginId(loginId)
                .password(encodedPassword)
                .name(name)
                .build();
    }

    public boolean isAdmin() {
        return role == ADMIN;
    }
}
