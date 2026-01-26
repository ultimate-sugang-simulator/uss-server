package uss.code.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uss.code.member.domain.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);
}
