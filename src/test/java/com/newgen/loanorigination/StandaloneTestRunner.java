package com.newgen.loanorigination;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public class StandaloneTestRunner {

    public static void main(String[] args) {
        List<Class<?>> testClasses = List.of(
                // Core Unit Test Suites
                FinancialCalculationServiceTest.class,
                CustomerValidatorTest.class,
                CustomerServiceTest.class,
                ProductValidatorTest.class,
                WorkflowValidatorTest.class,
                LoanApplicationServiceTest.class,
                DisbursementServiceTest.class,
                OverdueReviewSchedulerTest.class,
                CoreBankingSchedulerTest.class,

                // Transaction Rollback Integration Tests (H2 in-memory MySQL mode)
                TransactionRollbackTest.class,

                // Dedicated Live MySQL Integration Tests
                MySqlIntegrationTest.class
        );

        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;
        int skippedTests = 0;

        System.out.println("===============================================================================");
        System.out.println("                 LOANSPHERE STANDALONE JUNIT TEST RUNNER                       ");
        System.out.println("===============================================================================");

        for (Class<?> clazz : testClasses) {
            String classDesc = clazz.isAnnotationPresent(DisplayName.class)
                    ? clazz.getAnnotation(DisplayName.class).value()
                    : clazz.getSimpleName();
            System.out.println("\nExecuting: " + classDesc + " (" + clazz.getSimpleName() + ")");

            // Execute @BeforeAll
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.isAnnotationPresent(BeforeAll.class) && Modifier.isStatic(m.getModifiers())) {
                    try {
                        m.setAccessible(true);
                        m.invoke(null);
                    } catch (Throwable t) {
                        System.err.println("  [ERROR] @BeforeAll failed for " + clazz.getSimpleName() + ": " + t.getMessage());
                    }
                }
            }

            Method beforeEachMethod = null;
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.isAnnotationPresent(BeforeEach.class)) {
                    beforeEachMethod = m;
                    beforeEachMethod.setAccessible(true);
                    break;
                }
            }

            for (Method m : clazz.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Test.class)) {
                    totalTests++;
                    String testDesc = m.isAnnotationPresent(DisplayName.class)
                            ? m.getAnnotation(DisplayName.class).value()
                            : m.getName();
                    try {
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        if (beforeEachMethod != null) {
                            beforeEachMethod.invoke(instance);
                        }
                        m.setAccessible(true);
                        m.invoke(instance);
                        passedTests++;
                        System.out.println("  [PASS] " + testDesc);
                    } catch (Throwable t) {
                        Throwable cause = t.getCause() != null ? t.getCause() : t;
                        if (cause instanceof TestAbortedException) {
                            skippedTests++;
                            System.out.println("  [SKIPPED] " + testDesc + " (" + cause.getMessage() + ")");
                        } else {
                            failedTests++;
                            System.out.println("  [FAIL] " + testDesc + " -> " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
                            cause.printStackTrace(System.out);
                        }
                    }
                }
            }

            // Execute @AfterAll
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.isAnnotationPresent(AfterAll.class) && Modifier.isStatic(m.getModifiers())) {
                    try {
                        m.setAccessible(true);
                        m.invoke(null);
                    } catch (Throwable t) {
                        System.err.println("  [ERROR] @AfterAll failed for " + clazz.getSimpleName() + ": " + t.getMessage());
                    }
                }
            }
        }

        System.out.println("\n===============================================================================");
        System.out.println(String.format("TEST RUN SUMMARY: Total: %d, Passed: %d, Failed: %d, Skipped: %d",
                totalTests, passedTests, failedTests, skippedTests));
        System.out.println("===============================================================================");

        if (failedTests > 0) {
            System.exit(1);
        }
    }
}
