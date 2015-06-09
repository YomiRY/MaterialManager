package com.material.management.data;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Date;

public class GroceryListData extends GlobalSearchData implements Parcelable{

    /* For global search usage. It maybe changed as ItemType.GROCERY_HISTORY_LIST */
    protected ItemType mItemType = ItemType.GROCERY_LIST;

    private int mId;
    private String mGroceryListName = "";
    /*
    *  0 : Disable the nearby alert.
    *  1 : Enable the nearby alert.
    * */
    private int mIsAlertWhenNearBy = 0;
    private String mStoreName = "";
    private String mAddress = "";
    private String mLat = "";
    private String mLong = "";
    private String mPhone = "";
    private String mServiceTime = "";
    private Date mCheckOutTime = null;
    /* Not be stored in database*/
    private double mTotalCost = 0;

    public double getTotalCost() {
        return mTotalCost;
    }

    public void setTotalCost(double mTotalCost) {
        this.mTotalCost = mTotalCost;
    }

    public int getId() {
        return mId;
    }

    public void setId(int mId) {
        this.mId = mId;
    }

    public String getGroceryListName() {
        return mGroceryListName;
    }

    public void setGroceryListName(String mGroceryListName) {
        this.mGroceryListName = mGroceryListName;
    }

    public int getIsAlertWhenNearBy() {
        return mIsAlertWhenNearBy;
    }

    public void setIsAlertWhenNearBy(int mIsAlertWhenNearBy) {
        this.mIsAlertWhenNearBy = mIsAlertWhenNearBy;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String mAddress) {
        this.mAddress = mAddress;
    }

    public String getLat() {
        return mLat;
    }

    public void setLat(String mLat) {
        this.mLat = mLat;
    }

    public String getLong() {
        return mLong;
    }

    public void setLong(String mLong) {
        this.mLong = mLong;
    }

    public String getStoreName() {
        return mStoreName;
    }

    public void setStoreName(String mStoreName) {
        this.mStoreName = mStoreName;
    }

    public String getPhone() {
        return mPhone;
    }

    public void setPhone(String mPhone) {
        this.mPhone = mPhone;
    }

    public String getServiceTime() {
        return mServiceTime;
    }

    public void setServiceTime(String mServiceTime) {
        this.mServiceTime = mServiceTime;
    }

    public Date getCheckOutTime() {
        return mCheckOutTime;
    }

    public void setCheckOutTime(Date mCheckOutTime) {
        this.mCheckOutTime = mCheckOutTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeString(mGroceryListName);
        dest.writeInt(mIsAlertWhenNearBy);
        dest.writeString(mStoreName);
        dest.writeString(mAddress);
        dest.writeString(mLat);
        dest.writeString(mLong);
        dest.writeString(mPhone);
        dest.writeString(mServiceTime);
        dest.writeSerializable(mCheckOutTime);
        dest.writeDouble(mTotalCost);
    }

    public static final Parcelable.Creator<GroceryListData> CREATOR= new Parcelable.Creator<GroceryListData>() {
        public GroceryListData createFromParcel(Parcel p) {
            GroceryListData groceryListData = new GroceryListData();

            groceryListData.setId(p.readInt());
            groceryListData.setGroceryListName(p.readString());
            groceryListData.setIsAlertWhenNearBy(p.readInt());
            groceryListData.setStoreName(p.readString());
            groceryListData.setAddress(p.readString());
            groceryListData.setLat(p.readString());
            groceryListData.setLong(p.readString());
            groceryListData.setPhone(p.readString());
            groceryListData.setServiceTime(p.readString());
            groceryListData.setCheckOutTime((Date)p.readSerializable());
            groceryListData.setTotalCost(p.readDouble());

            return groceryListData;
        }

        @Override
        public GroceryListData[] newArray(int size) {
            return new GroceryListData[size];
        }
    };
}
