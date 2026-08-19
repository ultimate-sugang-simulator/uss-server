package uss.code.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.auth.dto.request.LoginRequest;
import uss.code.auth.dto.request.SignUpRequest;
import uss.code.auth.dto.response.AuthTokenResponse;
import uss.code.auth.dto.response.EmailAvailabilityResponse;
import uss.code.auth.infra.JwtProvider;
import uss.code.auth.infra.MemberPasswordEncoder;
import uss.code.global.exception.domain.RestApiException;
import uss.code.member.domain.AcademicStatus;
import uss.code.member.domain.Member;
import uss.code.member.domain.MemberCollege;
import uss.code.member.domain.MemberDepartment;
import uss.code.member.domain.MemberGrade;
import uss.code.member.repository.MemberRepository;

import static uss.code.global.exception.domain.ExceptionCode.COLLEGE_DEPARTMENT_MISMATCH;
import static uss.code.global.exception.domain.ExceptionCode.EMAIL_ALREADY_EXISTS;
import static uss.code.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND;
import static uss.code.global.exception.domain.ExceptionCode.PASSWORD_NOT_MATCH;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final MemberPasswordEncoder passwordEncoder;

    private final MemberRepository memberRepository;

    @Transactional
    public AuthTokenResponse signUp(final SignUpRequest request) {
        final MemberDepartment department = MemberDepartment.from(request.department());

        validateCollegeMatchesDepartment(MemberCollege.from(request.college()), department);

        final Member member = Member.create(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.studentId(),
                request.name(),
                department,
                MemberGrade.from(request.grade()),
                AcademicStatus.from(request.academicStatus()),
                request.lastSemesterGpa()
        );

        return jwtProvider.generateAuthToken(saveUniqueEmail(member).getId());
    }

    @Transactional(readOnly = true)
    public EmailAvailabilityResponse checkEmailAvailability(final String email) {
        return EmailAvailabilityResponse.of(!memberRepository.existsByEmail(email));
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse login(final LoginRequest request) {
        final Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new RestApiException(MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), member.getPassword()))
            throw new RestApiException(PASSWORD_NOT_MATCH);

        return jwtProvider.generateAuthToken(member.getId());
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse reIssue(final String accessToken) {
        final Long memberId = jwtProvider.getMemberIdAllowingExpiration(accessToken);

        if (!memberRepository.existsById(memberId))
            throw new RestApiException(MEMBER_NOT_FOUND);

        return jwtProvider.generateAuthToken(memberId);
    }

    private void validateCollegeMatchesDepartment(
            final MemberCollege college,
            final MemberDepartment department
    ) {
        if (department.getMemberCollege() != college) {
            throw new RestApiException(COLLEGE_DEPARTMENT_MISMATCH);
        }
    }

    private Member saveUniqueEmail(final Member member) {
        if (memberRepository.existsByEmail(member.getEmail())) {
            throw new RestApiException(EMAIL_ALREADY_EXISTS);
        }

        try {
            return memberRepository.saveAndFlush(member);
        } catch (final DataIntegrityViolationException e) {
            throw new RestApiException(EMAIL_ALREADY_EXISTS);
        }
    }
}
