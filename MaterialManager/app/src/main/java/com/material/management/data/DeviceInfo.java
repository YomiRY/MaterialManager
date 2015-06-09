package com.material.management.data;

public class DeviceInfo {
    /* Always used in "android" */
    private String mPlatformType = "android";
    private String mPlatformVersion;
    private String mAppVersion;
    private String mLanguage;
    private String mLocale;
    private String mDevice;

    public String getAppVersion() {
        return mAppVersion;
    }

    public void setAppVersion(String mAppVersion) {
        this.mAppVersion = mAppVersion;
    }

    public String getPlatformType() {
        return mPlatformType;
    }

    public void setPlatformType(String mPlatformType) {
        this.mPlatformType = mPlatformType;
    }

    public String getPlatformVersion() {
        return mPlatformVersion;
    }

    public void setPlatformVersion(String mPlatformVersionNum) {
        this.mPlatformVersion = mPlatformVersionNum;
    }

    public String getLanguage() {
        return mLanguage;
    }

    public void setLanguage(String mLanguage) {
        this.mLanguage = mLanguage;
    }

    public String getLocale() {
        return mLocale;
    }

    public void setLocale(String mLocale) {
        this.mLocale = mLocale;
    }

    public String getDevice() {
        return mDevice;
    }

    public void setDevice(String mDevice) {
        this.mDevice = mDevice;
    }
}