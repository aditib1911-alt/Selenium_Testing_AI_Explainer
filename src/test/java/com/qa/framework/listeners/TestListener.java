package com.qa.framework.listeners;

import com.qa.framework.base.DriverFactory;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

public class TestListener implements ITestListener {

    private static final Logger LOGGER = LogManager.getLogger(TestListener.class);

    @Override
    public void onTestFailure(ITestResult result) {
        LOGGER.error("Test failed: {} params={}", result.getMethod().getMethodName(),
                Arrays.toString(result.getParameters()));

        WebDriver driver;
        try {
            driver = DriverFactory.getDriver();
        } catch (IllegalStateException notAUiTest) {
            return; // API tests have no WebDriver -- nothing to screenshot.
        }

        byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        Allure.addAttachment(result.getMethod().getMethodName() + "-failure",
                new ByteArrayInputStream(screenshot));
    }
}
