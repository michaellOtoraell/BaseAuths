package com.otorael.BaseAuths.dto.passwords;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResponse {
    private String RespOne = "Password can't be same with email, or names";
    private String RespTwo = "Password needs to be of 6 characters or more";
    private String RespThree = "Password needs to be of alphanumeric";
    private String RespFour =  "Password cannot be blank";
}
