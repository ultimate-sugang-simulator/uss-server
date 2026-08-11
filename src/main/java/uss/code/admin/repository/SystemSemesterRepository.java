package uss.code.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uss.code.admin.domain.SystemSemester;

import java.util.List;

public interface SystemSemesterRepository extends JpaRepository<SystemSemester, Long> {
    @Query("""
        SELECT s
        FROM SystemSemester s
        ORDER BY s.id
    """)
    List<SystemSemester> findAllOrdered();
}
