package com.ozz;

import com.alibaba.fastjson.JSON;
import com.ozz.model.AttendanceData;
import com.ozz.model.AttendanceData.ReturnDataBean.DTLBean;
import com.ozz.util.SeleniumUtil;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ConfirmAttendance {
  // public static final String USER_DIR = "C:/Dev/workspace/ConfirmAttendance";
  public static String USER_DIR = System.getProperty("user.dir").replaceAll("\\\\", "/");

  public static void main(String[] args) throws ParseException {
     new ConfirmAttendance().start();
//    new ConfirmAttendance().parseAttendanceJson();
  }

  private void parseAttendanceJson() throws ParseException {
    Scanner scanner = new Scanner(System.in);
    System.out.println("请输入考勤JSON：");
    String json = scanner.nextLine();
    AttendanceData data = JSON.parseObject(json, AttendanceData.class);

    // check date type
    for (Entry<String, String> en : data.getReturnData().getXdfDayType().entrySet()) {
      if(!"A".equals(en.getValue()) && !"B".equals(en.getValue())) {
        throw new RuntimeException("非法考勤类型：" + en.toString());
      }
    }

    // parse overtime date
    Map<String, Long> overtimeMap = new TreeMap<>();
    String dateFormat = "yyyy-MM-dd HH:mm:ss";
    for (DTLBean dtlBean : data.getReturnData().getDTL()) {
      if(StringUtils.isNotBlank(dtlBean.getTIMEBG()) && StringUtils.isNotBlank(dtlBean.getTIMEEND())) {
        long res = calcOvertime(DateUtils.parseDate(dtlBean.getDate() + " " + dtlBean.getTIMEBG(), dateFormat),
            DateUtils.parseDate(dtlBean.getDate() + " " + dtlBean.getTIMEEND(), dateFormat),
            "A".equals(data.getReturnData().getXdfDayType().get(dtlBean.getDate())));
        if (res > 0) {
          overtimeMap.put(dtlBean.getDate(), res);
        }
      }
    }

    // print
    System.out.println("-- overtime start --");
    int c = 0;
    for (Entry<String, Long> en : overtimeMap.entrySet()) {
      System.out.println(String.format("%s: %s -> %s", ++c, en.getKey(), en.getValue()));
    }
    System.out.println("-- overtime end --");
  }

  public void start() {
    WebDriver driver = null;
    try {
      String username = getProp("usr");
      String password = getProp("pwd");

      driver = SeleniumUtil.getWebDriver();
      login(driver, getProp("url"), username, password);

      openAttendanceFillPage(driver);

      fillOvertime(driver);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void fillOvertime(WebDriver driver) {
    LocalDate localDate = LocalDate.now();
    localDate = localDate.plusMonths(-1);
    localDate = localDate.plusDays(26 - localDate.getDayOfMonth());

    int i = 0;
    List<Pair<Integer, String>> dateList = new LinkedList<>();
    do {
      i++;
      String date = localDate.toString();
      dateList.add(0, Pair.of(i, date));

      System.out.print(String.format("add row %s: %S", i, date));
      findEle(driver, By.xpath(
          "//*[@id=\"ant-content\"]/div[1]/app-staff-leave-apply/div/nz-spin/div[2]/div/div[1]/button"))
          .click();
      sleep(200);

      System.out.print(" -> type date");
      WebElement dataEle = findEle(driver, 
          By.xpath(String.format("//*[@id=\"DataTables_Table_2\"]/tbody/tr[%d]/td[2]/input", i)));
      dataEle.clear();
      dataEle.sendKeys(date);
      dataEle.click();
      sleep(200);

      System.out.println(" -> click desc to update hour");
      findEle(driver, By.xpath(String.format("//*[@id=\"DataTables_Table_2\"]/tbody/tr[%s]/td[6]/input", i))).click();
      sleep(200);

      localDate = localDate.plusDays(1);
    } while (localDate.getDayOfMonth() != 26);

    sleep(500);
    Iterator<Pair<Integer, String>> it = dateList.iterator();
    while(it.hasNext()) {
      Pair<Integer, String> pair = it.next();
      System.out.print("check hour " + pair.getLeft());
      WebElement hourEle = findEle(driver, By.xpath(String
          .format("//*[@id=\"DataTables_Table_2\"]/tbody/tr[%d]/td[3]/select", pair.getLeft())));
      String hour = hourEle.getAttribute("value");
      System.out.println(String.format(" -> %s = %s", pair.getRight(), hour));
      if(!"0".equals(hour)) {
        it.remove();
      }
    }

    for(Pair<Integer, String> pair : dateList) {
      System.out.println(String.format("delete row %s: %S", pair.getLeft(), pair.getRight()));
      findEle(driver, By.xpath(String.format("//*[@id=\"DataTables_Table_2\"]/tbody/tr[%d]/td[7]/i", pair.getLeft()))).click();
      sleep(200);
    }

    // 点击原因，自动计算汇总时数
    System.out.println(String.format("fill end, click reason"));
    findEle(driver, By.xpath("//*[@id=\"ant-content\"]/div[1]/app-staff-leave-apply/div/nz-spin/div[2]/div/app-process-form/div/form/div[5]/div/div[2]/div/nz-input/textarea"))
        .click();
  }

  private void openAttendanceFillPage(WebDriver driver) {
    clickSelfHelp(driver);

    System.out.println("click apply for overtime");
    findEle(driver, By.xpath("//*[@id=\"staff-self-menus\"]/li[1]/a")).click();
    sleep(2000);

    System.out.println("click new overtime");
    findEle(driver, By.xpath(
        "//*[@id=\"ant-content\"]/div[1]/app-staff-leave-list/app-ex-datatable/div/app-ex-search/div/form/div[3]/button[4]"))
        .click();
    sleep(1000);

    System.out.println("click overtime type");
    findEle(driver, By.xpath(
        "//*[@id=\"ant-content\"]/div[1]/app-staff-leave-apply/div/nz-spin/div[2]/div/app-process-form/div/form/div[3]/div[1]/div[2]/div/nz-select/div/div"))
        .click();
    sleep(1000);

    System.out.println("select overtime type");
    findEle(driver, By.xpath("//*[@id=\"cdk-overlay-1\"]/div/div/ul/li[4]")).click();
    sleep(1000);
  }

  private WebElement findEle(WebDriver driver, By by) {
    WebElement ele = null;
    NoSuchElementException e = null;
    for(int i=0; i<5 && ele==null; i++) {
      try {
        ele = driver.findElement(by);
      } catch (NoSuchElementException e1) {
        e = e1;
        System.out.println("try to get element: " + by.toString());
        sleep(1000);
      }
    }
    if(e != null) {
      throw e;
    }
    return ele;
  }

  private long calcOvertime(Date begin, Date end, boolean isWorkday) {
    // 计算加班时间
    long time = end.getTime() - begin.getTime();
    if (isWorkday) {
      time = time - new Double(9.5 * 60 * 60 * 1000).longValue();
    }
    time = time / (60 * 60 * 1000);

    // 吃饭时间
    if (time <= 0) {
      time = 0;
    } else {
      if (isWorkday) {
      } else {
        if (time > 4 && time <= 8) {
          // 加班时长超过4小时不超过8小时，扣除1小时
          time = time - 1;
        } else if (time > 8 && time <= 12) {
          // 加班时长超过8小时不超过12小时，扣除2小时
          time = time - 2;
        } else if (time > 12) {
          // 超过12小时，扣除3小时
          time = time - 3;
        }
      }
    }

    System.out.println(
        String.format(
            "%s -> %s : %s",
            DateFormatUtils.format(begin, "yyyy-MM-dd HH:mm:ss"),
            DateFormatUtils.format(end, "yyyy-MM-dd HH:mm:ss"),
            time));
    return time;
  }

  private void login(WebDriver driver, String url, String username, String password) {
    System.out.println("open homepage");
    driver.get(url);
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
    sleep(5000);

    System.out.println("type username");
    findEle(driver, By.id("txtUser")).click();
    findEle(driver, By.id("txtUser")).clear();
    findEle(driver, By.id("txtUser")).sendKeys(username);

    System.out.println("type password");
    findEle(driver, By.id("txtPwd")).click();
    findEle(driver, By.id("txtPwd")).clear();
    findEle(driver, By.id("txtPwd")).sendKeys(password);
    password = null;

    System.out.println("loging...");
    findEle(driver, By.id("loginBtn")).click();

    sleep(3000);
  }

  private void clickSelfHelp(WebDriver driver) {
    System.out.println("switch to self-help iframe");
    driver.switchTo().frame(findEle(driver, By.xpath("//*[@id=\"app\"]/div/section/section/main/iframe")));

    System.out.println("click self-help");
    WebElement ele = findEle(driver, By.xpath("//*[@id=\"staff-self\"]"));
    ele.click();
    sleep(500);
  }

  private String getProp(String key) {
    String path = String.format("%s/src/main/resources/config.properties", USER_DIR);
    Properties pro = new Properties();
    try (InputStream in = new FileInputStream(path)) {
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
