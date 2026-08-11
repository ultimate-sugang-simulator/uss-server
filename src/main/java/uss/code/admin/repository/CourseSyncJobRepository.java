package uss.code.admin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uss.code.admin.domain.CourseSyncJob;
import uss.code.admin.domain.SyncJobStatus;

import java.util.Optional;

public interface CourseSyncJobRepository extends JpaRepository<CourseSyncJob, Long> {
    Optional<CourseSyncJob> findFirstByStatus(final SyncJobStatus status);

    Optional<CourseSyncJob> findFirstByOrderByStartedAtDesc();

    Page<CourseSyncJob> findAllByOrderByStartedAtDesc(final Pageable pageable);

    @Query("""
        SELECT j
        FROM CourseSyncJob j
        JOIN FETCH j.executedBy
        WHERE j.id = :id
    """)
    Optional<CourseSyncJob> findByIdWithAdmin(@Param("id") final long id);
}
