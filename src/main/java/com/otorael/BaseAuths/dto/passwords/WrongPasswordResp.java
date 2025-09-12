package com.otorael.BaseAuths.dto.passwords;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WrongPasswordResp {
    private String notification;
    private String message;
    private PasswordResponse passwordResponse;
    private String timestamp;
}
