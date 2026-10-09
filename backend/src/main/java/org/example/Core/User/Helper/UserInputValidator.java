package org.example.Core.User.Helper;

import java.util.regex.Pattern;

/** Input rules for email/password accounts and the profile form. Throws IllegalArgumentException (HTTP 400). */
public final class UserInputValidator {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+'-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()-]{6,25}$");
    public static final int MIN_PASSWORD = 8;
    public static final int MAX_PASSWORD = 128;
    public static final int MAX_NAME = 100;
    public static final int MAX_EMAIL = 254;
    public static final int MAX_ADDRESS = 255;

    private UserInputValidator() {
    }

    /** Lower-cases and trims, then validates. Returns the normalised email. */
    public static String email(String raw) {
        String email = raw == null ? "" : raw.trim().toLowerCase();
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (email.length() > MAX_EMAIL || !EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("Enter a valid email address");
        }
        return email;
    }

    public static void password(String password) {
        if (password == null || password.length() < MIN_PASSWORD) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD + " characters");
        }
        if (password.length() > MAX_PASSWORD) {
            throw new IllegalArgumentException("Password must be at most " + MAX_PASSWORD + " characters");
        }
        boolean letter = false;
        boolean digit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        if (!letter || !digit) {
            throw new IllegalArgumentException("Password must contain at least one letter and one number");
        }
    }

    public static String name(String raw, String label) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        if (value.length() > MAX_NAME) {
            throw new IllegalArgumentException(label + " must be at most " + MAX_NAME + " characters");
        }
        return value;
    }

    /** Optional. Returns null for blank input. */
    public static String phone(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (!PHONE.matcher(value).matches()) {
            throw new IllegalArgumentException("Enter a valid phone number, for example +961 70 123 456");
        }
        return value;
    }

    /** Optional. Returns null for blank input. */
    public static String address(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > MAX_ADDRESS) {
            throw new IllegalArgumentException("Address must be at most " + MAX_ADDRESS + " characters");
        }
        return value;
    }
}
