package com.otorael.BaseAuths.security.PasswordValidator;

import java.util.regex.Pattern;

public class PasswordValidator {

    // Regex for at least one lowercase, one uppercase, one digit, min length 6
    private static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,}$";

    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    public static boolean isValid(String password, String email, String firstName, String lastName) {
        if (password == null || password.isBlank()) {
            return false;
        }
        // Check regex
        if (!pattern.matcher(password).matches()) {
            return false;
        }
        // Normalize everything to lowercase for comparison
        String lowerPassword = password.toLowerCase();

        // Disallow resemblance with username, firstname, lastname
        if (email != null && lowerPassword.contains(email.toLowerCase())) return false;
        if (firstName != null && lowerPassword.contains(firstName.toLowerCase())) return false;
        if (lastName != null && lowerPassword.contains(lastName.toLowerCase())) return false;

        return true;
    }
}
