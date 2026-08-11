package uss.code.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.admin.domain.SystemSemester;
import uss.code.admin.dto.request.SystemSemesterRequest;
import uss.code.admin.dto.response.SystemSemesterResponse;
import uss.code.admin.repository.SystemSemesterRepository;
import uss.code.global.exception.domain.RestApiException;

import java.util.List;

import static uss.code.global.exception.domain.ExceptionCode.SYSTEM_SEMESTER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class SystemSemesterService {

    private final SystemSemesterRepository systemSemesterRepository;

    @Transactional(readOnly = true)
    public SystemSemesterResponse getSystemSemester() {
        return SystemSemesterResponse.from(findSystemSemester());
    }

    @Transactional
    public SystemSemesterResponse changeSystemSemester(final SystemSemesterRequest request) {
        final SystemSemester systemSemester = findSystemSemester();

        systemSemester.change(request.academicYear(), request.term());

        return SystemSemesterResponse.from(systemSemester);
    }

    private SystemSemester findSystemSemester() {
        final List<SystemSemester> systemSemesters = systemSemesterRepository.findAllOrdered();

        if (systemSemesters.isEmpty())
            throw new RestApiException(SYSTEM_SEMESTER_NOT_FOUND);

        return systemSemesters.get(0);
    }
}
