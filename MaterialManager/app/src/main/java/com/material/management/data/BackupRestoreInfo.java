package com.material.management.data;

import android.os.Parcel;
import android.os.Parcelable;

public class BackupRestoreInfo implements Parcelable {
    public String mMsg;
    public int mProgress;
    
    public static final Creator<BackupRestoreInfo> CREATOR = new Creator<BackupRestoreInfo>() {

        @Override
        public BackupRestoreInfo createFromParcel(Parcel in) {
            BackupRestoreInfo obj = new BackupRestoreInfo();
            
            obj.setMsg(in.readString());
            obj.setProgress(in.readInt());
            
            return obj;
        }

        @Override
        public BackupRestoreInfo[] newArray(int arg0) {            
            return new BackupRestoreInfo[arg0];
        }
        
    };

    public void setMsg(String mMsg) {
        this.mMsg = mMsg;
    }

    public void setProgress(int mProgress) {
        this.mProgress = mProgress;
    }

    public int getProgress() {
        return mProgress;
    }

    public String getMsg() {
        return mMsg;
    }

    @Override
    public int describeContents() {        
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mMsg);
        dest.writeInt(mProgress);        
    }
}
