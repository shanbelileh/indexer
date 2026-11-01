package com.example.fileindexer.config;


import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.ShellRunner;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.jline.JLineInputProvider;
import org.springframework.shell.jline.JLineShellRunner;
import org.springframework.shell.jline.PromptProvider;

@Configuration
class ShellStarterConfig {


    @Bean
    ShellRunner shellRunner(JLineInputProvider inputProvider, CommandParser commandParser, CommandRegistry commandRegistry) {
        return new JLineShellRunner(inputProvider, commandParser, commandRegistry);
    }

    @Bean
    ApplicationRunner fileIndexerShellRunner(ShellRunner shellRunner) {
        return args -> shellRunner.run(args.getSourceArgs());
    }

    @Bean
    PromptProvider promptProvider() {
        return () -> new AttributedStringBuilder()
                .append("indexer", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold())
                .append(" :> ", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                .toAttributedString();
    }
}
