package com.dnd.auth;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.*;

public class UserStoreTest {

    @Test
    public void savePersistsAcrossInstances() throws IOException {
        Path usersFile = Files.createTempDirectory("dnd-userstore").resolve("users.json");
        UserStore first = new UserStore(usersFile);
        assertTrue(first.list().isEmpty());

        User user = new User();
        user.setUsername("gwen");
        user.setEmail("gwen@example.com");
        first.save(user);

        UserStore second = new UserStore(usersFile);
        assertEquals(1, second.list().size());
        Optional<User> found = second.findByEmail("gwen@example.com");
        assertTrue(found.isPresent());
        assertEquals("gwen", found.get().getUsername());
    }

    @Test
    public void saveReplacesExistingUserWithSameId() throws IOException {
        Path usersFile = Files.createTempDirectory("dnd-userstore").resolve("users.json");
        UserStore store = new UserStore(usersFile);

        User user = new User();
        user.setUsername("hank");
        user.setEmail("hank@example.com");
        store.save(user);

        user.setEmailVerified(true);
        store.save(user);

        assertEquals(1, store.list().size());
        assertTrue(store.findByUsername("hank").get().isEmailVerified());
    }

    @Test
    public void findByEmailOrUsernameMatchesEither() throws IOException {
        Path usersFile = Files.createTempDirectory("dnd-userstore").resolve("users.json");
        UserStore store = new UserStore(usersFile);
        User user = new User();
        user.setUsername("iris");
        user.setEmail("iris@example.com");
        store.save(user);

        assertTrue(store.findByEmailOrUsername("iris").isPresent());
        assertTrue(store.findByEmailOrUsername("iris@example.com").isPresent());
        assertFalse(store.findByEmailOrUsername("nobody").isPresent());
    }
}
