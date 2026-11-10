package com.digitalbank.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AccountUpdateRequestDto {
    @NotBlank(message = "Account type is required")
    @Pattern(regexp = "SAVINGS|CURRENT", message = "Account type must be SAVINGS or CURRENT")
    private String accountType;

    @NotBlank(message = "Account status is required")
    @Pattern(regexp = "ACTIVE|BLOCKED|CLOSED",
            message = "Account status must be ACTIVE, BLOCKED or CLOSED")
    private String accountStatus;
}
