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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthsService implements AuthsServiceImpl {

    private final AuthEventProducer authEventProducer;

    private static final Logger log = LoggerFactory.getLogger(AuthsService.class);

    private final JwtUtility jwtUtility;

    private final AuthsRepository authsRepository;

    private final PasswordEncoder passwordEncoder;

    @Value("${app.login.max-attempts}")
    private int maxLoginAttempts;

    @Value("${app.login.lock-time-minutes}")
    private int lockTimeMinutes;

    public AuthsService(AuthEventProducer authEventProducer, JwtUtility jwtUtility, AuthsRepository authsRepository, PasswordEncoder passwordEncoder) {
        this.authEventProducer = authEventProducer;
        this.jwtUtility = jwtUtility;
        this.authsRepository = authsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public ResponseEntity<?> login(UserLogin login) {
        try {
            Optional<Auths> userOptional = authsRepository.findByEmail(login.getEmail());
            if (userOptional.isEmpty()) {
                log.error("There was an error, Error: Email not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new CustomResponse("failed","An Email "+login.getEmail()+" is not found",""+Instant.now())
                );
            }
            Auths user = userOptional.get();

            // Check if account is locked
            if (user.getAccountLockedUntil() != null) {
                if (LocalDateTime.now().isBefore(user.getAccountLockedUntil())) {
                    Duration remainingTime = Duration.between(LocalDateTime.now(), user.getAccountLockedUntil());
                    long remainingMinutes = remainingTime.toMinutes();
                    log.error("Account is locked. Try again in {} minutes", remainingMinutes);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                            new CustomResponse("failed","Account is locked. Try again in " + remainingMinutes + " minutes",""+Instant.now())
                    );
                } else {
                    // Lock has expired, reset the account
                    user.setFailedLoginAttempts(0);
                    user.setAccountLockedUntil(null);
                    authsRepository.save(user);
                }
            }

            if (passwordEncoder.matches(login.getPassword(), user.getPassword())) {
                // Successful login - reset attempts
                user.setFailedLoginAttempts(0);
                user.setAccountLockedUntil(null);
                user.setLastFailedLogin(null);
                authsRepository.save(user);

                String token = jwtUtility.generateToken(userOptional);

                //Sending Kafka Auths topic
                log.info("Sending Kafka Auths topic ...");

                UserDetails userDetails = new UserDetails(
                        "success",
                        "User logged in successfully",
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getRole(),
                        token,
                        Instant.now().toString()
                );
                authEventProducer.sendAuthEvent(userDetails);

                log.info("User logged in successfully");
                return ResponseEntity.status(HttpStatus.OK).body(
                        new UserDetails("success","User logged in successfully",user.getFirstName(),user.getLastName(),user.getEmail(),user.getRole(),token,""+ Instant.now())
                );
            } else {
                // Failed login - increment attempts and update last failed login
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                user.setLastFailedLogin(LocalDateTime.now());

                if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
                    user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(lockTimeMinutes));
                    log.error("Account locked due to too many failed attempts");
                    authsRepository.save(user);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                            new CustomResponse("failed","Account locked due to too many failed attempts. Try again in " + lockTimeMinutes + " minutes",""+Instant.now())
                    );
                }
                authsRepository.save(user);

                log.error("There was an error, Error: Incorrect password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new CustomResponse("failed","Incorrect password. " + (maxLoginAttempts - user.getFailedLoginAttempts()) + " attempts remaining",""+Instant.now())
                );
            }

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
