package com.nabil.teranga_pay_api.auth;

import java.util.UUID;

public record RegisterResponse(UUID userId, String fullName, String phone) {
}