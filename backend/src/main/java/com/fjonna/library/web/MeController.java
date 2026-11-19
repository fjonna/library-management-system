package com.fjonna.library.web;

import com.fjonna.library.user.User;
import com.fjonna.library.user.UserService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        User user = userService.syncFromJwt(jwt);
        return Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "roles", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList());
    }
}