package com.qa.framework.utils;

import org.testng.annotations.DataProvider;

public final class TestDataProvider {

    private TestDataProvider() {
    }

    /**
     * scenarioId, username, password, expectedErrorSubstring
     */
    @DataProvider(name = "invalidLoginCombinations")
    public static Object[][] invalidLoginCombinations() {
        return new Object[][]{
                {"LOGIN-02", "invalid_user", ConfigReader.get("login.valid.password"), "Username and password do not match"},
                {"LOGIN-03", ConfigReader.get("login.valid.username"), "wrong_password", "Username and password do not match"},
                {"LOGIN-04", "", "", "Username is required"},
                {"LOGIN-05", ConfigReader.get("login.valid.username"), "", "Password is required"},
        };
    }

    /**
     * scenarioId, sortOptionValue, expectAscendingByName, expectByPrice
     */
    @DataProvider(name = "sortOptions")
    public static Object[][] sortOptions() {
        return new Object[][]{
                {"DASH-02", "az", "name-asc"},
                {"DASH-03", "za", "name-desc"},
                {"DASH-04", "lohi", "price-asc"},
                {"DASH-05", "hilo", "price-desc"},
        };
    }
}
