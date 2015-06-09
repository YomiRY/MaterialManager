package com.material.management.dialog;

import com.material.management.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

public class SelectPhotoDialog extends AlertDialog.Builder {
    private String mTitle;
    private Context mContext;
    private boolean mIsShown;
    private String[] mItems;    
    private DialogInterface.OnClickListener mListener;

    public SelectPhotoDialog(Context context, String title, String[] items, DialogInterface.OnClickListener listener) {
        super(context, R.style.AlertDialogTheme);
        this.mTitle = title;        
        this.mContext = context;
        this.mListener = listener;
        this.mItems = items;
        initView();
    }

    private void initView() {
        this.setItems(mItems, mListener);
        this.setTitle(mTitle);
    }
    
    @Override
    public AlertDialog show() {
        mIsShown = true;
        AlertDialog dialog = super.show();
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        window.setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
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
