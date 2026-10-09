package org.example.Core.User.Model;

/** Everything the profile page shows. {@code hasPassword} is false for Google-only accounts. */
public record UserProfile(
        String id,
        String username,
        String provider,
        String email,
        String firstName,
        String lastName,
        String phone,
        String address,
        String pfp,
        String role,
        boolean hasPassword,
        String createdAt
) {
    /** What the sidebar shows: the full name when known, else the username. */
    public String displayName() {
        String full = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        return full.isEmpty() ? username : full;
    }
}
