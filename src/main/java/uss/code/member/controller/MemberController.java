package uss.code.member.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uss.code.auth.annotation.Auth;
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
}
