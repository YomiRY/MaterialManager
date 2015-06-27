package com.material.management.data;

public class GlobalSearchData {
    public static enum ItemType {
        MATERIAL_ITEM(0), GROCERY_ITEM(1), GROCERY_LIST(2), GROCERY_HISTORY_LIST(3);
        private int mValue;

        ItemType(int value) {
            mValue = value;
        }

        public int value() {
            return mValue;
        }

    };

    /* It doesn't need parcelable */
    private ItemType mItemType;
    /*
    * If mTypeTitle is null, then the object is indicated a pure data. Otherwise, it is only the group title usage.
    * It doesn't need parcelable
    * */
    private String mTypeTitle = null;

    private String mItemName = null;
    private String mItemCount = null;
    private String mItemTotalCost = null;
    private String mItemRestExpDays = null;
    private boolean mIsHead = false;
    private GroceryItem mGroceryItem = null;
    private Material mMaterial = null;

    public String getItemCount() {
        return mItemCount;
    }

    public void setItemCount(String mItemCount) {
        this.mItemCount = mItemCount;
    }

    public String getItemTotalCost() {
        return mItemTotalCost;
    }

    public void setItemTotalCost(String mItemTotalCost) {
        this.mItemTotalCost = mItemTotalCost;
    }

    public String getItemRestExpDays() {
        return mItemRestExpDays;
    }

    public void setItemRestExpDays(String mItemRestExpDays) {
        this.mItemRestExpDays = mItemRestExpDays;
    }

    public boolean isHead() {
        return mIsHead;
    }

    public void setIsHead(boolean mIsHead) {
        this.mIsHead = mIsHead;
    }

    public String getItemName() {
        return mItemName;
    }

    public void setItemName(String mItemName) {
        this.mItemName = mItemName;
    }

    public ItemType getItemType() {
       return mItemType;
    }

    public void setItemType(ItemType itemType) {
        mItemType = itemType;
    }

    public String getTypeTitle() {
        return mTypeTitle;
    }

    public void setTypeTitle(String title) {
        mTypeTitle = title;
    }

    public Material getMaterial() {
        return mMaterial;
    }

    public void setMaterial(Material mMaterial) {
        this.mMaterial = mMaterial;
    }

    public GroceryItem getGroceryItem() {
        return mGroceryItem;
    }

    public void setGroceryItem(GroceryItem mGroceryItem) {
        this.mGroceryItem = mGroceryItem;
    }
}
