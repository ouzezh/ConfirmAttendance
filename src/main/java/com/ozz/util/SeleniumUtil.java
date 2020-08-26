package com.ozz.util;

import com.ozz.ConfirmAttendance;
import java.util.Iterator;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class SeleniumUtil {
  public static WebDriver getWebDriver() {
//    System.setProperty("webdriver.firefox.bin", ConfirmAttendance.USER_DIR + "/driver/FirefoxPortable/FirefoxPortable.exe");
//    System.setProperty("webdriver.gecko.driver", ConfirmAttendance.USER_DIR + "/driver/geckodriver.exe");
//    WebDriver driver = new FirefoxDriver();

    String driverPath = ConfirmAttendance.USER_DIR + "/driver/chromedriver.exe";
    String bin = ConfirmAttendance.USER_DIR + "/driver/ChromePortable/ChromePortable.exe";
    System.out.println("webdriver.chrome.bin: " + bin);
    System.out.println("webdriver.chrome.driver: " + driverPath);

    System.setProperty("webdriver.chrome.driver", driverPath);
    ChromeOptions options = new ChromeOptions(); 
    options.setBinary(bin);
    options.addArguments("--start-maximized");
    WebDriver driver = new ChromeDriver(options);

    return driver;
  }

  public static void closeWindow(WebDriver driver) {
    String currentWindow = driver.getWindowHandle();// 得到当前窗口的句柄
    Set<String> handles = driver.getWindowHandles();// 得到所有窗口的句柄
    Iterator<String> it = handles.iterator();
    String newWindow = null;
    while (it.hasNext()) {
      if (currentWindow == it.next())
        continue;
      newWindow = it.next();
    }
    driver.close();
    if (newWindow != null) {
      driver.switchTo().window(newWindow);
    }
  }

  public static WebElement findElement(WebDriver driver, By by) {
    return findElement(driver, by, 5);
  }
  
  public static WebElement findElement(WebDriver driver, By by, long timeOutInSeconds) {
    WebElement ele = new WebDriverWait(driver, timeOutInSeconds).until(ExpectedConditions.presenceOfElementLocated(by));
    return ele;
  }

  public static void moveToElement(WebDriver driver, WebElement ele) {
    Actions action = new Actions(driver);
    action.moveToElement(ele).build().perform();
  }
  
  // public static boolean isElementPresent(WebDriver driver, By by) {
  // try {
  // driver.findElement(by);
  // return true;
  // } catch (NoSuchElementException e) {
  // return false;
  // }
  // }
  //
  // private boolean isAlertPresent() {
  // try {
  // driver.switchTo().alert();
  // return true;
  // } catch (NoAlertPresentException e) {
  // return false;
  // }
  // }
  //
  // private String closeAlertAndGetItsText() {
  // try {
  // Alert alert = driver.switchTo().alert();
  // String alertText = alert.getText();
  // if (acceptNextAlert) {
  // alert.accept();
  // } else {
  // alert.dismiss();
  // }
  // return alertText;
  // } finally {
  // acceptNextAlert = true;
  // }
  // }
}
