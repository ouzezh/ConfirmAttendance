package com.ozz.model;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AttendanceData {

  /**
   * returnData : {"DTL":[{"date":"2020-07-26","descrIn":"周末","weekdayName":"星期日","descrOut":"周末"}]}
   */

  private ReturnDataBean returnData;

  @Getter
  @Setter
  @ToString
  public static class ReturnDataBean {

    private Map<String, String> xdfDayType;
    private List<DTLBean> DTL;

    @Getter
    @Setter
    @ToString
    public static class DTLBean {

      /**
       * date : 2020-07-26 descrIn : 周末 weekdayName : 星期日 descrOut : 周末
       */

      private String date;
      private String descrIn;
      private String weekdayName;
      private String descrOut;
      private String TIMEBG;
      private String TIMEEND;
    }
  }
}
