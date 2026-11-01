package com.example.fileindexer.config;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class JLineConfig {

   /* @Bean
    public Terminal terminal() throws IOException {
        return TerminalBuilder.builder()
                .name("indexer Terminal")
                .system(true)
                .encoding(StandardCharsets.UTF_8)
                .build();
    }*/


}
