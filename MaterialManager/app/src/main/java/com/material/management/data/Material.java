package com.material.management.data;

import java.util.Calendar;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class Material implements Parcelable {
    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Calendar getPurchaceDate() {
        return purchaceDate;
    }

    public void setPurchaceDate(Calendar purchaceDate) {
        this.purchaceDate = purchaceDate;
    }

    public Calendar getValidDate() {
        return validDate;
    }

    public void setValidDate(Calendar validDate) {
        this.validDate = validDate;
    }

    public Bitmap getMaterialPic() {
        return materialPic;
    }

    public void setMaterialPic(Bitmap materialPic) {
        this.materialPic = materialPic;
    }

    public String getMaterialPicPath() {
        return materialPicPath == null ? "" : materialPicPath;
    }

    public void setMaterialPicPath(String materialPicPath) {
        this.materialPicPath = materialPicPath;
    }

    public String getMaterialPlace() {
        return materialPlace == null ? "" : materialPlace;
    }

    public void setMaterialPlace(String materialPlace) {
        this.materialPlace = materialPlace;
    }

    public String getComment() {
        return comment == null ? "" : comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getMaterialType() {
        return materialType == null ? "" : materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public int getIsAsPhotoType() {
        return isAsPhotoType;
    }

    public void setIsAsPhotoType(int isAsPhotoType) {
        this.isAsPhotoType = isAsPhotoType;
    }

    public int getNotificationDays() {
        return notificationDays;
    }

    public void setNotificationDays(int notificationDays) {
        this.notificationDays = notificationDays;
    }

    public String getResetDaysInfo() {
        return resetDaysInfo == null ? "" : resetDaysInfo;
    }

    public void setResetDaysInfo(String resetDaysInfo) {
        this.resetDaysInfo = resetDaysInfo;
    }

    public String getBarcode() {
        /* for database migration */
        return barcode == null ? "" : barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getBarcodeFormat() {
        /* for database migration */
        return barcodeFormat == null ? "" : barcodeFormat;
    }

    public void setBarcodeFormat(String barcodeFormat) {
        this.barcodeFormat = barcodeFormat;
    }

    public int getIsValidDateSetup() {
        return isValidDateSetup;
    }

    public void setIsValidDateSetup(int isValidDateSetup) {
        this.isValidDateSetup = isValidDateSetup;
    }

    public static final Parcelable.Creator<Material> CREATOR = new Parcelable.Creator<Material>() {
        public Material createFromParcel(Parcel p) {
            return new Material(p);
        }

        @Override
        public Material[] newArray(int size) {
            return new Material[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(barcodeFormat);
        dest.writeString(barcode);
        dest.writeString(materialType);
        dest.writeInt(isAsPhotoType);
        dest.writeSerializable(purchaceDate);
        dest.writeSerializable(validDate);
        dest.writeInt(isValidDateSetup);
        dest.writeInt(notificationDays);
        dest.writeParcelable(materialPic, flags);
        dest.writeString(materialPicPath);
        dest.writeString(materialPlace);
        dest.writeString(comment);
        dest.writeString(resetDaysInfo);
    }

    public Material() {
    }

    Material(Parcel p) {
        name = p.readString();
        barcodeFormat = p.readString();
        barcode = p.readString();
        materialType = p.readString();
        isAsPhotoType = p.readInt();
        purchaceDate = (Calendar) p.readSerializable();
        validDate = (Calendar) p.readSerializable();
        isValidDateSetup = p.readInt();
        notificationDays = p.readInt();
        materialPic = p.readParcelable(Bitmap.class.getClassLoader());
        materialPicPath = p.readString();
        materialPlace = p.readString();
        comment = p.readString();
        resetDaysInfo = p.readString();
    }

    /* For global search usage. */
    protected GlobalSearchData.ItemType mItemType = GlobalSearchData.ItemType.MATERIAL_ITEM;

    private String name = "";
    private String barcodeFormat = "";
    private String barcode = "";
    private String materialType = "";
    private int isAsPhotoType = 0;
    private Calendar purchaceDate;
    private Calendar validDate;
    /* defult is setup(1), otherwise is not-setup(0) */
    private int isValidDateSetup = 1;
    private int notificationDays;
    /* this field only for insert material information usage */
    private Bitmap materialPic;
    /* this field only for insert material display usage */
    private String materialPicPath = "";
    private String materialPlace = "";
    private String comment = "";

    /* Not stored in database */
    private String resetDaysInfo = "";
}
