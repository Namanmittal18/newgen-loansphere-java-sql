package com.newgen.loanorigination;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

public class StandaloneTestRunner {

    public static void main(String[] args) {
        List<Class<?>> testClasses = List.of(
                FinancialCalculationServiceTest.class,
                CustomerValidatorTest.class,
                ProductValidatorTest.class,
                WorkflowValidatorTest.class,
                LoanApplicationServiceTest.class,
                DisbursementServiceTest.class,
                OverdueReviewSchedulerTest.class,
                CoreBankingSchedulerTest.class
        );

        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;

        System.out.println("===============================================================================");
        System.out.println("                 LOANSPHERE STANDALONE JUNIT TEST RUNNER                       ");
        System.out.println("===============================================================================");

        for (Class<?> clazz : testClasses) {
            String classDesc = clazz.isAnnotationPresent(DisplayName.class)
                    ? clazz.getAnnotation(DisplayName.class).value()
                    : clazz.getSimpleName();
            System.out.println("\nExecuting: " + classDesc + " (" + clazz.getSimpleName() + ")");

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
                        failedTests++;
                        Throwable cause = t.getCause() != null ? t.getCause() : t;
                        System.out.println("  [FAIL] " + testDesc + " -> " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
                        cause.printStackTrace(System.out);
                    }
                }
            }
        }

        System.out.println("\n===============================================================================");
        System.out.println(String.format("TEST RUN SUMMARY: Total: %d, Passed: %d, Failed: %d, Skipped: 0",
                totalTests, passedTests, failedTests));
        System.out.println("===============================================================================");

        if (failedTests > 0) {
            System.exit(1);
        }
    }
}
