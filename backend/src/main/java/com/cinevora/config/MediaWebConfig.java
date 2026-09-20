package com.cinevora.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MediaWebConfig implements WebMvcConfigurer {
    private final String storageRoot;

    public MediaWebConfig(@Value("${app.media.storage-root:uploads/media}") String storageRoot) {
        this.storageRoot = storageRoot;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path posterRoot = Paths.get(storageRoot).toAbsolutePath().normalize().resolve("posters");
        registry.addResourceHandler("/media/posters/**")
                .addResourceLocations("file:" + posterRoot + "/");
    }
}
