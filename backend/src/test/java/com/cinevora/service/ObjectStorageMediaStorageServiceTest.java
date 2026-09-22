package com.cinevora.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ObjectStorageMediaStorageServiceTest {
    @Test void realSdkUploadsSignedBytesAndDeletesOnlyOwnedGeneratedKeys() throws Exception {
        var objects = new ConcurrentHashMap<String, byte[]>();
        var requests = new AtomicInteger();
        var signed = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            if (exchange.getRequestHeaders().getFirst("Authorization").startsWith("AWS4-HMAC-SHA256")) signed.incrementAndGet();
            String path = exchange.getRequestURI().getPath();
            if (exchange.getRequestMethod().equals("PUT")) {
                objects.put(path, exchange.getRequestBody().readAllBytes());
                exchange.getResponseHeaders().add("ETag", "\"test-etag\"");
                exchange.sendResponseHeaders(200, -1);
            } else {
                objects.remove(path); exchange.sendResponseHeaders(204, -1);
            }
            exchange.close();
        });
        server.start();
        try (var s3 = S3Client.builder().endpointOverride(URI.create("http://127.0.0.1:" + server.getAddress().getPort()))
                .region(Region.of("auto")).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test-access", "test-secret")))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).chunkedEncodingEnabled(false).build())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED).build()) {
            var service = new ObjectStorageMediaStorageService(s3, new PosterValidator(5242880), "test", "https://media.example.test");
            byte[] bytes = PosterValidatorTest.image("png", 2, 2);
            var stored = service.uploadPoster(new MockMultipartFile("file", "../../pwn.png", "image/png", bytes));
            assertTrue(stored.url().matches("https://media\\.example\\.test/posters/[a-f0-9-]{36}\\.png"));
            assertArrayEquals(bytes, objects.get("/test/posters/" + stored.filename()));
            service.deletePoster("https://external.test/posters/" + stored.filename());
            service.deletePoster("https://media.example.test/posters/../other.png");
            service.deletePoster("https://media.example.test.evil/posters/" + stored.filename());
            assertEquals(1, requests.get());
            service.deletePoster(stored.url());
            assertTrue(objects.isEmpty());
            assertEquals(2, signed.get());
        } finally { server.stop(0); }
    }
}
