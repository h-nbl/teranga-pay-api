package com.nabil.teranga_pay_api.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TransferRequest(
        @NotBlank @Size(max = 30) String toPhone,
        @Positive long amount) {

    // Le numéro du destinataire est une donnée personnelle : on ne l'écrit jamais dans les logs
    @Override
    public String toString() {
        return "TransferRequest[amount=" + amount + "]";
    }
}