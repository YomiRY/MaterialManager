package com.material.management.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.material.management.R;
import com.material.management.data.GroceryItem;
import com.material.management.utils.Utility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ReceiptDialog extends AlertDialog.Builder {
    private final Context mContext;
    private final ArrayList<GroceryItem> mGroceryItemList;
    private final DialogInterface.OnClickListener mListener;
    private final String mTitle;
    private boolean mIsShown;
    private Date mSpecificDate = null;

    public ReceiptDialog(Context context, String title, String receiptTitle, ArrayList<GroceryItem> groceryItemList, DialogInterface.OnClickListener listener, boolean isOnlyOneConfirm, Date date) {
        super(context, R.style.AlertDialogTheme);

        mContext = context;
        mTitle = receiptTitle;
        mGroceryItemList = groceryItemList;
        mListener = listener;

        this.setTitle(title);
        this.setPositiveButton(mContext.getString(R.string.title_positive_btn_label), mListener);
        /* For only show grocery list history. */
        if (!isOnlyOneConfirm) {
            this.setNegativeButton(mContext.getString(R.string.title_negative_btn_label), null);
        } else {
            this.mSpecificDate = date;
        }

        initView();
    }

    private void initView() {
        Calendar nowDate = Calendar.getInstance();

        if (mSpecificDate != null)
            nowDate.setTime(mSpecificDate);

        LayoutInflater layoutInflater = (LayoutInflater) Utility.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View layout = layoutInflater.inflate(R.layout.view_checkout, null);
        LinearLayout llCheckoutItems = (LinearLayout) layout.findViewById(R.id.ll_checkout_items);
        TextView tvReceiptTitle = (TextView) layout.findViewById(R.id.tv_receipt_title);
        TextView tvDate = (TextView) layout.findViewById(R.id.tv_date);
        TextView tvTime = (TextView) layout.findViewById(R.id.tv_time);
        TextView tvNumOfItem = (TextView) layout.findViewById(R.id.tv_num_of_items);
        TextView tvTotalValue = (TextView) layout.findViewById(R.id.tv_total_value);
        double totalValue = 0;
        int itemCount = 0;

        tvReceiptTitle.setText(mTitle);
        tvDate.setText(Utility.transDateToString("yyyy - MM - dd", nowDate.getTime()));
        tvTime.setText(Utility.transDateToString("hh:mm a", nowDate.getTime()));
        for (GroceryItem item : mGroceryItemList) {
            totalValue += Long.parseLong(item.getQty()) * Double.parseDouble(item.getPrice());
            View checkOutItemLayout = layoutInflater.inflate(R.layout.view_checkout_item, null);
            TextView tvQty = (TextView) checkOutItemLayout.findViewById(R.id.tv_qty);
            TextView tvItemName = (TextView) checkOutItemLayout.findViewById(R.id.tv_item_name);
            TextView tvItemPrice = (TextView) checkOutItemLayout.findViewById(R.id.tv_item_price);

            tvQty.setText(item.getQty());
            tvItemName.setText(item.getName());
            tvItemPrice.setText(Double.toString(Double.parseDouble(item.getPrice()) * Double.parseDouble(item.getQty())));
            llCheckoutItems.addView(checkOutItemLayout);
            ++itemCount;
        }
        tvNumOfItem.setText(Integer.toString(itemCount));
        tvTotalValue.setText("$" + totalValue);

        try {
            Method methodSetView = AlertDialog.Builder.class.getMethod("setView", View.class, int.class, int.class, int.class, int.class);

            methodSetView.invoke(this, layout, 0, 0, 0, 0);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public AlertDialog show() {
        mIsShown = true;
        AlertDialog dialog = super.show();
        Window window = dialog.getWindow();

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        return dialog;
    }

    public boolean isDialogShowing() {
        return mIsShown;
    }

    public void setShowState(boolean isShowing) {
        mIsShown = isShowing;
    }
}
