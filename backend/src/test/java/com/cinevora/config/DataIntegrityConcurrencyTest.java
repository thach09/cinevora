package com.cinevora.config;

import com.cinevora.dto.ProfileDtos;
import com.cinevora.entity.Profile;
import com.cinevora.entity.Role;
import com.cinevora.entity.User;
import com.cinevora.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import com.cinevora.repository.MovieRepository;
import com.cinevora.repository.ProfileRepository;
import com.cinevora.repository.UserRepository;
import com.cinevora.service.ProfileService;
import com.cinevora.service.UserDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL boundaries; never points at the developer database. */
@EnabledIfEnvironmentVariable(named = "CINEVORA_DATA_DB", matches = "true")
@SpringBootTest
@ActiveProfiles("dev")
class DataIntegrityConcurrencyTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("CINEVORA_DATA_DATABASE_URL");
        if (url == null || !url.matches("jdbc:postgresql://[^/]+/cinevora_test_data[a-zA-Z0-9_]*"))
            throw new IllegalStateException("Data concurrency tests require a named isolated database");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> System.getenv("CINEVORA_TEST_DATABASE_USER"));
        registry.add("spring.datasource.password", () -> System.getenv("CINEVORA_TEST_DATABASE_PASSWORD"));
        registry.add("app.jwt.secret", () -> UUID.randomUUID() + "-" + UUID.randomUUID() + "abcdefghijklmnop");
    }

    @Autowired UserRepository users;
    @Autowired ProfileRepository profiles;
    @Autowired MovieRepository movies;
    @Autowired ProfileService profileService;
    @Autowired UserDataService userData;
    @Autowired JdbcTemplate db;

    @Test void tenParallelCreatesNeverExceedFiveProfiles() throws Exception {
        for (int trial = 0; trial < 10; trial++) {
            User user = customer();
            List<Boolean> results = parallel(10, i -> () -> {
                try {
                    profileService.create(user.getUsername(), new ProfileDtos.Request("parallel_" + i, null));
                    return true;
                } catch (BusinessException expected) { return false; }
            });
            assertEquals(4, results.stream().filter(Boolean::booleanValue).count());
            assertEquals(5, profiles.countByUser_Id(user.getId()));
        }
    }

    @Test void oneRemainingSlotAndSeparateUsersStayIndependent() throws Exception {
        User user = customer();
        for (int i = 0; i < 3; i++) profileService.create(user.getUsername(), new ProfileDtos.Request("existing_" + i, null));
        List<Boolean> results = parallel(10, i -> () -> {
            try { profileService.create(user.getUsername(), new ProfileDtos.Request("last_" + i, null)); return true; }
            catch (BusinessException expected) { return false; }
        });
        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals(5, profiles.countByUser_Id(user.getId()));

        User other = customer();
        List<Boolean> distinct = parallel(2, i -> () -> {
            User owner = i == 0 ? customer() : other;
            profileService.create(owner.getUsername(), new ProfileDtos.Request("independent", null));
            return true;
        });
        assertEquals(List.of(true, true), distinct);
    }

    @Test void favouriteAndHistoryCountersTrackSuccessfulDistinctActions() throws Exception {
        List<User> actors = new ArrayList<>();
        for (int i = 0; i < 10; i++) actors.add(customer());
        List<Long> movieIds = movies.findAll().stream().filter(movie -> movie.isActive()).limit(4).map(movie -> movie.getId()).toList();
        assertEquals(4, movieIds.size());
        for (int trial = 0; trial < 4; trial++) {
            long movieId = movieIds.get(trial);
            boolean favourite = trial < 2;
            String column = favourite ? "favourites_count" : "views";
            String table = favourite ? "favourites" : "watch_history";
            Long before = db.queryForObject("select " + column + " from movies where id=?", Long.class, movieId);
            Integer rowsBefore = db.queryForObject("select count(*) from " + table + " where movie_id=?", Integer.class, movieId);
            List<Boolean> results = parallel(10, i -> () -> {
                User actor = actors.get(i);
                if (favourite) userData.addFavourite(actor.getUsername(), null, movieId);
                else userData.addHistory(actor.getUsername(), null, movieId);
                return true;
            });
            assertEquals(10, results.stream().filter(Boolean::booleanValue).count());
            Long after = db.queryForObject("select " + column + " from movies where id=?", Long.class, movieId);
            Integer rowsAfter = db.queryForObject("select count(*) from " + table + " where movie_id=?", Integer.class, movieId);
            assertEquals(10, after - before);
            assertEquals(10, rowsAfter - rowsBefore);
        }
    }

    @Test void duplicateProfileAndFavouriteDoNotDuplicateStateOrCounter() throws Exception {
        User actor = customer();
        List<Boolean> profilesCreated = parallel(2, i -> () -> {
            try { profileService.create(actor.getUsername(), new ProfileDtos.Request("same_name", null)); return true; }
            catch (BusinessException | DataIntegrityViolationException expected) { return false; }
        });
        assertEquals(1, profilesCreated.stream().filter(Boolean::booleanValue).count());
        assertEquals(2, profiles.countByUser_Id(actor.getId()));

        long movieId = movies.findAll().stream().filter(movie -> movie.isActive()).findFirst().orElseThrow().getId();
        Long before = db.queryForObject("select favourites_count from movies where id=?", Long.class, movieId);
        List<Boolean> favouriteAdded = parallel(2, i -> () -> {
            try { userData.addFavourite(actor.getUsername(), null, movieId); return true; }
            catch (DataIntegrityViolationException expected) { return false; }
        });
        assertEquals(1, favouriteAdded.stream().filter(Boolean::booleanValue).count());
        assertEquals(1, db.queryForObject("select count(*) from favourites where user_id=? and movie_id=?", Integer.class, actor.getId(), movieId));
        assertEquals(before + 1, db.queryForObject("select favourites_count from movies where id=?", Long.class, movieId));
        userData.removeFavourite(actor.getUsername(), null, movieId);
        assertEquals(0, db.queryForObject("select count(*) from favourites where user_id=? and movie_id=?", Integer.class, actor.getId(), movieId));
        assertEquals(before, db.queryForObject("select favourites_count from movies where id=?", Long.class, movieId));
    }

    private User customer() {
        String name = "race_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        User user = new User();
        user.setUsername(name); user.setEmail(name + "@example.test"); user.setPassword("not-used-in-this-service-test");
        user.setFullName("Concurrency test"); user.setRole(Role.CUSTOMER); user.setActive(true);
        user = users.saveAndFlush(user);
        profiles.saveAndFlush(new Profile(user, "Default", true));
        return user;
    }

    private <T> List<T> parallel(int count, java.util.function.IntFunction<Callable<T>> action) throws Exception {
        try (var pool = Executors.newFixedThreadPool(count)) {
            var barrier = new CyclicBarrier(count);
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Callable<T> task = action.apply(i);
                futures.add(pool.submit(() -> { barrier.await(15, TimeUnit.SECONDS); return task.call(); }));
            }
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) results.add(future.get(30, TimeUnit.SECONDS));
            return results;
        }
    }
}
