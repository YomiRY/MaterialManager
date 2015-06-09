package com.material.management.dialog;

import com.material.management.R;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.WindowManager.LayoutParams;
import android.view.Window;

public class LightProgressDialog extends ProgressDialog {
    
    private boolean mIsShown;
    
    private LightProgressDialog(Context context) {
        super(context, R.style.AlertDialogTheme);
    }   
   
    /* FIXME: Need to be refactor...*/
    public static LightProgressDialog getInstance(Context context) {
        LightProgressDialog dialog =new LightProgressDialog(context);        
        Window window = dialog.getWindow();
        
        dialog.setCancelable(false);
        dialog.setShowState(true);
        dialog.setIndeterminate(false);
        dialog.setProgress(0);
        dialog.setProgressStyle(STYLE_SPINNER);
        dialog.setIndeterminateDrawable(context.getResources().getDrawable(R.drawable.progress_style_spinner));
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
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
