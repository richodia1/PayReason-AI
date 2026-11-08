package ai.payreason.identity.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

class SecurityConfigTest {
    final SecurityConfig config = new SecurityConfig();

    @Test
    void rejectsDemoPasswordWithoutOptIn() {
        assertThrows(IllegalStateException.class, () -> config.users("admin", "admin", false));
    }

    @Test
    void permitsExplicitLocalDemoLoginWithHashedPassword() {
        var user = config.users("admin", "admin", true).loadUserByUsername("admin");
        assertTrue(PasswordEncoderFactories.createDelegatingPasswordEncoder().matches("admin", user.getPassword()));
        assertNotEquals("admin", user.getPassword());
        assertTrue(user.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
    }

    @Test
    void optInDoesNotPermitOtherWeakCredentials() {
        assertThrows(IllegalStateException.class, () -> config.users("admin", "short", true));
        assertThrows(IllegalStateException.class, () -> config.users("another-user", "admin", true));
    }

    @Test
    void rejectsBlankCredentialsInBothModes() {
        for (boolean allowLocalDemoLogin : new boolean[]{false, true}) {
            assertThrows(IllegalStateException.class, () -> config.users("", "test-password-only", allowLocalDemoLogin));
            assertThrows(IllegalStateException.class, () -> config.users("admin", "", allowLocalDemoLogin));
        }
    }

    @Test
    void acceptsStrongPasswordsInBothModes() {
        for (boolean allowLocalDemoLogin : new boolean[]{false, true}) {
            var user = config.users("reviewer", "test-password-only", allowLocalDemoLogin).loadUserByUsername("reviewer");
            assertTrue(PasswordEncoderFactories.createDelegatingPasswordEncoder().matches("test-password-only", user.getPassword()));
        }
    }
}
