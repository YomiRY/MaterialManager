package com.material.management.dialog;

import com.material.management.R;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class MaterialMenuDialog extends AlertDialog.Builder {
    private String mTitle;
    private String[] mItems;
    private OnItemClickListener mListener;
    private boolean mIsShown;
    private ListView mListView;

    public MaterialMenuDialog(Context context, String title, String[] items, OnItemClickListener listener) {
        super(context, R.style.AlertDialogTheme);

        this.mTitle = title;
        this.mItems = items;
        this.mListener = listener;
        initView();
    }

    private void initView() {
        this.setItems(mItems, null);
        this.setTitle(mTitle);
    }

    @Override
    public AlertDialog show() {
        mIsShown = true;
        AlertDialog dialog = super.show();
        mListView = dialog.getListView();
        Window window = dialog.getWindow();

        mListView.setOnItemClickListener(mListener);

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

    public String getItemAtPos(int pos) {
        return (mListView != null) ? (String) mListView.getItemAtPosition(pos) : "";
    }
}
