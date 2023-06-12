package com.worldline.direct.b2bcheckoutaddon.forms;


import de.hybris.platform.b2bacceleratorfacades.order.data.B2BReplenishmentRecurrenceEnum;
import de.hybris.platform.cronjob.enums.DayOfWeek;

import java.util.Date;
import java.util.List;

public class WorldlinePlaceOrderForm {

    private String screenHeight;
    private String screenWidth;
    private Boolean navigatorJavaEnabled;
    private Boolean navigatorJavaScriptEnabled;
    private String timezoneOffset;
    private Integer colorDepth;
    private boolean termsCheck;
    private boolean cardDetailsCheck;
    private String securityCode;
    private boolean replenishmentOrder;
    private Date replenishmentStartDate;
    private Date replenishmentEndDate;
    private String nDays;
    private String nWeeks;
    private String nMonths;
    private String nthDayOfMonth;
    private B2BReplenishmentRecurrenceEnum replenishmentRecurrence;
    private List<DayOfWeek> nDaysOfWeek;


    public String getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(String screenHeight) {
        this.screenHeight = screenHeight;
    }

    public String getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(String screenWidth) {
        this.screenWidth = screenWidth;
    }

    public Boolean getNavigatorJavaEnabled() {
        return navigatorJavaEnabled;
    }

    public void setNavigatorJavaEnabled(Boolean navigatorJavaEnabled) {
        this.navigatorJavaEnabled = navigatorJavaEnabled;
    }

    public Integer getColorDepth() {
        return colorDepth;
    }

    public void setColorDepth(Integer colorDepth) {
        this.colorDepth = colorDepth;
    }

    public boolean isTermsCheck() {
        return termsCheck;
    }

    public void setTermsCheck(boolean termsCheck) {
        this.termsCheck = termsCheck;
    }

    public String getTimezoneOffset() {
        return timezoneOffset;
    }

    public void setTimezoneOffset(String timezoneOffset) {
        this.timezoneOffset = timezoneOffset;
    }

    public Boolean getNavigatorJavaScriptEnabled() {
        return navigatorJavaScriptEnabled;
    }

    public void setNavigatorJavaScriptEnabled(Boolean navigatorJavaScriptEnabled) {
        this.navigatorJavaScriptEnabled = navigatorJavaScriptEnabled;
    }

    public String getSecurityCode() {
        return securityCode;
    }


    public void setSecurityCode(String securityCode) {
        this.securityCode = securityCode;
    }

    public boolean isReplenishmentOrder() {
        return replenishmentOrder;
    }


    public void setReplenishmentOrder(boolean replenishmentOrder) {
        this.replenishmentOrder = replenishmentOrder;
    }

    public Date getReplenishmentStartDate() {
        return replenishmentStartDate;
    }


    public void setReplenishmentStartDate(Date replenishmentStartDate) {
        this.replenishmentStartDate = replenishmentStartDate;
    }

    public String getnDays() {
        return nDays;
    }


    public void setnDays(String nDays) {
        this.nDays = nDays;
    }

    public String getnWeeks() {
        return nWeeks;
    }


    public void setnWeeks(String nWeeks) {
        this.nWeeks = nWeeks;
    }

    public String getNthDayOfMonth() {
        return nthDayOfMonth;
    }


    public void setNthDayOfMonth(String nthDayOfMonth) {
        this.nthDayOfMonth = nthDayOfMonth;
    }

    public B2BReplenishmentRecurrenceEnum getReplenishmentRecurrence() {
        return replenishmentRecurrence;
    }


    public void setReplenishmentRecurrence(B2BReplenishmentRecurrenceEnum replenishmentRecurrence) {
        this.replenishmentRecurrence = replenishmentRecurrence;
    }

    public List<DayOfWeek> getnDaysOfWeek() {
        return nDaysOfWeek;
    }


    public void setnDaysOfWeek(List<DayOfWeek> nDaysOfWeek) {
        this.nDaysOfWeek = nDaysOfWeek;
    }

    public Date getReplenishmentEndDate() {
        return replenishmentEndDate;
    }

    public void setReplenishmentEndDate(Date replenishmentEndDate) {
        this.replenishmentEndDate = replenishmentEndDate;
    }

    public String getnMonths() {
        return nMonths;
    }

    public void setnMonths(String nMonths) {
        this.nMonths = nMonths;
    }

    public boolean isCardDetailsCheck() {
        return cardDetailsCheck;
    }

    public void setCardDetailsCheck(boolean cardDetailsCheck) {
        this.cardDetailsCheck = cardDetailsCheck;
    }
}
