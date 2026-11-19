package com.fjonna.library.user;

import java.util.UUID;

public record UserSummary(UUID id, String username, String email, long bookCount) {
}
