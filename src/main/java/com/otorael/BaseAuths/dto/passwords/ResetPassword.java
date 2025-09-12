package com.otorael.BaseAuths.dto.passwords;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPassword {
    private String token;
    private String newPassword;
}
