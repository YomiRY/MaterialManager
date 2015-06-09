package com.material.management.data;

import android.os.Parcel;
import android.os.Parcelable;

public class StoreData implements Parcelable {
    private String mStoreName;
    private String mStoreRef;
    private String mStoreLat;
    private String mStoreLong;
    private String mStoreAddress;
    private String mStorePhone;
    private String mStoreRate;
    private String mStoreServiceTime;
    /* Currently, we don't parcel the below three fields. */
    private String mPhotoReference;
    private String mPhotoWidth;
    private String mPhotoHeight;

    public String getPhotoHeight() {
        return mPhotoHeight;
    }

    public void setPhotoHeight(String height) {
        this.mPhotoHeight = height;
    }

    public String getPhotoWidth() {
        return mPhotoWidth;
    }

    public void setPhotoWidth(String width) {
        this.mPhotoWidth = width;
    }

    public String getPhotoReference() {
        return mPhotoReference;
    }

    public void setPhotoReference(String photoReference) {
        this.mPhotoReference = photoReference;
    }

    public String getStoreServiceTime() {
        return mStoreServiceTime;
    }

    public void setStoreServiceTime(String mStoreServiceTime) {
        this.mStoreServiceTime = mStoreServiceTime;
    }

    public String getStoreRate() {
        return mStoreRate;
    }

    public void setStoreRate(String mStoreRate) {
        this.mStoreRate = mStoreRate;
    }

    public String getStoreLong() {
        return mStoreLong;
    }

    public void setStoreLong(String mStoreLong) {
        this.mStoreLong = mStoreLong;
    }

    public String getStoreLat() {
        return mStoreLat;
    }

    public void setStoreLat(String mStoreLat) {
        this.mStoreLat = mStoreLat;
    }

    public String getStoreName() {
        return mStoreName;
    }

    public void setStoreName(String mStoreName) {
        this.mStoreName = mStoreName;
    }

    public String getStoreRef() {
        return mStoreRef;
    }

    public void setStoreRef(String mStoreRef) {
        this.mStoreRef = mStoreRef;
    }

    public String getStoreAddress() {
        return mStoreAddress;
    }

    public void setStoreAddress(String mStoreAddress) {
        this.mStoreAddress = mStoreAddress;
    }

    public String getStorePhone() {
        return mStorePhone;
    }

    public void setStorePhone(String mStorePhone) {
        this.mStorePhone = mStorePhone;
    }

    public static final Parcelable.Creator<StoreData> CREATOR= new Parcelable.Creator<StoreData>() {
        public StoreData createFromParcel(Parcel p) {
            StoreData storeData = new StoreData();

            storeData.setStoreName(p.readString());
            storeData.setStoreRef(p.readString());
            storeData.setStoreLat(p.readString());
            storeData.setStoreLong(p.readString());
            storeData.setStoreAddress(p.readString());
            storeData.setStorePhone(p.readString());
            storeData.setStoreRate(p.readString());
            storeData.setStoreServiceTime(p.readString());

            return storeData;
        }

        @Override
        public StoreData[] newArray(int size) {
            return new StoreData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mStoreName);
        dest.writeString(mStoreRef);
        dest.writeString(mStoreLat);
        dest.writeString(mStoreLong);
        dest.writeString(mStoreAddress);
        dest.writeString(mStorePhone);
        dest.writeString(mStoreRate);
        dest.writeString(mStoreServiceTime);

    }
}
