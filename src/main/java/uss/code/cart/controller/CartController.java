package uss.code.cart.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uss.code.auth.annotation.Auth;
import uss.code.cart.dto.response.CartedCoursesResponse;
import uss.code.cart.service.CartService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/carts")
public class CartController implements CartControllerDocs {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartedCoursesResponse> getCartedCourse(@Auth final long memberId){
        return ResponseEntity.ok(cartService.getCartedCourse(memberId));
    }

    @PostMapping("/{courseId}")
    public ResponseEntity<Void> addCart(
            @Auth final long memberId,
            @PathVariable("courseId") final long courseId
    ){
        cartService.addCart(memberId, courseId);
        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCartedCourse(
            @Auth final long memberId,
            @PathVariable("courseId") final long courseId
    ){
        cartService.deleteCartedCourse(memberId, courseId);
        return ResponseEntity.noContent().build();
    }
}
