package com.material.management.dialog;

import java.util.HashSet;
import com.material.management.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

public class MultiChoiceDialog extends AlertDialog.Builder {
    private String mTitle;
    private String[] mItems;
    private HashSet<String> mSelectedItems;
    private boolean mIsShown;
    private Context mContext;
    private DialogInterface.OnClickListener mListener;

    public MultiChoiceDialog(Context context, String title, String[] items, DialogInterface.OnClickListener listener) {
        super(context, R.style.AlertDialogTheme);
        this.mContext = context;
        this.mTitle = title;
        this.mItems = items;
        this.mListener = listener;
        this.mSelectedItems = new HashSet<String>();
        initView();
    }

    private void initView() {
        this.setTitle(mTitle);
        this.setMultiChoiceItems(mItems, null, new OnMultiChoiceClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                String item = mItems[which];
                if (isChecked)
                    mSelectedItems.add(item);
                else
                    mSelectedItems.remove(item);

            }
        });
        this.setPositiveButton(mContext.getString(R.string.title_positive_btn_label), mListener);
        this.setNegativeButton(mContext.getString(R.string.title_negative_btn_label), mListener);
    }

    public String[] getSelectedItemsString() {
        return mSelectedItems.toArray(new String[0]);
    }

    @Override
    public AlertDialog show() {
        mIsShown = true;
        AlertDialog dialog = super.show();
        Window window = dialog.getWindow();

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        window.setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.TOP);
        return dialog;
    }

    public boolean isDialogShowing() {
        return mIsShown;
    }

    public void setShowState(boolean isShowing) {
        mIsShown = isShowing;
    }
}