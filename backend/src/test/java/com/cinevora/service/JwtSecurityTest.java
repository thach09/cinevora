package com.cinevora.service;
import com.cinevora.security.JwtService;
import com.cinevora.entity.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class JwtSecurityTest {
    private User user() { var user = new User(); user.setUsername("security-test"); user.setRole(Role.CUSTOMER); return user; }
    @Test void signatureExpiryAndCredentialEpochAreEnforced() {
        String key = java.util.UUID.randomUUID()+"-"+java.util.UUID.randomUUID();
        var jwt = new JwtService(key, 900); var user = user(); String token = jwt.generate(user);
        assertTrue(jwt.isValid(token)); assertTrue(jwt.matchesUser(token,user));
        user.invalidateCredentials(); assertFalse(jwt.matchesUser(token,user));
        assertFalse(new JwtService(java.util.UUID.randomUUID()+"-"+java.util.UUID.randomUUID(),900).isValid(token));
        assertFalse(jwt.isValid(new JwtService(key,-1).generate(user)));
        for (String invalid : new String[]{"", "broken", "a".repeat(5000), token.substring(0, token.lastIndexOf('.')+1)+"AAAA"}) assertFalse(jwt.isValid(invalid));
    }
    @Test void bcryptDoesNotSilentlyTruncateMultibytePasswords() {
        var encoder = new com.cinevora.security.BoundedBCryptPasswordEncoder();
        assertThrows(com.cinevora.exception.BusinessException.class, () -> encoder.encode("\u20ac".repeat(25)));
        String hash = encoder.encode("Valid-password-9");
        assertTrue(encoder.matches("Valid-password-9",hash)); assertFalse(encoder.matches("\u20ac".repeat(25),hash));
    }
}
