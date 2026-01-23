package uss.code.challengeCode.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uss.code.member.domain.Member;

@Getter
@Entity
@Table(name = "challenge_codes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeCode {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn( nullable = false, name = "member_id")
    private Member member;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false, name = "failure_count")
    private int failureCount;
}
