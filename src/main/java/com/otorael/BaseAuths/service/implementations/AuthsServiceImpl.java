package com.otorael.BaseAuths.service.implementations;

import com.otorael.BaseAuths.dto.UserRegister;
import com.otorael.BaseAuths.dto.UserLogin;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface AuthsServiceImpl {
    public ResponseEntity<?> login (UserLogin login);
    public ResponseEntity<?> register (UserRegister register);
    public ResponseEntity<?> logout (HttpServletRequest request);
}
