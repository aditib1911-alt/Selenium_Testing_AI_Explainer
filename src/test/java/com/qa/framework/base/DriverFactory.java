package com.qa.framework.base;

import com.qa.framework.constants.FrameworkConstants;
import com.qa.framework.utils.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * ThreadLocal-backed WebDriver management so parallel TestNG {@code <test>} blocks
 * (one per browser) never share a driver instance across threads.
 */
public final class DriverFactory {

    private static final Logger LOGGER = LogManager.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() {
    }

    public static void initDriver(String browser) {
        LOGGER.info("Initializing driver for browser: {}", browser);
        WebDriver driver;
        String normalizedBrowser = browser == null ? "" : browser.toLowerCase();

        // SafariDriver has no headless mode and cannot run through a containerized
        // Grid node (no official Apple-sanctioned Docker image exists), so it is
        // instantiated locally rather than routed through RemoteWebDriver.
        if (FrameworkConstants.SAFARI.equals(normalizedBrowser)) {
            driver = new SafariDriver();
        } else {
            driver = createRemoteDriver(normalizedBrowser);
        }

        driver.manage().window().maximize();
        DRIVER.set(driver);
    }

    private static WebDriver createRemoteDriver(String browser) {
        MutableCapabilities options = switch (browser) {
            case FrameworkConstants.CHROME -> {
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--headless=new", "--window-size=1920,1080", "--no-sandbox", "--disable-dev-shm-usage");
                yield chromeOptions;
            }
            case FrameworkConstants.FIREFOX -> {
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                firefoxOptions.addArguments("-headless");
                yield firefoxOptions;
            }
            case FrameworkConstants.EDGE -> {
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.addArguments("--headless=new", "--window-size=1920,1080", "--no-sandbox", "--disable-dev-shm-usage");
                yield edgeOptions;
            }
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
        };

        try {
            URL hubUrl = URI.create(ConfigReader.get("grid.hub.url")).toURL();
            return new RemoteWebDriver(hubUrl, options);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid grid.hub.url", e);
        }
    }

    public static WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("Driver not initialized for this thread. Call initDriver() first.");
        }
        return driver;
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }
}
