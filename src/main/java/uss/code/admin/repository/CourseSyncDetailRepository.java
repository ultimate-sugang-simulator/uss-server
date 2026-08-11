package uss.code.admin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import uss.code.admin.domain.CourseSyncDetail;
import uss.code.admin.domain.SyncChangeType;

public interface CourseSyncDetailRepository extends JpaRepository<CourseSyncDetail, Long> {
    Page<CourseSyncDetail> findByJobIdAndChangeTypeOrderByHaksuCodeAsc(
            final long jobId,
            final SyncChangeType changeType,
            final Pageable pageable
    );
}
