package com.otorael.BaseAuths.service.implementations;

import com.otorael.BaseAuths.dto.passwords.ForgotPassword;
import com.otorael.BaseAuths.dto.passwords.ResetPassword;
import com.otorael.BaseAuths.dto.auths.UserRegister;
import com.otorael.BaseAuths.dto.auths.UserLogin;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface AuthsServiceImpl {

    ResponseEntity<?> login (UserLogin login);

    ResponseEntity<?> register (UserRegister register);

    ResponseEntity<?> logout (HttpServletRequest request);

    ResponseEntity<?> forgotPassword(ForgotPassword forgotPassword);

    ResponseEntity<?> resetPassword(ResetPassword resetPassword);

}
