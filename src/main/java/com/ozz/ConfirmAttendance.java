package com.ozz;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ozz.util.SeleniumUtil;

public class ConfirmAttendance {
  protected final Logger log = LoggerFactory.getLogger(getClass());

  // public static final String USER_DIR = "C:/Dev/workspace/ConfirmAttendance";
  public static String USER_DIR = System.getProperty("user.dir").replaceAll("\\\\", "/");

  public static void main(String[] args) {
     new ConfirmAttendance().start();
    // System.out.println(new ConfirmAttendance().encode("X", (byte)112));
  }

  public void start() {
    WebDriver driver = null;
    try {
      String username = getProp("usr");
      String password = decode(getProp("pwd"), (byte) 112);

      driver = SeleniumUtil.getWebDriver();
      login(driver, getProp("url"), username, password);

      openConfirmPage(driver);

      List<Pair<String, Double>> list = calOvertime(driver);
      if (list.isEmpty()) {
        log.debug("no record, exit...");
        return;
      }

      driver.get(getProp("url"));
      openAttendanceFillPage(driver);

      fillOvertime(driver, list);
    } catch (Exception e) {
      log.error(null, e);
      throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  protected String encode(String str, byte key) {
    byte[] bytes = str.getBytes();
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) (bytes[i] ^ key);
    }
    return Base64.getEncoder().encodeToString(bytes);
  }

  protected String decode(String str, byte key) {
    byte[] bytes = Base64.getDecoder().decode(str);
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) (bytes[i] ^ key);
    }
    return new String(bytes);
  }

  private void fillOvertime(WebDriver driver, List<Pair<String, Double>> list) {
    int i = 0;
    for (Pair<String, Double> item : list) {
      i++;
      log.debug("add row");
      driver.findElement(By.xpath("//*[@id=\"ant-content\"]/div[1]/app-staff-leave-apply/nz-spin/div[2]/div/div[3]/button")).click();
      sleep(200);

      log.debug("type date:" + item.getKey());
      WebElement dataEle = driver.findElement(By.xpath("//*[@id=\"DataTables_Table_2\"]/tbody/tr[" + i + "]/td[2]/input"));
      dataEle.clear();
      dataEle.sendKeys(item.getKey());
      sleep(200);

      log.debug("type hour:" + item.getValue());
      WebElement hourEle = driver.findElement(By.xpath("//*[@id=\"DataTables_Table_2\"]/tbody/tr[" + i + "]/td[3]/input"));
      hourEle.clear();
      hourEle.sendKeys(String.valueOf(item.getValue()));
      sleep(200);

      log.debug("show clock in time:" + item.getValue());
      dataEle.click();
      sleep(200);
      hourEle.click();
      sleep(200);
    }

    driver.findElement(By.xpath("//*[@id=\"DataTables_Table_2\"]/tbody/tr[" + i + "]/td[1]")).click();// 点击原因，自动计算汇总时数
  }

  private void openAttendanceFillPage(WebDriver driver) {
    clickSelfHelp(driver);

    log.debug("click apply for overtime");
    driver.findElement(By.xpath("//*[@id=\"staff-self-menus\"]/li[1]/a")).click();
    sleep(1000);

    log.debug("click new overtime");
    driver.findElement(By.xpath("//*[@id=\"ant-content\"]/div[1]/app-staff-leave-list/app-ex-datatable/div/app-ex-search/div/form/div[3]/button[4]")).click();
    sleep(1000);

    log.debug("click overtime type");
    driver.findElement(By.xpath("//*[@id=\"ant-content\"]/div[1]/app-staff-leave-apply/nz-spin/div[2]/div/app-process-form/div/form/div[3]/div[1]/div[2]/div/nz-select/div/div"))
          .click();
    sleep(1000);

    log.debug("select overtime type");
    driver.findElement(By.xpath("//*[@id=\"cdk-overlay-1\"]/div/div/ul/li[4]")).click();
    sleep(1000);
  }

  private List<Pair<String, Double>> calOvertime(WebDriver driver) {
    WebElement table = driver.findElement(By.xpath("//*[@id=\"ant-content\"]/div[1]/app-pag-confirm/div/div[3]/div[2]/div[2]"));

    return calOvertime(table.getText());
  }

  private List<Pair<String, Double>> calOvertime(String text) {
    List<Pair<String, Double>> list = new ArrayList<>();
    String[] lines = text.trim().split("\n");

    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.DAY_OF_MONTH, 25);
    cal.add(Calendar.MONTH, -1);

    double overtimeTotal = 0;
    String datePattern = "yyyy-MM-dd HH:mm";
    String timePattern = "^.*(\\d{2}:\\d{2}).*$";
    for (int i = 0; i < lines.length; i=i+3) {
      try {
        cal.add(Calendar.DAY_OF_MONTH, 1);

        if (i + 2 >= lines.length) {// 当前数据不完整
          log.info("[skip]" + lines[i] + (i + 1 < lines.length ? "\t" + lines[i + 1] : ""));
          continue;
        }
        if (lines[i + 1].matches(timePattern) && lines[i + 2].matches(timePattern)) {
        } else if (lines[i + 1].contains("周末") && lines[i + 2].contains("周末")) {
          continue;
        } else {// 数据格式不对
          log.info("[skip]" + lines[i] + "\t" + lines[i + 1] + "\t" + lines[i + 2]);
          continue;
        }

        // 日期
        String date = DateFormatUtils.format(cal, "yyyy-MM-dd");
        if (cal.get(Calendar.DAY_OF_MONTH) != Integer.valueOf(lines[i]).intValue()) {
          throw new RuntimeException("解析数据错误 ,line:" + i + ", text:\n" + text);
        }

        // 开始时间
        Date beginDate;
        if (lines[i + 1].contains("加班")) {// 加班
          beginDate = DateUtils.parseDate(date + " " + lines[i + 1].replaceFirst(timePattern, "$1"), datePattern);
          log.debug(lines[i] + "\t" + lines[i + 1] + "\t" + lines[i + 2]);
        } else {
          beginDate = DateUtils.parseDate(date + " 18:00", datePattern);
        }

        // 结束时间
        Date endDate = DateUtils.parseDate(date + " " + lines[i + 2].replaceFirst(timePattern, "$1"), datePattern);

        double time = ((endDate.getTime() - beginDate.getTime()) / (30 * 60 * 1000)) / 2.0;
        if (time >= 1) {
          Pair<String, Double> item = new MutablePair<>(date, time);
          overtimeTotal += time;
          list.add(item);
        }
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    // 取整
    if (String.valueOf(overtimeTotal).contains(".5")) {
      for (Pair<String, Double> item : list) {
        if (String.valueOf(item.getValue()).contains(".5")) {
          item.setValue(item.getValue() - 0.5);
          break;
        }
      }
    }
    return list;
  }

  private void openConfirmPage(WebDriver driver) {
    clickSelfHelp(driver);

    log.debug("click attendance confirm");
    driver.findElement(By.xpath("//*[@id=\"staff-self-menus\"]/li[2]/a")).click();
    sleep(500);
  }

  private void login(WebDriver driver, String url, String username, String password) {
    log.debug("open homepage");
    driver.get(url);
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
    sleep(2000);

    log.debug("type username");
    driver.findElement(By.id("txtUser")).click();
    driver.findElement(By.id("txtUser")).clear();
    driver.findElement(By.id("txtUser")).sendKeys(username);

    log.debug("type password");
    driver.findElement(By.id("txtPwd")).click();
    driver.findElement(By.id("txtPwd")).clear();
    driver.findElement(By.id("txtPwd")).sendKeys(password);
    password = null;

    log.debug("loging...");
    driver.findElement(By.id("loginBtn")).click();

    sleep(3000);
  }

  private void clickSelfHelp(WebDriver driver) {
    log.debug("click self-help");
    WebElement ele = driver.findElement(By.id("staff-self"));
    ele.click();
    sleep(500);
  }

  private String getProp(String key) {
    Properties pro = new Properties();
    try (InputStream in = getClass().getResourceAsStream("/config.properties")) {
      pro.load(in);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return pro.getProperty(key);
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
