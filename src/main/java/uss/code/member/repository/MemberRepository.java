package uss.code.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uss.code.member.domain.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByStudentId(final String studentId);
    boolean existsByStudentId(final String studentId);

    boolean existsByEmail(String email);
}
