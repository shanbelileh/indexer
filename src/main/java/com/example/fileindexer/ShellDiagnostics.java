package com.example.fileindexer;


import java.util.Arrays;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.shell.core.ShellRunner;
import org.springframework.stereotype.Component;

@Component
class ShellDiagnostics {

    @EventListener(ApplicationReadyEvent.class)
    void onReady(ApplicationReadyEvent event) {

        /*System.out.println("Shell runners:");
        var context = event.getApplicationContext();
        context.getBeansOfType(
                ShellRunner.class
        ).forEach((name, runner) ->
                System.out.println(name + " -> " + runner.getClass())
        );*/
        /*var ctx = event.getApplicationContext();
        System.out.println("ApplicationRunner beans: "
                + Arrays.toString(ctx.getBeanNamesForType(ApplicationRunner.class)));
        System.out.println("Shell-related beans: " + Arrays.stream(ctx.getBeanDefinitionNames())
                .filter(n -> n.toLowerCase().contains("shell")).toList());*/
    }
}
