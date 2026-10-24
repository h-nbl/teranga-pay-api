package com.nabil.teranga_pay_api.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {

    @Override
    public String toString() {
        return "LoginResponse[redacted]";
    }
}