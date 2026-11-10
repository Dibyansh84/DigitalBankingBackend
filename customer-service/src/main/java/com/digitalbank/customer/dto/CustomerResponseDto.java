package com.digitalbank.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponseDto
{
    private Long id;
    private String fullName;
    private String email;
    private String phone;
}

/*Response DTO kyun? Hum database entity ko directly API response mein expose nahi karenge.
Response DTO se hum decide karte hain ki client ko kaunse fields dikhane hain.*/