package com.material.management.data;

import android.content.Context;
import android.widget.ImageView.ScaleType;

public class StreamItem {
//    public Bitmap getMaterialPicPath() {
//        return mMaterialPic;
//    }

    public String getMaterialPicPath() {
        return mMaterialPicPath;
    }

    public String getMaterialType() {
        return mMaterialType;
    }

    public ScaleType getScaleType() {
        return mScaleType;
    }

    public int getIsAsPhotoType() {
        return mIsAsPhotoType;
    }

    public boolean isExpired() {
        return mIsExpired;
    }

    String mMaterialPicPath;
//    Bitmap mMaterialPic;
    String mMaterialType;
    ScaleType mScaleType;
    int mIsAsPhotoType;
    boolean mIsExpired;

//    public StreamItem(Context c, Bitmap bitmap, String materialType, int asPhotoType, boolean isExpired) {
public StreamItem(Context c, String materialPicPath, String materialType, int asPhotoType, boolean isExpired) {
//        mMaterialPic = bitmap;
        mMaterialPicPath = materialPicPath;
        mMaterialType = materialType;
        mScaleType = ScaleType.CENTER_CROP;
        mIsAsPhotoType = asPhotoType;
        mIsExpired = isExpired;
    }
}
