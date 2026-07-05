package com.qa.framework.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class ProductDetailPage extends BasePage {

    private static final By ITEM_NAME = By.cssSelector("[data-test='inventory-item-name']");
    private static final By BACK_BUTTON = By.cssSelector("[data-test='back-to-products']");

    public ProductDetailPage(WebDriver driver) {
        super(driver);
    }

    public boolean isAt() {
        return driver.getCurrentUrl().contains("inventory-item.html");
    }

    public String getProductName() {
        return getText(ITEM_NAME);
    }

    public void addToCart() {
        click(By.cssSelector("[data-test='add-to-cart']"));
    }

    public InventoryPage backToProducts() {
        click(BACK_BUTTON);
        return new InventoryPage(driver);
    }
}
