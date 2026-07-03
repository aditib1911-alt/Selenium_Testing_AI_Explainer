package com.qa.framework.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class CartPage extends BasePage {

    private static final By CART_ITEMS = By.cssSelector("[data-test='inventory-item']");
    private static final By ITEM_NAMES = By.cssSelector("[data-test='inventory-item-name']");
    private static final By CHECKOUT_BUTTON = By.cssSelector("[data-test='checkout']");
    private static final By CONTINUE_SHOPPING_BUTTON = By.cssSelector("[data-test='continue-shopping']");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    public boolean isAt() {
        return driver.getCurrentUrl().contains("/cart.html");
    }

    public List<String> getCartItemNames() {
        return driver.findElements(ITEM_NAMES).stream().map(WebElement::getText).collect(Collectors.toList());
    }

    public int getCartItemCount() {
        return driver.findElements(CART_ITEMS).size();
    }

    public void removeItem(String productSlug) {
        click(By.cssSelector("[data-test='remove-" + productSlug + "']"));
    }

    public InventoryPage continueShopping() {
        click(CONTINUE_SHOPPING_BUTTON);
        return new InventoryPage(driver);
    }
}
