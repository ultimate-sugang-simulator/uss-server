package uss.code.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uss.code.auth.annotation.Auth;
import uss.code.member.dto.request.DepartmentUpdateRequest;
import uss.code.member.dto.response.MemberProfileResponse;
import uss.code.member.service.MemberService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController implements MemberControllerDocs {

    private final MemberService memberService;

    @GetMapping("/profile")
    public ResponseEntity<MemberProfileResponse> getProfile(@Auth final long memberId){
        return ResponseEntity.ok(memberService.getProfile(memberId));
    }

    @PatchMapping("/department")
    public ResponseEntity<Void> updateDepartment(
            @Auth final long memberId,
            @Valid @RequestBody final DepartmentUpdateRequest request
    ){
        memberService.updateDepartment(memberId, request);
        return ResponseEntity.ok().build();
    }
}
