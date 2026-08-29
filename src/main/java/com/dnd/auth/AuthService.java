package com.dnd.auth;

import com.dnd.security.PasswordHasher;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Registration / login / email-verification / password-recovery logic.
 *
 * <p>Every operation that needs to email the user (registration,
 * resend-verification, forgot-password) tries to actually send it via
 * {@link EmailService}, but falls back to returning the code inline (see
 * {@link CodeResult#emailSent()}) when SMTP hasn't been configured yet, so
 * the whole flow remains usable before real credentials are supplied.</p>
 */
public class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final long CODE_VALIDITY_MINUTES = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserStore store;
    private final EmailService emailService;

    public AuthService() {
        this(new UserStore(), new EmailService());
    }

    public AuthService(UserStore store, EmailService emailService) {
        this.store = store;
        this.emailService = emailService;
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /** Result of an operation that needs to convey a one-time code to the user. */
    public record CodeResult(boolean success, String message, String code, boolean emailSent) {
        public static CodeResult failure(String message) {
            return new CodeResult(false, message, null, false);
        }
    }

    /** Result of a plain success/fail operation (verify, reset). */
    public record SimpleResult(boolean success, String message) {
    }

    /** Result of a login attempt. */
    public record LoginResult(boolean success, String message, User user) {
        public static LoginResult failure(String message) {
            return new LoginResult(false, message, null);
        }
    }

    public CodeResult register(String username, String email, String password, String confirmPassword) {
        if (username == null || username.isBlank()) return CodeResult.failure("Username is required.");
        if (!isValidEmail(email)) return CodeResult.failure("Please enter a valid email address.");
        if (password == null || password.length() < 6) return CodeResult.failure("Password must be at least 6 characters.");
        if (!password.equals(confirmPassword)) return CodeResult.failure("Passwords do not match.");
        if (store.findByUsername(username).isPresent()) return CodeResult.failure("That username is already taken.");
        if (store.findByEmail(email).isPresent()) return CodeResult.failure("An account with that email already exists.");

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        String salt = PasswordHasher.generateSalt();
        user.setPasswordSalt(salt);
        user.setPasswordHash(PasswordHasher.hash(password, salt));
        user.setEmailVerified(false);
        String code = generateCode();
        user.setVerificationCode(code);
        user.setVerificationExpiresAtMillis(System.currentTimeMillis() + CODE_VALIDITY_MINUTES * 60_000L);
        store.save(user);

        boolean sent = tryEmail(email,
            "Verify your DnD Campaign Manager account",
            "Welcome, " + username + "!\n\nYour verification code is: " + code
                + "\n\nIt expires in " + CODE_VALIDITY_MINUTES + " minutes.");
        return new CodeResult(true, "Account created. Enter the verification code to continue.", code, sent);
    }

    public CodeResult resendVerification(String email) {
        Optional<User> found = store.findByEmail(email);
        if (found.isEmpty()) return CodeResult.failure("No account found with that email.");
        User user = found.get();
        if (user.isEmailVerified()) return CodeResult.failure("This account is already verified.");
        String code = generateCode();
        user.setVerificationCode(code);
        user.setVerificationExpiresAtMillis(System.currentTimeMillis() + CODE_VALIDITY_MINUTES * 60_000L);
        store.save(user);
        boolean sent = tryEmail(email, "Your new verification code",
            "Your verification code is: " + code + "\n\nIt expires in " + CODE_VALIDITY_MINUTES + " minutes.");
        return new CodeResult(true, "A new code was generated.", code, sent);
    }

    public SimpleResult verifyEmail(String email, String code) {
        Optional<User> found = store.findByEmail(email);
        if (found.isEmpty()) return new SimpleResult(false, "No account found with that email.");
        User user = found.get();
        if (user.isEmailVerified()) return new SimpleResult(true, "This account is already verified.");
        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            return new SimpleResult(false, "Incorrect verification code.");
        }
        if (user.getVerificationExpiresAtMillis() != null && System.currentTimeMillis() > user.getVerificationExpiresAtMillis()) {
            return new SimpleResult(false, "This code has expired. Request a new one.");
        }
        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationExpiresAtMillis(null);
        store.save(user);
        return new SimpleResult(true, "Email verified! You can now log in.");
    }

    public LoginResult login(String identifier, String password) {
        Optional<User> found = store.findByEmailOrUsername(identifier);
        if (found.isEmpty()) return LoginResult.failure("No account found with that username/email.");
        User user = found.get();
        if (!PasswordHasher.matches(password, user.getPasswordSalt(), user.getPasswordHash())) {
            return LoginResult.failure("Incorrect password.");
        }
        if (!user.isEmailVerified()) {
            return LoginResult.failure("Please verify your email before logging in.");
        }
        return new LoginResult(true, "Welcome back, " + user.getUsername() + "!", user);
    }

    public CodeResult requestPasswordReset(String email) {
        Optional<User> found = store.findByEmail(email);
        if (found.isEmpty()) return CodeResult.failure("No account found with that email.");
        User user = found.get();
        String code = generateCode();
        user.setResetCode(code);
        user.setResetExpiresAtMillis(System.currentTimeMillis() + CODE_VALIDITY_MINUTES * 60_000L);
        store.save(user);
        boolean sent = tryEmail(email, "Reset your DnD Campaign Manager password",
            "Your password reset code is: " + code + "\n\nIt expires in " + CODE_VALIDITY_MINUTES + " minutes.");
        return new CodeResult(true, "Enter the reset code and your new password.", code, sent);
    }

    public SimpleResult resetPassword(String email, String code, String newPassword, String confirmPassword) {
        Optional<User> found = store.findByEmail(email);
        if (found.isEmpty()) return new SimpleResult(false, "No account found with that email.");
        User user = found.get();
        if (user.getResetCode() == null || !user.getResetCode().equals(code)) {
            return new SimpleResult(false, "Incorrect reset code.");
        }
        if (user.getResetExpiresAtMillis() != null && System.currentTimeMillis() > user.getResetExpiresAtMillis()) {
            return new SimpleResult(false, "This code has expired. Request a new one.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            return new SimpleResult(false, "Password must be at least 6 characters.");
        }
        if (!newPassword.equals(confirmPassword)) {
            return new SimpleResult(false, "Passwords do not match.");
        }
        String salt = PasswordHasher.generateSalt();
        user.setPasswordSalt(salt);
        user.setPasswordHash(PasswordHasher.hash(newPassword, salt));
        user.setResetCode(null);
        user.setResetExpiresAtMillis(null);
        store.save(user);
        return new SimpleResult(true, "Password reset. You can now log in.");
    }

    private boolean tryEmail(String to, String subject, String body) {
        try {
            emailService.send(to, subject, body);
            return true;
        } catch (Exception e) {
            // SMTP not configured (or a transient failure) - the caller falls back to showing
            // the code on-screen so the flow stays usable without real email credentials.
            return false;
        }
    }

    private static String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
