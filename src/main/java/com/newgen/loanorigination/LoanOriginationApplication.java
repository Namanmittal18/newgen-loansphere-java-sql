package com.newgen.loanorigination;

import com.newgen.loanorigination.console.ConsoleMenuHandler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Scanner;

@SpringBootApplication
@EnableScheduling
public class LoanOriginationApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanOriginationApplication.class, args);
    }

    /**
     * Optional CommandLineRunner: if launched with '--console' parameter,
     * launches the interactive console immediately after context startup.
     */
    @Bean
    public CommandLineRunner commandLineRunner(ConsoleMenuHandler consoleMenuHandler) {
        return args -> {
            boolean runConsole = false;
            for (String arg : args) {
                if ("--console".equalsIgnoreCase(arg)) {
                    runConsole = true;
                    break;
                }
            }
            if (runConsole) {
                try (Scanner scanner = new Scanner(System.in)) {
                    consoleMenuHandler.startConsole(scanner);
                }
            }
        };
    }
}
