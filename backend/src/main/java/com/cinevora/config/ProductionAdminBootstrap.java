package com.cinevora.config;

import com.cinevora.entity.*;
import com.cinevora.repository.*;
import com.cinevora.service.SessionService;
import org.springframework.beans.factory.SmartInitializingSingleton;

import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Executes before the web server starts accepting requests; all changes commit together. */
@Component
@org.springframework.context.annotation.Profile("prod")
public class ProductionAdminBootstrap implements SmartInitializingSingleton {
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final SessionService sessions;
    private final PasswordEncoder encoder;
    private final Environment env;
    public ProductionAdminBootstrap(JdbcTemplate db, TransactionTemplate tx, UserRepository users,
                                    ProfileRepository profiles, SessionService sessions, PasswordEncoder encoder, Environment env) {
        this.db = db; this.tx = tx; this.users = users; this.profiles = profiles;
        this.sessions = sessions; this.encoder = encoder; this.env = env;
    }
    @Override public void afterSingletonsInstantiated() {
        tx.executeWithoutResult(status -> {
            var row = db.queryForMap("select completed_at from security_bootstrap where id=1 for update");
            // V2 defines exactly these seed identities; never usable in a prod context.
            db.update("update users set is_active=false, security_version=security_version+1 where id between 1 and 11 and is_active=true");
            db.update("update user_sessions set revoked_at=now() where user_id between 1 and 11 and revoked_at is null");
            if (row.get("completed_at") == null) {
                String username = env.getProperty("BOOTSTRAP_ADMIN_USERNAME", "");
                String email = env.getProperty("BOOTSTRAP_ADMIN_EMAIL", "");
                String password = env.getProperty("BOOTSTRAP_ADMIN_PASSWORD", "");
                if (!username.matches("[A-Za-z0-9_]{8,50}") || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") || email.length() > 100
                        || password.length() < 20 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
                        || password.toLowerCase(java.util.Locale.ROOT).contains("cinevora")
                        || password.chars().distinct().count() < 12)
                    throw new IllegalStateException("First production boot requires unique bootstrap identity and strong external secret");
                if (users.existsByUsernameIgnoreCase(username) || users.existsByEmailIgnoreCase(email))
                    throw new IllegalStateException("Bootstrap identity must be new");
                User user = new User(); user.setUsername(username); user.setEmail(email);
                user.setPassword(encoder.encode(password)); user.setFullName("Application owner");
                user.setRole(Role.ADMIN); user.setActive(true);
                user = users.saveAndFlush(user);
                profiles.save(new com.cinevora.entity.Profile(user, "Owner", true));
                db.update("update security_bootstrap set completed_at=now(), admin_user_id=? where id=1", user.getId());
            }
            Integer usable = db.queryForObject("select count(*) from users where id between 1 and 11 and is_active=true", Integer.class);
            if (usable == null || usable != 0) throw new IllegalStateException("Unsafe demo identity state");
        });
    }
}
