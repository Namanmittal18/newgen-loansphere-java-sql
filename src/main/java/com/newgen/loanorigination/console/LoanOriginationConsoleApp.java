package com.newgen.loanorigination.console;

import com.newgen.loanorigination.LoanOriginationApplication;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Scanner;

/**
 * Dedicated standalone entry point for the Menu-driven Console Application.
 * Boots the application context in headless non-web mode and launches the console.
 */
public class LoanOriginationConsoleApp {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(LoanOriginationApplication.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .run(args);

        ConsoleMenuHandler handler = context.getBean(ConsoleMenuHandler.class);
        try (Scanner scanner = new Scanner(System.in)) {
            handler.startConsole(scanner);
        } finally {
            context.close();
        }
    }
}
