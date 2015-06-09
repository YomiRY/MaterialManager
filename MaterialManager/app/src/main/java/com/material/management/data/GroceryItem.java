package com.material.management.data;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Calendar;

public class GroceryItem extends GlobalSearchData implements Parcelable {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroceryType() {
        return groceryType;
    }

    public void setGroceryType(String groceryType) {
        this.groceryType = groceryType;
    }

    public String getGroceryPicPath() {
        return groceryPicPath;
    }

    public void setGroceryPicPath(String materialPicPath) {
        this.groceryPicPath = materialPicPath;
    }

    public String getBarcodeFormat() {
        return barcodeFormat;
    }

    public void setBarcodeFormat(String barcodeFormat) {
        this.barcodeFormat = barcodeFormat;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
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

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getSizeUnit() {
        return sizeUnit;
    }

    public void setSizeUnit(String sizeUnit) {
        this.sizeUnit = sizeUnit;
    }

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        if(qty == null || qty.trim().isEmpty()) {
            this.qty = "0";
        } else {
            this.qty = qty;
        }
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        if(price == null || price.trim().isEmpty()) {
            this.price = "0";
        } else {
            this.price = price;
        }
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Bitmap getGroceryPic() {
        return groceryPic;
    }

    public void setGroceryPic(Bitmap groceryPic) {
        this.groceryPic = groceryPic;
    }

    public int getGroceryListId() {
        return groceryListId;
    }

    public void setGroceryListId(int groceryListId) {
        this.groceryListId = groceryListId;
    }

    /* For global search usage. */
    protected ItemType mItemType = ItemType.GROCERY_ITEM;

    /* this field only for insert material display usage */
    private String name;
    private String groceryType;
    private int groceryListId;
    private Bitmap groceryPic;
    private String groceryPicPath;
    private String barcodeFormat;
    private String barcode;
    private Calendar purchaceDate;
    private Calendar validDate;
    private String size;
    private String sizeUnit;
    private String qty = "0";
    private String price = "0";
    private String comment;

    public static final Parcelable.Creator<GroceryItem> CREATOR
            = new Parcelable.Creator<GroceryItem>() {
        public GroceryItem createFromParcel(Parcel p) {
            return new GroceryItem(p);
        }

        @Override
        public GroceryItem[] newArray(int size) {
            return new GroceryItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(groceryType);
        dest.writeInt(groceryListId);
        dest.writeParcelable(groceryPic, flags);
        dest.writeString(groceryPicPath);
        dest.writeString(barcodeFormat);
        dest.writeString(barcode);
        dest.writeSerializable(purchaceDate);
        dest.writeSerializable(validDate);
        dest.writeString(size);
        dest.writeString(sizeUnit);
        dest.writeString(qty);
        dest.writeString(price);
        dest.writeString(comment);
//        dest.writeInt(isPurchase ? 1 : 0);
    }

    GroceryItem(Parcel p) {
        name = p.readString();
        groceryType = p.readString();
        groceryListId = p.readInt();
        groceryPic = p.readParcelable(Bitmap.class.getClassLoader());
        groceryPicPath = p.readString();
        barcodeFormat = p.readString();
        barcode = p.readString();
        purchaceDate = (Calendar)p.readSerializable();
        validDate = (Calendar)p.readSerializable();
        size = p.readString();
        sizeUnit = p.readString();
        qty = p.readString();
        price = p.readString();
        comment = p.readString();
//        isPurchase = (p.readInt() == 1) ? true : false;
   }

    public GroceryItem(){}
}
