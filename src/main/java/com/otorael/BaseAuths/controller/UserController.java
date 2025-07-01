package com.otorael.BaseAuths.controller;

import com.otorael.BaseAuths.dto.CustomResponse;
import com.otorael.BaseAuths.dto.GetAllUsers;
import com.otorael.BaseAuths.dto.MultiUsersDto;
import com.otorael.BaseAuths.model.Auths;
import com.otorael.BaseAuths.repository.AuthsRepository;
import com.otorael.BaseAuths.security.JwtSecurityFilter;
import com.otorael.BaseAuths.security.JwtUtility;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
//@RequestMapping("/api/v1/")
public class UserController {

    private final JwtSecurityFilter jwtSecurityFilter;

    private final JwtUtility jwtUtility;

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final AuthsRepository authsRepository;

    public UserController(JwtSecurityFilter jwtSecurityFilter, JwtUtility jwtUtility, AuthsRepository authsRepository) {
        this.jwtSecurityFilter = jwtSecurityFilter;
        this.jwtUtility = jwtUtility;
        this.authsRepository = authsRepository;
    }

    @RequestMapping(value = "/api/v1/private/get-users", method = RequestMethod.GET)
    public ResponseEntity<?> getAllUsers(String token, HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                if (jwtSecurityFilter.isTokenBlacklisted(token)){
                    String email = jwtUtility.getUserEmail(authHeader.substring(7));

                    log.info("Initializing the request to check for all the users in the database");
                    List<Auths> allUsers = authsRepository.findAll();
                    List<MultiUsersDto> userDetailsList = new ArrayList<>();
                    // Convert each Auths entity to UserDetails DTO
                    for (Auths auth : allUsers) {
                        MultiUsersDto userDetails = new MultiUsersDto();
                        userDetails.setFirstName(auth.getFirstName());
                        userDetails.setLastName(auth.getLastName());
                        userDetails.setEmail(auth.getEmail());
                        userDetails.setRole(auth.getRole());
                        // Leave other fields (notification, message, token, timestamp) as null/default
                        userDetailsList.add(userDetails);
                    }
                    log.info("All users retrieved successfully, by {} ",email);
                    return ResponseEntity.status(HttpStatus.OK).body(
                            new GetAllUsers(
                                    "success",
                                    "All users retrieved successfully",
                                    "Date :"+Instant.now().toString().trim().substring(0,10)+" time: "+Instant.now().toString().trim().substring(11,19),
                                    userDetailsList
                            )
                    );
                }
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new CustomResponse("failed","Token is not valid",""+Instant.now())
                );
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new CustomResponse("failed","Token is null or missing",""+Instant.now())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetAllUsers(
                            "failed",
                            "Failed to retrieve users: " + e.getMessage(),
                            Instant.now().toString().trim().substring(0,11),
                            Collections.emptyList()
                    )
            );
        }
    }
}
