package com.example.fileindexer;

import com.example.fileindexer.shell.IndexerCommands;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.shell.core.command.annotation.EnableCommand;

@SpringBootApplication
@ConfigurationPropertiesScan
//@EnableCommand(IndexerCommands.class)
public class FileIndexerApplication {


    public static void main(String[] args) {
        SpringApplication.run(FileIndexerApplication.class, args);
    }
}
