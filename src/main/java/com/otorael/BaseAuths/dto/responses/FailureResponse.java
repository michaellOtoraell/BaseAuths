package com.otorael.BaseAuths.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FailureResponse {
    private String notification = "failed";
    private String message = "There is an internal error: ERROR : ";
    private String timestamp = String.valueOf(Instant.now());
}
