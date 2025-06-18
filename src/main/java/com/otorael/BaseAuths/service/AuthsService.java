package com.otorael.BaseAuths.service;

import com.otorael.BaseAuths.kafka.AuthEventProducer;
import com.otorael.BaseAuths.security.JwtUtility;
import com.otorael.BaseAuths.dto.CustomResponse;
import com.otorael.BaseAuths.dto.UserDetails;
import com.otorael.BaseAuths.dto.UserRegister;
import com.otorael.BaseAuths.dto.UserLogin;
import com.otorael.BaseAuths.model.Auths;
import com.otorael.BaseAuths.repository.AuthsRepository;
import com.otorael.BaseAuths.security.JwtSecurityFilter;
import com.otorael.BaseAuths.service.implementations.AuthsServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthsService implements AuthsServiceImpl {

    private final AuthEventProducer authEventProducer;

    private static final Logger log = LoggerFactory.getLogger(AuthsService.class);

    private final JwtUtility jwtUtility;

    private final AuthsRepository authsRepository;

    private final PasswordEncoder passwordEncoder;

    public AuthsService(AuthEventProducer authEventProducer, JwtUtility jwtUtility, AuthsRepository authsRepository, PasswordEncoder passwordEncoder) {
        this.authEventProducer = authEventProducer;
        this.jwtUtility = jwtUtility;
        this.authsRepository = authsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public ResponseEntity<?> login(UserLogin login) {
        try {
            Optional<Auths> loginAttempt = authsRepository.findByEmail(login.getEmail());
            if (loginAttempt.isPresent()) {
                if (passwordEncoder.matches(login.getPassword(), loginAttempt.get().getPassword())){
                    String token = jwtUtility.generateToken(loginAttempt);
                    //Sending Kafka Auths topic
                    log.info("Sending Kafka Auths topic ...");

                    UserDetails userDetails = new UserDetails(
                            "success",
                            "User logged in successfully",
                            loginAttempt.get().getFirstName(),
                            loginAttempt.get().getLastName(),
                            loginAttempt.get().getEmail(),
                            loginAttempt.get().getRole(),
                            token,
                            Instant.now().toString()
                    );
                    authEventProducer.sendAuthEvent(userDetails);

                    log.info("User logged in successfully");
                    return ResponseEntity.status(HttpStatus.OK).body(
                            new UserDetails("success","User logged in successfully",loginAttempt.get().getFirstName(),loginAttempt.get().getLastName(),loginAttempt.get().getEmail(),loginAttempt.get().getRole(),token,""+ Instant.now())
                    );
                }
                log.error("There was an error, Error: Incorrect password");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new CustomResponse("failed","Incorrect password",""+Instant.now())
                );
            }
            log.error("There was an error, Error: Email not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new CustomResponse("failed","An Email "+login.getEmail()+" is not found",""+Instant.now())
            );

        } catch (Exception e) {
            log.error("There was an error during login, Error: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CustomResponse("failed","there was internal server error",""+Instant.now())
            );
        }
    }

    @Override
    public ResponseEntity<?> register(UserRegister register) {
        try {
            log.info("checking if the attempting users exists in the database");
            Optional<Auths> registerAttempt = authsRepository.findByEmail(register.getEmail());
            if (registerAttempt.isEmpty()){
                //create a new user
                Auths auths = new Auths();
                auths.setFirstName(register.getFirstName());
                auths.setLastName(register.getLastName());
                auths.setEmail(register.getEmail());
                auths.setPassword(passwordEncoder.encode(register.getPassword()));
                auths.setRole(register.getRole());
                //save a new user
                authsRepository.save(auths);

                log.info("User, {} registered successfully",register.getEmail());
                return ResponseEntity.status(HttpStatus.CREATED).body(
                        new CustomResponse("success","User registered successfully",""+Instant.now())
                );
            }
            log.error("There was an error, register: Email {} is already taken",register.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new CustomResponse("failed","The Email "+register.getEmail()+" is already taken",""+Instant.now())
            );
        } catch (Exception e) {
            log.error("There was an error during sign up, Error: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CustomResponse("failed","there was internal server error",""+Instant.now())
            );
        }
    }
    @Override
    public ResponseEntity<?> logout(@NotNull HttpServletRequest request) {
        String authHeader = request.getHeader("authorization");
        if (!authHeader.isEmpty()) {
            String token = authHeader.substring(7);

            //blacklist a token
            JwtSecurityFilter.blacklistToken(token);
            log.error("User logged out successfully");
            return ResponseEntity.status(HttpStatus.OK).body(
                    new CustomResponse("success","User logged out successfully",""+Instant.now())
            );
        }
        log.error("There was an error during signing out");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new CustomResponse("failed","there was an error during logout",""+Instant.now())
        );
    }
}
