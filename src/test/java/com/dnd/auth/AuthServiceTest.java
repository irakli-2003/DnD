package com.dnd.auth;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class AuthServiceTest {

    private AuthService newService() throws IOException {
        Path usersFile = Files.createTempDirectory("dnd-auth").resolve("users.json");
        UserStore store = new UserStore(usersFile);
        // A disabled SmtpConfig (the default) makes EmailService.send() always throw
        // EmailNotConfiguredException, so tests exercise the same "code shown in-app" fallback
        // path real usage takes until SMTP credentials are supplied.
        EmailService emailService = new EmailService(new SmtpConfig());
        return new AuthService(store, emailService);
    }

    @Test
    public void registerCreatesUnverifiedUserAndReturnsCodeWhenEmailNotConfigured() throws IOException {
        AuthService auth = newService();
        AuthService.CodeResult result = auth.register("alice", "alice@example.com", "password1", "password1");
        assertTrue(result.success());
        assertFalse(result.emailSent());
        assertNotNull(result.code());
        assertEquals(6, result.code().length());
    }

    @Test
    public void registerRejectsMismatchedPasswords() throws IOException {
        AuthService auth = newService();
        AuthService.CodeResult result = auth.register("bob", "bob@example.com", "password1", "different");
        assertFalse(result.success());
    }

    @Test
    public void registerRejectsInvalidEmail() throws IOException {
        AuthService auth = newService();
        AuthService.CodeResult result = auth.register("bob", "not-an-email", "password1", "password1");
        assertFalse(result.success());
    }

    @Test
    public void registerRejectsDuplicateUsernameOrEmail() throws IOException {
        AuthService auth = newService();
        auth.register("carol", "carol@example.com", "password1", "password1");
        assertFalse(auth.register("carol", "other@example.com", "password1", "password1").success());
        assertFalse(auth.register("someoneElse", "carol@example.com", "password1", "password1").success());
    }

    @Test
    public void loginFailsBeforeVerificationAndSucceedsAfter() throws IOException {
        AuthService auth = newService();
        AuthService.CodeResult reg = auth.register("dave", "dave@example.com", "password1", "password1");

        AuthService.LoginResult beforeVerify = auth.login("dave", "password1");
        assertFalse(beforeVerify.success());

        AuthService.SimpleResult verify = auth.verifyEmail("dave@example.com", reg.code());
        assertTrue(verify.success());

        AuthService.LoginResult afterVerify = auth.login("dave", "password1");
        assertTrue(afterVerify.success());
        assertEquals("dave", afterVerify.user().getUsername());

        // Login also works by email, and wrong password is rejected.
        assertTrue(auth.login("dave@example.com", "password1").success());
        assertFalse(auth.login("dave", "wrong-password").success());
    }

    @Test
    public void verifyEmailRejectsWrongCode() throws IOException {
        AuthService auth = newService();
        auth.register("erin", "erin@example.com", "password1", "password1");
        AuthService.SimpleResult result = auth.verifyEmail("erin@example.com", "000000");
        assertFalse(result.success());
    }

    @Test
    public void passwordResetFlowChangesPassword() throws IOException {
        AuthService auth = newService();
        AuthService.CodeResult reg = auth.register("frank", "frank@example.com", "password1", "password1");
        auth.verifyEmail("frank@example.com", reg.code());

        AuthService.CodeResult resetRequest = auth.requestPasswordReset("frank@example.com");
        assertTrue(resetRequest.success());

        AuthService.SimpleResult wrongCode = auth.resetPassword("frank@example.com", "000000", "newpass1", "newpass1");
        assertFalse(wrongCode.success());

        AuthService.SimpleResult reset = auth.resetPassword(
            "frank@example.com", resetRequest.code(), "newpass1", "newpass1");
        assertTrue(reset.success());

        assertFalse(auth.login("frank", "password1").success());
        assertTrue(auth.login("frank", "newpass1").success());
    }
}
