package com.qa.framework.base;

import com.qa.framework.pages.LoginPage;
import com.qa.framework.utils.ConfigReader;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;

public abstract class BaseUiTest {

    @BeforeMethod(alwaysRun = true)
    @Parameters("browser")
    public void setUp(String browser) {
        DriverFactory.initDriver(browser);
        getDriver().get(ConfigReader.get("ui.base.url"));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        DriverFactory.quitDriver();
    }

    protected WebDriver getDriver() {
        return DriverFactory.getDriver();
    }

    protected LoginPage loginPage() {
        return new LoginPage(getDriver());
    }
}
