package com.otorael.BaseAuths.controller;

import com.otorael.BaseAuths.dto.GetAllUsers;
import com.otorael.BaseAuths.dto.MultiUsersDto;
import com.otorael.BaseAuths.model.Auths;
import com.otorael.BaseAuths.repository.AuthsRepository;
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
@RequestMapping("/api/v1/")
public class UserController {

    private final AuthsRepository authsRepository;

    public UserController(AuthsRepository authsRepository) {
        this.authsRepository = authsRepository;
    }

    @RequestMapping(value = "/private/get-users", method = RequestMethod.GET)
    public ResponseEntity<?> getUsers() {
        try {
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
            return ResponseEntity.status(HttpStatus.OK).body(
                    new GetAllUsers(
                            "success",
                            "All users retrieved successfully",
                            "Date :"+Instant.now().toString().trim().substring(0,10)+" time: "+Instant.now().toString().trim().substring(11,19),
                            userDetailsList
                    )
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
