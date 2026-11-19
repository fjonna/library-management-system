package com.fjonna.library.user;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /** Creates the user on first login, refreshes name/email on later ones. */
    @Transactional
    public User syncFromJwt(Jwt jwt) {
        UUID id = UUID.fromString(jwt.getSubject());
        User user = userRepository.findById(id).orElseGet(() -> {
            User created = new User();
            created.setId(id);
            return created;
        });

        String username = jwt.getClaimAsString("preferred_username");
        user.setUsername(username != null ? username : jwt.getSubject());
        user.setEmail(jwt.getClaimAsString("email"));
        user.setFirstName(jwt.getClaimAsString("given_name"));
        user.setLastName(jwt.getClaimAsString("family_name"));
        return userRepository.save(user);
    }
}