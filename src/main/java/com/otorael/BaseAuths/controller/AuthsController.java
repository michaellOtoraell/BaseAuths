package com.otorael.BaseAuths.controller;

import com.otorael.BaseAuths.dto.CustomResponse;
import com.otorael.BaseAuths.dto.UserRegister;
import com.otorael.BaseAuths.dto.UserLogin;
import com.otorael.BaseAuths.service.AuthsService;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/")
public class AuthsController {

    private static final Logger log = LoggerFactory.getLogger(AuthsController.class);

    private final AuthsService authsService;

    public AuthsController(AuthsService authsService) {
        this.authsService = authsService;
    }

    @RequestMapping("/public/login")
    public ResponseEntity<?> login(@RequestBody UserLogin login){
        try {
            //accessing the service
            log.info("accessing the service ");
            return authsService.login(login);
        } catch (Exception e) {
            log.error("There was an error during login, Error: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CustomResponse("failed","there was internal server error",""+ Instant.now())
            );
        }
    }
    @RequestMapping("/public/register")
    public ResponseEntity<?> register(@RequestBody UserRegister register){
        try {
            //access the register method in service class
            log.info("access the register method in service class");
            return authsService.register(register);
        } catch (Exception e) {
            log.error("There was an error during sign up, Error: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CustomResponse("failed","there was internal server error",""+ Instant.now())
            );
        }
    }
    @RequestMapping("/private/logout")
    public ResponseEntity<?> logout(@NotNull HttpServletRequest request){
        try {
            //access the register method in service class
            log.info("access the logout method in service class");
            return authsService.logout(request);
        } catch (Exception e) {
            log.error("There was an error during sign out, Error: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CustomResponse("failed","there was internal server error",""+ Instant.now())
            );
        }
    }
}
