package com.otorael.BaseAuths.controller;

import com.otorael.BaseAuths.dto.auths.UserLogin;
import com.otorael.BaseAuths.dto.auths.UserRegister;
import com.otorael.BaseAuths.dto.passwords.ForgotPassword;
import com.otorael.BaseAuths.dto.passwords.ResetPassword;
import com.otorael.BaseAuths.dto.responses.CustomResponse;
import com.otorael.BaseAuths.service.AuthsService;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/")
public class AuthsController {

    private static final Logger log = LoggerFactory.getLogger(AuthsController.class);

    private final AuthsService authsService;

    public AuthsController(AuthsService authsService) {
        this.authsService = authsService;
    }

    @RequestMapping(value = "/public/login", method = RequestMethod.POST)
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
    @RequestMapping(value = "/public/register",method = RequestMethod.POST)
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
    @RequestMapping(value = "/private/logout",method = RequestMethod.GET)
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
    @RequestMapping(value = "/public/forgot-password",method = RequestMethod.POST)
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPassword forgotPassword){
        try {
            //accessing the forgotPassword method in service class
            log.info("accessing the forgotPassword service class");
            return authsService.forgotPassword(forgotPassword);
        } catch (Exception e) {
            log.error("There was an error during forgot password request, Error: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CustomResponse("failed","there was internal server error",""+ Instant.now())
            );
        }
    }

    /**
     *
     * @param resetPassword
     * @return
     */
    @RequestMapping(value = "/public/reset-password",method = RequestMethod.POST)
    public ResponseEntity<?> resetPassword(@RequestBody ResetPassword resetPassword){
        try {
            //accessing the resetPassword method in service class
            log.info("accessing the resetPassword method in service class");
            return authsService.resetPassword(resetPassword);
        } catch (Exception e) {
            log.error("There was an error during reset password request, Error: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CustomResponse("failed","there was internal server error",""+ Instant.now())
            );
        }
    }
}
