package uss.code.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uss.code.admin.domain.Admin;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByLoginId(final String loginId);
}
