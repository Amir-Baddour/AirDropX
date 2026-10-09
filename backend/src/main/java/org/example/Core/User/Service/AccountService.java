package org.example.Core.User.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.example.Config.Config;
import org.example.Core.User.Exception.EmailAlreadyRegisteredException;
import org.example.Core.User.Exception.InvalidCredentialsException;
import org.example.Core.User.Helper.PasswordHasher;
import org.example.Core.User.Helper.UserInputValidator;
import org.example.Core.User.Model.UserProfile;
import org.example.Infra.Persistence.User.AccountRepository;

import java.util.Date;
import java.util.Optional;

/** Email/password sign-up and sign-in, plus profile management for every account type. */
public class AccountService {
    private static final long TOKEN_LIFETIME_MS = 24L * 60 * 60 * 1000; // same 24 h as Google sign-in

    public record LoginResult(String accessToken, long expiresAtMillis, UserProfile profile) {
    }

    private final AccountRepository repository = new AccountRepository();

    public void register(String email, String password, String firstName, String lastName) throws Exception {
        String normalisedEmail = UserInputValidator.email(email);
        UserInputValidator.password(password);
        String first = UserInputValidator.name(firstName, "First name");
        String last = UserInputValidator.name(lastName, "Last name");
        repository.createLocalUser(normalisedEmail, PasswordHasher.hash(password), first, last);
    }

    public LoginResult login(String email, String password) throws Exception {
        String normalisedEmail = email == null ? "" : email.trim().toLowerCase();
        Optional<AccountRepository.Credentials> credentials = repository.findCredentialsByEmail(normalisedEmail);
        // Always run one hash check, so an unknown email takes as long as a wrong password.
        String stored = credentials.map(AccountRepository.Credentials::passwordHash).orElse(PasswordHasher.dummyHash());
        boolean valid = PasswordHasher.verify(password, stored);
        if (credentials.isEmpty() || !valid) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        UserProfile profile = repository.findProfile(credentials.get().userId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        long now = System.currentTimeMillis();
        long expiresAt = now + TOKEN_LIFETIME_MS;
        String jwt = Jwts.builder()
                .setSubject(profile.id())
                .claim("username", profile.username())
                .claim("provider", profile.provider())
                .claim("role", profile.role())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(expiresAt))
                .signWith(SignatureAlgorithm.HS256, Config.getJwtSecret())
                .compact();
        return new LoginResult(jwt, expiresAt, profile);
    }

    public UserProfile getProfile(String userId) throws Exception {
        return repository.findProfile(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public UserProfile updateProfile(String userId, String firstName, String lastName, String phone, String address) throws Exception {
        repository.updateProfile(
                userId,
                UserInputValidator.name(firstName, "First name"),
                UserInputValidator.name(lastName, "Last name"),
                UserInputValidator.phone(phone),
                UserInputValidator.address(address));
        return getProfile(userId);
    }

    public void changePassword(String userId, String currentPassword, String newPassword) throws Exception {
        Optional<String> stored = repository.findPasswordHash(userId);
        if (stored.isEmpty()) {
            throw new IllegalArgumentException("This account signs in with Google, so it has no password to change");
        }
        if (!PasswordHasher.verify(currentPassword, stored.get())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        UserInputValidator.password(newPassword);
        if (newPassword.equals(currentPassword)) {
            throw new IllegalArgumentException("Choose a password you have not used just now");
        }
        repository.updatePasswordHash(userId, PasswordHasher.hash(newPassword));
    }
}
