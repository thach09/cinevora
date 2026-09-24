package com.cinevora.config;
import org.springframework.context.annotation.*;
@Configuration
public class JsonLimits {
    @Bean org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer jsonReadConstraints() {
        return builder -> builder.postConfigurer(mapper -> mapper.getFactory().setStreamReadConstraints(
                com.fasterxml.jackson.core.StreamReadConstraints.builder().maxNestingDepth(30)
                        .maxStringLength(16000).maxNumberLength(100).build()));
    }
}
