package com.material.management.dialog;

import java.util.Calendar;

import com.material.management.R;
import com.material.management.utils.Utility;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.DatePicker;

public class DatePickerDialog extends AlertDialog.Builder {
    private Context mContext;

    private View mLayout;
    private DatePicker mDatePicker;
//  private TimePicker mTimePicker;

    private AlertDialog mDialog;
    private DialogInterface.OnClickListener mListener;
    private int mNeuBtnTextId;
    private boolean mIsShown;
    private int mBtnId;

    public DatePickerDialog(Context context, DialogInterface.OnClickListener listener, int btnId) {
        super(context, R.style.AlertDialogTheme);
        mListener = listener;
        mContext = context;
        mBtnId = btnId;
        initView();
    }

    private void initView() {
        LayoutInflater layoutInflater = (LayoutInflater) Utility.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mLayout = layoutInflater.inflate(R.layout.dialog_calendar_layout, null);
        mDatePicker = (DatePicker) mLayout.findViewById(R.id.dp_date_picker);
//      mTimePicker = (TimePicker) mLayout.findViewById(R.id.tp_time_picker);
        mNeuBtnTextId = R.string.title_date_picker_neutral_calendar;

        /* Default is spinnner mode */
        mDatePicker.setSpinnersShown(true);
        mDatePicker.setCalendarViewShown(false);
        this.setTitle(mContext.getString(R.string.title_date_picker_dialog_title));
        this.setPositiveButton(mContext.getString(R.string.title_positive_btn_label), mListener);
        this.setNeutralButton(mNeuBtnTextId, null);
        this.setNegativeButton(mContext.getString(R.string.title_negative_btn_label), null);
        this.setView(mLayout);
    }

    @Override
    public AlertDialog show() {
        mIsShown = true;
        mDialog = super.show();
        Window window = mDialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);

        /* If doesn't override the listener, then the dialog will dismiss after the button pressed */
        mDialog.getButton(Dialog.BUTTON_NEUTRAL).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mNeuBtnTextId == R.string.title_date_picker_neutral_calendar) {
                    mNeuBtnTextId = R.string.title_date_picker_neutral_spinner;
                    mDatePicker.setSpinnersShown(false);
                    mDatePicker.setCalendarViewShown(true);
                    mDialog.getButton(Dialog.BUTTON_NEUTRAL).setText(
                            mContext.getString(R.string.title_date_picker_neutral_spinner));
                } else if (mNeuBtnTextId == R.string.title_date_picker_neutral_spinner) {
                    mNeuBtnTextId = R.string.title_date_picker_neutral_calendar;
                    mDatePicker.setSpinnersShown(true);
                    mDatePicker.setCalendarViewShown(false);
                    mDialog.getButton(Dialog.BUTTON_NEUTRAL).setText(
                            mContext.getString(R.string.title_date_picker_neutral_calendar));
                }
            }
        });

        return mDialog;
    }

    public boolean isDialogShowing() {
        return mIsShown;
    }

    public void setShowState(boolean isShowing) {
        mIsShown = isShowing;
    }

    public Calendar getChooseCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(mDatePicker.getYear(), mDatePicker.getMonth(), mDatePicker.getDayOfMonth());
        return calendar;
    }

    public int getId() {
        return mBtnId;
    }
}
