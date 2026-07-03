package com.qa.framework.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.stream.Collectors;

public class InventoryPage extends BasePage {

    private static final By INVENTORY_LIST = By.cssSelector("[data-test='inventory-list']");
    private static final By INVENTORY_ITEMS = By.cssSelector("[data-test='inventory-item']");
    private static final By ITEM_NAMES = By.cssSelector("[data-test='inventory-item-name']");
    private static final By ITEM_PRICES = By.cssSelector("[data-test='inventory-item-price']");
    private static final By SORT_CONTAINER = By.cssSelector("[data-test='product-sort-container']");
    private static final By CART_LINK = By.cssSelector("[data-test='shopping-cart-link']");
    private static final By CART_BADGE = By.cssSelector("[data-test='shopping-cart-badge']");
    private static final By MENU_BUTTON = By.id("react-burger-menu-btn");
    private static final By LOGOUT_LINK = By.id("logout_sidebar_link");

    public InventoryPage(WebDriver driver) {
        super(driver);
    }

    public boolean isAt() {
        return isElementPresent(INVENTORY_LIST);
    }

    public int getProductCount() {
        waitForVisible(INVENTORY_LIST);
        return driver.findElements(INVENTORY_ITEMS).size();
    }

    public List<String> getDisplayedProductNames() {
        waitForVisible(INVENTORY_LIST);
        return driver.findElements(ITEM_NAMES).stream().map(WebElement::getText).collect(Collectors.toList());
    }

    public List<Double> getDisplayedProductPrices() {
        waitForVisible(INVENTORY_LIST);
        return driver.findElements(ITEM_PRICES).stream()
                .map(e -> Double.parseDouble(e.getText().replace("$", "")))
                .collect(Collectors.toList());
    }

    public void sortBy(String optionValue) {
        Select select = new Select(waitForVisible(SORT_CONTAINER));
        select.selectByValue(optionValue);
    }

    public void addProductToCart(String productSlug) {
        click(By.cssSelector("[data-test='add-to-cart-" + productSlug + "']"));
    }

    public void removeProductFromCart(String productSlug) {
        click(By.cssSelector("[data-test='remove-" + productSlug + "']"));
    }

    public boolean isCartBadgePresent() {
        return isElementPresent(CART_BADGE);
    }

    public int getCartBadgeCount() {
        if (!isCartBadgePresent()) {
            return 0;
        }
        return Integer.parseInt(getText(CART_BADGE));
    }

    public CartPage goToCart() {
        click(CART_LINK);
        return new CartPage(driver);
    }

    public ProductDetailPage openProductDetail(String productName) {
        click(By.xpath("//div[@data-test='inventory-item-name' and text()='" + productName + "']"));
        return new ProductDetailPage(driver);
    }

    public LoginPage logout() {
        click(MENU_BUTTON);
        click(LOGOUT_LINK);
        return new LoginPage(driver);
    }
}
