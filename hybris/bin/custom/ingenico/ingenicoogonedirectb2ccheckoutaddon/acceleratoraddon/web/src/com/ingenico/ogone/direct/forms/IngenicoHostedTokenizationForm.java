package com.ingenico.ogone.direct.forms;


import javax.validation.constraints.NotNull;

public class IngenicoHostedTokenizationForm {
    private String hostedTokenizationId;
    private String screenHeight;
    private String screenWidth;
    private Boolean navigatorJavaEnabled;
    private String timezoneOffset;
    private Integer colorDepth;

    @NotNull(message = "{checkout.error.hostedTokenization.height.invalid}")
    public String getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(String screenHeight) {
        this.screenHeight = screenHeight;
    }

    @NotNull(message = "{checkout.error.hostedTokenization.width.invalid}")
    public String getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(String screenWidth) {
        this.screenWidth = screenWidth;
    }

    @NotNull(message = "{checkout.error.hostedTokenization.javaEnabled.invalid}")
    public Boolean getNavigatorJavaEnabled() {
        return navigatorJavaEnabled;
    }

    public void setNavigatorJavaEnabled(Boolean navigatorJavaEnabled) {
        this.navigatorJavaEnabled = navigatorJavaEnabled;
    }

    @NotNull(message = "{checkout.error.hostedTokenization.timezone.invalid}")
    public String getTimezoneOffset() {
        return timezoneOffset;
    }

    public void setTimezoneOffset(String timezoneOffset) {
        this.timezoneOffset = timezoneOffset;
    }

    @NotNull(message = "{checkout.error.hostedTokenization.colorDepth.invalid}")
    public Integer getColorDepth() {
        return colorDepth;
    }

    public void setColorDepth(Integer colorDepth) {
        this.colorDepth = colorDepth;
    }

    public String getHostedTokenizationId() {
        return hostedTokenizationId;
    }

    public void setHostedTokenizationId(String hostedTokenizationId) {
        this.hostedTokenizationId = hostedTokenizationId;
    }
}
