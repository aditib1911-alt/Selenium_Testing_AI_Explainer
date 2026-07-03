package com.qa.framework.tests.ui;

import com.qa.framework.base.BaseUiTest;
import com.qa.framework.pages.CartPage;
import com.qa.framework.pages.InventoryPage;
import com.qa.framework.pages.ProductDetailPage;
import com.qa.framework.utils.ConfigReader;
import com.qa.framework.utils.TestDataProvider;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DashboardTests extends BaseUiTest {

    private static final String BACKPACK_SLUG = "sauce-labs-backpack";
    private static final String BIKE_LIGHT_SLUG = "sauce-labs-bike-light";
    private static final String BACKPACK_NAME = "Sauce Labs Backpack";

    private InventoryPage inventoryPage;

    @BeforeMethod(alwaysRun = true)
    public void loginBeforeEachTest() {
        inventoryPage = loginPage().login(
                ConfigReader.get("login.valid.username"), ConfigReader.get("login.valid.password"));
    }

    @Test(description = "DASH-01")
    public void productListingRendersOnLogin() {
        assertThat(inventoryPage.isAt()).isTrue();
        assertThat(inventoryPage.getProductCount()).isEqualTo(6);
    }

    @Test(description = "DASH-02..05", dataProvider = "sortOptions", dataProviderClass = TestDataProvider.class)
    public void sortingReordersProductList(String scenarioId, String optionValue, String mode) {
        inventoryPage.sortBy(optionValue);

        switch (mode) {
            case "name-asc" -> assertThat(inventoryPage.getDisplayedProductNames())
                    .as(scenarioId).isSortedAccordingTo(Comparator.naturalOrder());
            case "name-desc" -> assertThat(inventoryPage.getDisplayedProductNames())
                    .as(scenarioId).isSortedAccordingTo(Comparator.reverseOrder());
            case "price-asc" -> assertThat(inventoryPage.getDisplayedProductPrices())
                    .as(scenarioId).isSortedAccordingTo(Comparator.naturalOrder());
            case "price-desc" -> assertThat(inventoryPage.getDisplayedProductPrices())
                    .as(scenarioId).isSortedAccordingTo(Comparator.reverseOrder());
            default -> throw new IllegalArgumentException("Unknown sort mode: " + mode);
        }
    }

    @Test(description = "DASH-06")
    public void addSingleItemToCartUpdatesBadge() {
        inventoryPage.addProductToCart(BACKPACK_SLUG);
        assertThat(inventoryPage.getCartBadgeCount()).isEqualTo(1);
    }

    @Test(description = "DASH-07")
    public void addMultipleItemsToCartUpdatesBadgeCount() {
        inventoryPage.addProductToCart(BACKPACK_SLUG);
        inventoryPage.addProductToCart(BIKE_LIGHT_SLUG);
        assertThat(inventoryPage.getCartBadgeCount()).isEqualTo(2);
    }

    @Test(description = "DASH-08")
    public void removeItemFromInventoryPageDecrementsBadge() {
        inventoryPage.addProductToCart(BACKPACK_SLUG);
        assertThat(inventoryPage.getCartBadgeCount()).isEqualTo(1);

        inventoryPage.removeProductFromCart(BACKPACK_SLUG);
        assertThat(inventoryPage.isCartBadgePresent()).isFalse();
    }

    @Test(description = "DASH-09")
    public void cartBadgeAbsentWhenCartEmpty() {
        assertThat(inventoryPage.isCartBadgePresent()).isFalse();
        assertThat(inventoryPage.getCartBadgeCount()).isZero();
    }

    @Test(description = "DASH-10")
    public void cartPageShowsAddedItems() {
        inventoryPage.addProductToCart(BACKPACK_SLUG);

        CartPage cartPage = inventoryPage.goToCart();
        assertThat(cartPage.isAt()).isTrue();
        assertThat(cartPage.getCartItemNames()).containsExactly(BACKPACK_NAME);
    }

    @Test(description = "DASH-11")
    public void removeItemFromCartPage() {
        inventoryPage.addProductToCart(BACKPACK_SLUG);
        CartPage cartPage = inventoryPage.goToCart();

        cartPage.removeItem(BACKPACK_SLUG);
        assertThat(cartPage.getCartItemCount()).isZero();
    }

    @Test(description = "DASH-12")
    public void navigateToProductDetailPage() {
        ProductDetailPage detailPage = inventoryPage.openProductDetail(BACKPACK_NAME);

        assertThat(detailPage.isAt()).isTrue();
        assertThat(detailPage.getProductName()).isEqualTo(BACKPACK_NAME);
    }

    @Test(description = "DASH-13")
    public void addToCartFromProductDetailPage() {
        ProductDetailPage detailPage = inventoryPage.openProductDetail(BACKPACK_NAME);
        detailPage.addToCart(BACKPACK_SLUG);

        assertThat(detailPage.isAt()).isTrue();
    }

    @Test(description = "DASH-14")
    public void backNavigationPreservesCartState() {
        inventoryPage.addProductToCart(BACKPACK_SLUG);
        ProductDetailPage detailPage = inventoryPage.openProductDetail(BACKPACK_NAME);

        InventoryPage backToInventory = detailPage.backToProducts();
        assertThat(backToInventory.getCartBadgeCount()).isEqualTo(1);
    }

    @Test(description = "DASH-15")
    public void cartPersistsAcrossSortOperation() {
        inventoryPage.addProductToCart(BACKPACK_SLUG);
        inventoryPage.sortBy("za");

        assertThat(inventoryPage.getCartBadgeCount()).isEqualTo(1);
    }
}
