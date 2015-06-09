package com.material.management.dialog;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.material.management.R;
import com.material.management.utils.BarCodeUtility;
import com.material.management.utils.Utility;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.material.management.R;
import com.material.management.utils.BarCodeUtility;
import com.material.management.utils.Utility;

public class BarcodeDialog extends AlertDialog.Builder {
    private Context mContext;
    private boolean mIsShown;
    private DialogInterface.OnClickListener mListener;
    private View mLayout;
    private TextView mTvBarcode;
    private Bitmap mBarcodeBitmap;

    public BarcodeDialog(Context context, DialogInterface.OnClickListener listener) {
        super(context, R.style.AlertDialogTheme);
        this.mContext = context;        
        this.mListener = listener;

        initView();
    }

    private void initView() {
        LayoutInflater layoutInflater = (LayoutInflater) Utility.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mLayout = layoutInflater.inflate(R.layout.dialog_barcode_layout, null);
        mTvBarcode = (TextView) mLayout.findViewById(R.id.tv_dialog_barcode);

        this.setTitle(mContext.getString(R.string.title_barcode_dialog));
        this.setPositiveButton(mContext.getString(R.string.title_positive_btn_label), mListener);
        this.setView(mLayout);
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
        window.setGravity(Gravity.CENTER);
        return dialog;
    }

    public boolean isDialogShowing() {
        return mIsShown;
    }

    public void setShowState(boolean isShowing) {
        mIsShown = isShowing;

        if (!mIsShown && (mBarcodeBitmap != null && !mBarcodeBitmap.isRecycled())) {
            mTvBarcode.setCompoundDrawables(null, null, null, null);
            Utility.releaseBitmaps(mBarcodeBitmap);
            mBarcodeBitmap = null;
        }
    }

    public void setBarcode(String barcodeFormat, String barcode) {
        try {
            mBarcodeBitmap = BarCodeUtility.encodeAsBitmap(barcode, BarcodeFormat.valueOf(barcodeFormat), 600, 300);
            Drawable barcodeDrawable = new BitmapDrawable(mContext.getResources(), mBarcodeBitmap);
            
            barcodeDrawable.setBounds(0, 0, barcodeDrawable.getIntrinsicWidth(),
                    barcodeDrawable.getIntrinsicHeight());
            mTvBarcode.setText(barcode);
            mTvBarcode.setCompoundDrawables(null, barcodeDrawable, null, null);            
        } catch (WriterException e) {            
            e.printStackTrace();
        }        
    }

}
