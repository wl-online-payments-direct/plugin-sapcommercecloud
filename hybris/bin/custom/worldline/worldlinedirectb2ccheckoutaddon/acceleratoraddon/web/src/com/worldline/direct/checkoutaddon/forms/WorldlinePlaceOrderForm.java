package com.worldline.direct.checkoutaddon.forms;

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

    public boolean isCardDetailsCheck() {
        return cardDetailsCheck;
    }

    public void setCardDetailsCheck(boolean cardDetailsCheck) {
        this.cardDetailsCheck = cardDetailsCheck;
    }
}
