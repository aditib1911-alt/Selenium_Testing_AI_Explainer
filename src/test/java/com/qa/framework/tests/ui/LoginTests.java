package com.qa.framework.tests.ui;

import com.qa.framework.base.BaseUiTest;
import com.qa.framework.pages.InventoryPage;
import com.qa.framework.pages.LoginPage;
import com.qa.framework.utils.ConfigReader;
import com.qa.framework.utils.TestDataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginTests extends BaseUiTest {

    @Test(description = "LOGIN-01")
    public void validLoginNavigatesToInventory() {
        InventoryPage inventoryPage = loginPage().login(
                ConfigReader.get("login.valid.username"), ConfigReader.get("login.valid.password"));

        assertThat(inventoryPage.isAt()).isTrue();
        assertThat(getDriver().getCurrentUrl()).contains("/inventory.html");
    }

    @Test(description = "LOGIN-02..05", dataProvider = "invalidLoginCombinations", dataProviderClass = TestDataProvider.class)
    public void invalidLoginShowsError(String scenarioId, String username, String password, String expectedErrorSubstring) {
        LoginPage loginPage = loginPage().loginExpectingFailure(username, password);

        assertThat(loginPage.isErrorDisplayed()).as(scenarioId).isTrue();
        assertThat(loginPage.getErrorMessage()).as(scenarioId).contains(expectedErrorSubstring);
    }

    @Test(description = "LOGIN-06")
    public void lockedOutUserShowsError() {
        LoginPage loginPage = loginPage().loginExpectingFailure(
                ConfigReader.get("login.locked.username"), ConfigReader.get("login.valid.password"));

        assertThat(loginPage.getErrorMessage()).contains("Sorry, this user has been locked out");
    }

    @Test(description = "LOGIN-07")
    public void logoutReturnsToLoginPage() {
        InventoryPage inventoryPage = loginPage().login(
                ConfigReader.get("login.valid.username"), ConfigReader.get("login.valid.password"));

        LoginPage loginPage = inventoryPage.logout();
        assertThat(loginPage.isAt()).isTrue();
    }

    @Test(description = "LOGIN-08")
    public void dismissingErrorHidesBanner() {
        LoginPage loginPage = loginPage().loginExpectingFailure("invalid_user", "wrong_password");
        assertThat(loginPage.isErrorDisplayed()).isTrue();

        loginPage.dismissError();
        assertThat(loginPage.isErrorDisplayed()).isFalse();
    }

    @Test(description = "LOGIN-09")
    public void directNavigationWithoutAuthRedirectsToLogin() {
        getDriver().get(ConfigReader.get("ui.base.url") + "/inventory.html");

        assertThat(loginPage().isErrorDisplayed()).isTrue();
        assertThat(loginPage().getErrorMessage()).contains("you are logged in");
    }
}
