package com.otorael.BaseAuths.service;

import com.otorael.BaseAuths.dto.auths.UserLogin;
import com.otorael.BaseAuths.dto.auths.UserRegister;
import com.otorael.BaseAuths.dto.passwords.ForgotPassword;
import com.otorael.BaseAuths.dto.passwords.PasswordResponse;
import com.otorael.BaseAuths.dto.passwords.ResetPassword;
import com.otorael.BaseAuths.dto.passwords.WrongPasswordResp;
import com.otorael.BaseAuths.dto.responses.CustomResponse;
import com.otorael.BaseAuths.dto.users.UserDetails;
import com.otorael.BaseAuths.kafka.AuthEventProducer;
import com.otorael.BaseAuths.security.JwtUtility;
import com.otorael.BaseAuths.model.Auths;
import com.otorael.BaseAuths.repository.AuthsRepository;
import com.otorael.BaseAuths.security.JwtSecurityFilter;
import com.otorael.BaseAuths.security.PasswordValidator.PasswordValidator;
import com.otorael.BaseAuths.service.NotificationService.EmailMessaging;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthsService implements AuthsServiceImpl {

    private final AuthEventProducer authEventProducer;

    private static final Logger log = LoggerFactory.getLogger(AuthsService.class);

    private final JwtUtility jwtUtility;

    private final AuthsRepository authsRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailMessaging emailMessaging;

    @Value("${app.login.max-attempts}")
    private int maxLoginAttempts;

    @Value("${app.login.lock-time-minutes}")
    private int lockTimeMinutes;

    public AuthsService(AuthEventProducer authEventProducer, JwtUtility jwtUtility, AuthsRepository authsRepository, PasswordEncoder passwordEncoder, EmailMessaging emailMessaging) {
        this.authEventProducer = authEventProducer;
        this.jwtUtility = jwtUtility;
        this.authsRepository = authsRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailMessaging = emailMessaging;
    }

    private @NotNull String formattedDate(){
        String formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
        return formattedDate;
    }

    /**
     * <p>
     *     login logic implemented and thrown to kafka for event creation to the consumers
     * </p>
     *
     * @param login as Data transfer object UserLogin
     * @return ResponseEntity as failure or success responses
     */
    @Override
    public ResponseEntity<?> login(UserLogin login) {
        try {
            Optional<Auths> userOptional = authsRepository.findByEmail(login.getEmail());
            if (userOptional.isEmpty()) {
                log.error("There was an error, Error: Email not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new CustomResponse("failed","An Email "+login.getEmail()+" is not found",formattedDate().formatted())
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
                            new CustomResponse("failed","Account is locked. Try again in " + remainingMinutes + " minutes",formattedDate().formatted())
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
                        new UserDetails("success","User logged in successfully",user.getFirstName(),user.getLastName(),user.getEmail(),user.getRole(),token,formattedDate().formatted())
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
                            new CustomResponse("failed","Account locked due to too many failed attempts. Try again in " + lockTimeMinutes + " minutes",formattedDate().formatted())
                    );
                }
                authsRepository.save(user);

                log.error("There was an error, Error: Incorrect password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new CustomResponse("failed","Incorrect password. " + (maxLoginAttempts - user.getFailedLoginAttempts()) + " attempts remaining",formattedDate().formatted())
                );
            }

        } catch (Exception e) {
            log.error("There was an error during login, Error: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CustomResponse("failed","there was internal server error",formattedDate().formatted())
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

                if (!PasswordValidator.isValid(register.getPassword(), register.getEmail(), register.getFirstName(), register.getLastName())) {
                    //throw new IllegalArgumentException("Password does not meet security requirements");
                    PasswordResponse passwordResponse = new PasswordResponse();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                            new WrongPasswordResp("failure","Password does not meet security requirements",passwordResponse,formattedDate().formatted())
                    );
                }

                auths.setPassword(passwordEncoder.encode(register.getPassword()));
                auths.setRole(register.getRole());
                //save a new user
                authsRepository.save(auths);

                log.info("User, {} registered successfully",register.getEmail());
                return ResponseEntity.status(HttpStatus.CREATED).body(
                        new CustomResponse("success","User registered successfully",formattedDate().formatted())
                );
            }
            log.error("There was an error, register: Email {} is already taken",register.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new CustomResponse("failed","The Email "+register.getEmail()+" is already taken",formattedDate().formatted())
            );
        } catch (Exception e) {
            log.error("There was an error during sign up, Error: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CustomResponse("failed","there was internal server error",formattedDate().formatted())
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
                    new CustomResponse("success","User logged out successfully",formattedDate().formatted())
            );
        }
        log.error("There was an error during signing out");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new CustomResponse("failed","there was an error during logout",formattedDate().formatted())
        );
    }

    @Override
    public ResponseEntity<?> forgotPassword(ForgotPassword forgotPassword) {
        try {
            Optional<Auths> userOptional = authsRepository.findByEmail(forgotPassword.getEmail());

            // Check if email exists or not
            if (userOptional.isEmpty()) {
                log.warn("Password reset requested for non-existent email: {}", forgotPassword.getEmail());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new CustomResponse("failed", "Email not found in our records", formattedDate().formatted())
                );
            }

            Auths user = userOptional.get();

            // Check if account is locked
            if (user.getAccountLockedUntil() != null) {
                if (LocalDateTime.now().isBefore(user.getAccountLockedUntil())) {
                    Duration remainingTime = Duration.between(LocalDateTime.now(), user.getAccountLockedUntil());
                    long remainingMinutes = remainingTime.toMinutes();
                    log.error("This account is locked, try again in {} minutes to reset password", remainingMinutes);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                            new CustomResponse("failed","This account is locked. Try again in " + remainingMinutes + " minutes to reset a password",formattedDate().formatted())
                    );
                } else {
                    // Lock has expired, reset the account
                    user.setFailedLoginAttempts(0);
                    user.setAccountLockedUntil(null);
                    authsRepository.save(user);
                }
            }

            // Generate 8 random CHARACTERS as reset token
            //String resetToken = String.format("%06d", new java.util.Random().nextInt(1000000));
            String resetToken = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 6).toUpperCase();

            // Set token expiry, for example 10 minutes from now
            LocalDateTime tokenExpiry = LocalDateTime.now().plusMinutes(10);

            // Save token and expiry in DB
            user.setResetToken(resetToken);
            user.setResetTokenExpiry(tokenExpiry);
            authsRepository.save(user);

            try {
                log.info("Calling Java Messaging service to send reset token to user... ");
                emailMessaging.sendOtpEmailAsync(forgotPassword.getEmail(), resetToken);

                log.info("Password reset token generated for {}: {}", forgotPassword.getEmail(), resetToken);
                return ResponseEntity.status(HttpStatus.OK).body(
                        new CustomResponse("success","Your OTP has been sent to "+forgotPassword.getEmail(), formattedDate().formatted())
                );

            } catch (Exception e) {
                log.error("There was an error during sending reset token request, Error: {}",e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new CustomResponse("failed","there was internal server error",formattedDate().formatted())
                );
            }

        } catch (Exception e) {
            log.error("There was an error during forgot Password request, Error: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CustomResponse("failed","there was internal server error",formattedDate().formatted())
            );
        }
    }

    @Override
    public ResponseEntity<?> resetPassword(ResetPassword resetPassword) {
        try {
            Optional<Auths> userOptional = authsRepository.findByResetToken(resetPassword.getToken());

            if (userOptional.isEmpty()) {
                log.error("Invalid or expired reset token: {}", resetPassword.getToken());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new CustomResponse("failure","The OTP "+resetPassword.getToken()+" is invalid or expired",formattedDate().formatted())
                );
            }

            Auths user = userOptional.get();

            // Check if token has expired
            if (user.getResetTokenExpiry() == null || LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
                log.error("Reset token expired for {}", user.getEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new CustomResponse("failure","The OTP is invalid or expired",formattedDate().formatted())
                );
            }

            // Update password
            if (!PasswordValidator.isValid(resetPassword.getNewPassword(), user.getEmail(), user.getFirstName(), user.getLastName())) {
                //throw new IllegalArgumentException("Password does not meet security requirements");
                PasswordResponse passwordResponse = new PasswordResponse();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        new WrongPasswordResp("failure","Password does not meet security requirements",passwordResponse,formattedDate().formatted())
                );
            }
            user.setPassword(passwordEncoder.encode(resetPassword.getNewPassword()));

            // Clear token and expiry after successful reset
            user.setResetToken(null);
            user.setResetTokenExpiry(null);

            authsRepository.save(user);

            log.info("Password reset successfully for {}", user.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new CustomResponse("success","New password was created successfully",formattedDate().formatted())
            );

        } catch (Exception e) {
            log.error("Error in resetPassword: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CustomResponse("failure","Oops there was an error",formattedDate().formatted())
            );
        }
    }
}
