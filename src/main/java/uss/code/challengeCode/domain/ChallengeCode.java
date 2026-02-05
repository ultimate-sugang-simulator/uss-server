package uss.code.challengeCode.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uss.code.member.domain.Member;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "challenge_codes")
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
