package uss.code.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.global.exception.domain.RestApiException;
import uss.code.member.domain.Member;
import uss.code.member.domain.MemberDepartment;
import uss.code.member.dto.request.DepartmentUpdateRequest;
import uss.code.member.dto.response.MemberProfileResponse;
import uss.code.member.repository.MemberRepository;

import static uss.code.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public MemberProfileResponse getProfile(final Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RestApiException(MEMBER_NOT_FOUND));

        return MemberProfileResponse.of(member);
    }

    @Transactional
    public void updateDepartment(
            final long memberId,
            final DepartmentUpdateRequest request
    ) {
        final Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RestApiException(MEMBER_NOT_FOUND));

        member.updateDepartment(MemberDepartment.fromSelectable(request.department()));
    }
}
