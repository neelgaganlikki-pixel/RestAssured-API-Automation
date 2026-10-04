package com.neel.selfhealing.listener;

import com.neel.selfhealing.engine.SelfHealingEngine;
import com.neel.selfhealing.report.FailureCategory;
import com.neel.selfhealing.report.FailureClassifier;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG Suite and Test Listener providing lifecycle hooks for self-healing,
 * reporting, history persistence, and failure classification.
 */
public class SelfHealingTestNGListener implements ISuiteListener, ITestListener {

    @Override
    public void onStart(ISuite suite) {
        SelfHealingEngine.initialize();
    }

    @Override
    public void onFinish(ISuite suite) {
        SelfHealingEngine.shutdown();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Throwable throwable = result.getThrowable();
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getName();

        FailureCategory category;
        if (className.toLowerCase().contains("db") || className.toLowerCase().contains("database")) {
            category = FailureClassifier.classifyDbFailure(throwable, null);
        } else {
            category = FailureClassifier.classifyApiFailure(throwable, null, null);
        }

        System.out.println();
        System.out.println("[FAILURE-CLASSIFICATION]");
        System.out.println("Test: " + testName);
        System.out.println("Category: " + category);
        System.out.println("Message: " + (throwable != null ? throwable.getMessage() : "Unknown failure"));
        System.out.println("--------------------------------------------------");
    }
}

