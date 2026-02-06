package uss.code.cart.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uss.code.auth.annotation.Auth;
import uss.code.cart.dto.response.CartedCoursesResponse;
import uss.code.cart.service.CartService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/carts")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartedCoursesResponse> getCart(@Auth final long memberId){
        return ResponseEntity.ok(cartService.getCart(memberId));
    }
}
