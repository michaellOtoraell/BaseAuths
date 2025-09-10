package com.otorael.BaseAuths.service.implementations;

import com.otorael.BaseAuths.dto.ForgotPassword;
import com.otorael.BaseAuths.dto.ResetPassword;
import com.otorael.BaseAuths.dto.UserRegister;
import com.otorael.BaseAuths.dto.UserLogin;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface AuthsServiceImpl {

    ResponseEntity<?> login (UserLogin login);

    ResponseEntity<?> register (UserRegister register);

    ResponseEntity<?> logout (HttpServletRequest request);

    ResponseEntity<?> forgotPassword(ForgotPassword forgotPassword);

    ResponseEntity<?> resetPassword(ResetPassword resetPassword);

}
