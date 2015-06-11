package com.material.management.fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

import com.android.datetimepicker.date.DatePickerDialog;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.material.management.MMFragment;
import com.material.management.MainActivity;
import com.material.management.MaterialManagerApplication;
import com.material.management.Observer;
import com.material.management.R;
import com.material.management.dialog.CropImageDialog;
import com.material.management.dialog.InputDialog;
import com.material.management.dialog.MultiChoiceDialog;
import com.material.management.dialog.SelectPhotoDialog;
import com.material.management.data.Material;
import com.material.management.utils.BarCodeUtility;
import com.material.management.utils.DBUtility;
import com.material.management.utils.FileUtility;
import com.material.management.utils.Utility;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class LoginMaterialFragment extends MMFragment implements Observer, OnItemSelectedListener,
        DialogInterface.OnClickListener, DatePickerDialog.OnDateSetListener {
    public static final String ACTION_BAR_BTN_ACTION_ADD = "add_material";
    public static final String ACTION_BAR_BTN_ACTION_CLEAR = "clear_user_input";
    private static final int REQ_CAMERA_TAKE_PIC = 1;
    private static final int REQ_SELECT_PICTURE = 2;
    //    private static final String CATEGORY_IS_INITIALIZED = "category_is_initialized";
    private static final String DATEPICKER_TAG = "datepicker";
    private static MainActivity sActivity;
    private static Pattern sMaterialNameErrorPattern = Pattern.compile(".*[\\\\/?]+.*", Pattern.DOTALL);

    private View mLayout;
    private RelativeLayout mRlPurchaceDate;
    private RelativeLayout mRlValidateDate;
    private ImageView mIvAddPhoto;
    private TextView mTvBarcode;
    private AutoCompleteTextView mActMaterialName;
    private Spinner mSpinMaterialCategory;
    private TextView mTvPurchaceDate;
    private TextView mTvValidDate;
    private EditText mEtNotificationDays;
    private AutoCompleteTextView mActMaterialPlace;
    private AutoCompleteTextView mActComment;
    private InputDialog mInputDialog;
    private MultiChoiceDialog mMultiChoiceDialog;
    private CropImageDialog mCropImgDialog;
    private SelectPhotoDialog mSelectPhotoDialog;
    private DatePickerDialog mDatePickerDialog;

    private Bitmap mNewestBitmap = null;
    private Bitmap mBarcodeBitmap = null;
    /* Store the newest recorded barcode */
    private String mBarcode = "";
    private String mBarcodeFormat = "";
    private int mCurPressDateBtnId = -1;
    private Options mOptions = null;
    private Calendar mPurchaceDate;
    private Calendar mValidDate;
    private ArrayAdapter<String> mTextHistAdapter = null;
    private ArrayAdapter<String> mCategoryAdapter = null;
    private LinkedHashSet<String> mMaterialTypes = null;
    private ArrayList<String> mTextHistoryList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        sActivity = (MainActivity) getActivity();
        mImm = (InputMethodManager) sActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        mLayout = inflater.inflate(R.layout.fragment_login_material_layout, container, false);
        boolean isInitialized = Utility.getBooleanValueForKey(Utility.CATEGORY_IS_INITIALIZED);
        mOptions = new Options();
        mOptions.inDensity = Utility.getDisplayMetrics().densityDpi;
        mOptions.inScaled = false;
        mOptions.inPurgeable = true;
        mOptions.inInputShareable = true;

        if (isInitialized)
            mMaterialTypes = new LinkedHashSet<String>();
        else {
            mMaterialTypes = new LinkedHashSet<String>(Arrays.asList(getResources().getStringArray(
                    R.array.default_material_type)));
            Utility.setBooleanValueForKey(Utility.CATEGORY_IS_INITIALIZED, true);
        }

        initView(mLayout);
        update(null);
        changeLayoutConfig(mLayout);

        Log.d(MaterialManagerApplication.TAG, "LoginMaterianFragment onCretatView()...");
        return mLayout;
    }

    private void initView(View layout) {
        mIvAddPhoto = (ImageView) layout.findViewById(R.id.iv_add_photo);
        mTvBarcode = (TextView) layout.findViewById(R.id.tv_barcode);
        mActMaterialName = (AutoCompleteTextView) layout.findViewById(R.id.act_material_name);
        mSpinMaterialCategory = (Spinner) layout.findViewById(R.id.spin_material_category);
        mRlPurchaceDate = (RelativeLayout) layout.findViewById(R.id.rl_purchace_date_layout);
        mRlValidateDate = (RelativeLayout) layout.findViewById(R.id.rl_validate_date_layout);
        mTvPurchaceDate = (TextView) layout.findViewById(R.id.tv_purchace_date);
        mTvValidDate = (TextView) layout.findViewById(R.id.tv_valid_date);
        mEtNotificationDays = (EditText) layout.findViewById(R.id.et_notification_days);
        mActMaterialPlace = (AutoCompleteTextView) layout.findViewById(R.id.act_material_place);
        mActComment = (AutoCompleteTextView) layout.findViewById(R.id.act_comment);

        mIvAddPhoto.setOnClickListener(this);
        mTvBarcode.setOnClickListener(this);
        mRlPurchaceDate.setOnClickListener(this);
        mRlValidateDate.setOnClickListener(this);
        mSpinMaterialCategory.setOnItemSelectedListener(this);
    }

    @Override
    public void onResume() {
        int isNeedDbWarnDialog = Utility.getIntValueForKey(Utility.DB_UPGRADE_FLAG_1to2);
        isNeedDbWarnDialog = isNeedDbWarnDialog + Utility.getIntValueForKey(Utility.DB_UPGRADE_FLAG_2to3);

        if (isNeedDbWarnDialog != 0) {
            AlertDialog.Builder warnDialog = new AlertDialog.Builder(sActivity, R.style.AlertDialogTheme);

            warnDialog.setTitle(getResources().getString(R.string.title_db_upgrade_dialog));
            warnDialog.setMessage(getResources().getString(R.string.title_db_upgrade_msg));
            warnDialog.setPositiveButton(getString(R.string.title_positive_btn_label), null);
            warnDialog.show();

            Utility.setIntValueForKey(Utility.DB_UPGRADE_FLAG_1to2, 0);
            Utility.setIntValueForKey(Utility.DB_UPGRADE_FLAG_2to3, 0);
        }

        sendScreenAnalytics(getString(R.string.ga_app_view_login_material_fragment));
        Log.d(MaterialManagerApplication.TAG, "LoginMaterianFragment onResume()...");
        super.onResume();
    }

    @Override
    public void onPause() {
        if (mInputDialog != null && mInputDialog.isDialogShowing())
            mInputDialog.setShowState(false);
        if (mMultiChoiceDialog != null && mMultiChoiceDialog.isDialogShowing())
            mMultiChoiceDialog.setShowState(false);
        if (mCropImgDialog != null && mCropImgDialog.isDialogShowing())
            mCropImgDialog.setShowState(false);
        if (mSelectPhotoDialog != null && mSelectPhotoDialog.isDialogShowing())
            mSelectPhotoDialog.setShowState(false);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        sActivity = null;

        Utility.releaseBitmaps(mNewestBitmap);

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initSpinnerData() {
        if (mMaterialTypes == null) {
            return;
        }

        for (String type : mMaterialTypes)
            DBUtility.insertMaterialTypeInfo(type);

        ArrayList<String> spinList = DBUtility.selectMaterialTypeInfo();

        spinList.add(getString(R.string.item_spinner_new_add));
        spinList.add(getString(R.string.item_spinner_del));

        if (spinList.size() == 2) {
            spinList.add(0, getString(R.string.item_spinner_empty));
        }

        if (mCategoryAdapter == null) {
            mCategoryAdapter = new ArrayAdapter<String>(sActivity, R.layout.view_spinner_item_layout, spinList) {
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View v = super.getDropDownView(position, convertView, parent);

                    ((TextView) v).setGravity(Gravity.CENTER);
                    changeLayoutConfig(v);

                    return v;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);

                    changeLayoutConfig(v);

                    return v;
                }
            };
        } else {
            mCategoryAdapter.clear();
            mCategoryAdapter.addAll(spinList);
        }

        mSpinMaterialCategory.setAdapter(mCategoryAdapter);
        mCategoryAdapter.notifyDataSetChanged();
    }

    private void initAutoCompleteData() {
        if (mTextHistAdapter == null) {
            mTextHistoryList = new ArrayList<String>();

            mTextHistoryList.addAll(Arrays.asList(Utility.getStringValueForKey(Utility.INPUT_TEXT_HISTORY).split(":")));

            mTextHistAdapter = new ArrayAdapter<String>(sActivity, android.R.layout.simple_dropdown_item_1line, mTextHistoryList);
        } else {
            mTextHistAdapter.clear();
            mTextHistAdapter.addAll(mTextHistoryList);
        }
        mActMaterialName.setAdapter(mTextHistAdapter);
        mActMaterialPlace.setAdapter(mTextHistAdapter);
        mActComment.setAdapter(mTextHistAdapter);
        mTextHistAdapter.notifyDataSetChanged();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case REQ_CAMERA_TAKE_PIC: {
                if (Activity.RESULT_OK == resultCode) {
                    try {
                    /* Restore to original icon */
                        mIvAddPhoto.setImageResource(R.drawable.selector_add_photo_status);
                        Utility.releaseBitmaps(mNewestBitmap);
                        mNewestBitmap = null;

                        mNewestBitmap = BitmapFactory.decodeFile(FileUtility.TEMP_PHOTO_FILE.getAbsolutePath(), mOptions);
                    } catch (OutOfMemoryError e) {
                        e.printStackTrace();
                        System.gc();
                    }

                    if (mNewestBitmap != null) {
                        mCropImgDialog = new CropImageDialog(sActivity, mNewestBitmap, this);

                        mCropImgDialog.show();
                    }
                }
            }
            break;
            case REQ_SELECT_PICTURE: {
                if (Activity.RESULT_OK == resultCode && intent != null && intent.getData() != null) {
                /* Restore to original icon */
                    mIvAddPhoto.setImageResource(R.drawable.selector_add_photo_status);
                    Utility.releaseBitmaps(mNewestBitmap);
                    mNewestBitmap = null;

                    Uri selectedImageUri = intent.getData();
                    String selectedImagePath = Utility.getPathFromUri(selectedImageUri);

                     /* FIXME: duplicate decode image */
                    try {
                        if (selectedImagePath != null) {
                            mNewestBitmap = BitmapFactory.decodeFile(selectedImagePath, mOptions);
                        }
                    } catch (OutOfMemoryError e) {
                    /* A workaround to avoid the OOM */
                        e.printStackTrace();
                        System.gc();
                    }

                /* Error handling */
                    if (mNewestBitmap != null) {
                        mCropImgDialog = new CropImageDialog(sActivity, mNewestBitmap, this);

                        mCropImgDialog.show();
                    }
                }
            }
            break;
            case IntentIntegrator.REQUEST_CODE: {
            /* For barcode scanner */
                IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

                if (scanResult != null) {
                    String barcode = scanResult.getContents();
                    String barcodeFormat = scanResult.getFormatName();

                    if (barcode != null && barcodeFormat != null) {
                        try {
                        /* Restore to default */
                            mBarcode = "";
                            Drawable defaultBarcodeImg = getResources().getDrawable(R.drawable.selector_barcode);

                            defaultBarcodeImg.setBounds(0, 0, defaultBarcodeImg.getIntrinsicWidth(),
                                    defaultBarcodeImg.getIntrinsicHeight());
                            mTvBarcode.setText("x xxxxxx xxxxxx x");
                            mTvBarcode.setCompoundDrawables(null, defaultBarcodeImg, null, null);
                            Utility.releaseBitmaps(mBarcodeBitmap);
                            mBarcodeBitmap = null;

                            mBarcode = barcode;
                            mBarcodeFormat = barcodeFormat;
                            mBarcodeBitmap = BarCodeUtility.encodeAsBitmap(barcode,
                                    BarcodeFormat.valueOf(mBarcodeFormat), 600, 300);
                            Drawable barcodeDrawable = new BitmapDrawable(getResources(), mBarcodeBitmap);

                            barcodeDrawable.setBounds(0, 0, barcodeDrawable.getIntrinsicWidth(),
                                    barcodeDrawable.getIntrinsicHeight());
                            mTvBarcode.setText(barcode);
                            mTvBarcode.setCompoundDrawables(null, barcodeDrawable, null, null);
                            new AutoFillTask().execute(mBarcodeFormat, mBarcode);
                        } catch (WriterException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            break;
        }
    }

    private void clearUserData() {
        if (mTvPurchaceDate == null || mTvValidDate == null
                || mIvAddPhoto == null || mActMaterialName == null
                || mActMaterialPlace == null || mActComment == null
                || mEtNotificationDays == null || mTvBarcode == null) {
            return;
        }

        mPurchaceDate = Calendar.getInstance();
        mValidDate = Calendar.getInstance();
        mNewestBitmap = ((BitmapDrawable) sActivity.getResources().getDrawable(R.drawable.ic_no_image_available)).getBitmap();
        Drawable defaultBarcodeImg = sActivity.getResources().getDrawable(R.drawable.selector_barcode);
        mBarcode = "";
        mBarcodeFormat = "";

        defaultBarcodeImg.setBounds(0, 0, defaultBarcodeImg.getIntrinsicWidth(), defaultBarcodeImg.getIntrinsicHeight());

        /* set the default time */
        mTvPurchaceDate.setText(Utility.transDateToString(mPurchaceDate.getTime()));
        mTvValidDate.setText(Utility.transDateToString(mValidDate.getTime()));
        mIvAddPhoto.setImageResource(R.drawable.selector_add_photo_status);
        mActMaterialName.setText("");
        mActMaterialPlace.setText("");
        mActComment.setText("");
        mEtNotificationDays.setText("");
        mTvBarcode.setText("x xxxxxx xxxxxx x");
        mTvBarcode.setCompoundDrawables(null, defaultBarcodeImg, null, null);
    }

    private String isAllowSave(Material material) {
        StringBuilder msg = new StringBuilder(sActivity.getResources().getString(R.string.msg_error_msg_title));
        String materialType = material.getMaterialType().trim();
        String materialName = material.getName().trim();
        boolean isAllow = true;

        if (materialType.isEmpty()
                || materialType.equals(this.getResources().getString(R.string.item_spinner_del))) {
            msg.append(sActivity.getString(R.string.msg_error_no_spercify_material_type));
            isAllow = isAllow && false;
        }
        if (material.getPurchaceDate().after(material.getValidDate())) {
            msg.append(sActivity.getString(R.string.msg_error_no_correct_valid_date));
            isAllow = isAllow && false;
        }

        if (materialName.isEmpty()) {
            msg.append(sActivity.getString(R.string.msg_error_no_material_name));
            isAllow = isAllow && false;
        }

        if (sMaterialNameErrorPattern.matcher(materialName).matches()) {
            msg.append(sActivity.getString(R.string.msg_error_special_material_naming));
            isAllow = isAllow && false;
        }

        return !isAllow ? msg.toString() : null;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        /* which < 0, then the dialog has positive/negative button */
        if (which < 0) {
            if (AlertDialog.BUTTON_POSITIVE == which) {
                if (mInputDialog != null && mInputDialog.isDialogShowing()) {
                    String input = mInputDialog.getInputString();

                    if (input.trim().isEmpty()) {
                        mSpinMaterialCategory.setSelection(0);
                        return;
                    }

                    mMaterialTypes.add(input);
                    mInputDialog.setShowState(false);
                    initSpinnerData();

                    int index = ((ArrayAdapter<String>) mSpinMaterialCategory.getAdapter()).getPosition(input);
                    mSpinMaterialCategory.setSelection(index);
                } else if (mMultiChoiceDialog != null && mMultiChoiceDialog.isDialogShowing()) {
                    final String[] selectedItems = mMultiChoiceDialog.getSelectedItemsString();

                    if (selectedItems == null || selectedItems.length == 0) {
                        mSpinMaterialCategory.setSelection(0);
                        return;
                    }

                    mMultiChoiceDialog.setShowState(false);

                    AlertDialog.Builder subDialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);

                    subDialog.setTitle(getResources().getString(R.string.msg_remind_title));
                    subDialog.setMessage(getResources().getString(R.string.msg_remind_delete_material_type_title));
                    subDialog.setPositiveButton(getString(R.string.title_positive_btn_label),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialoginterface, int i) {
                                    for (String item : selectedItems) {
                                        if (item.trim().isEmpty())
                                            continue;

                                        mMaterialTypes.remove(item);
                                        DBUtility.delMaterialTypeInfo(item);
                                        initSpinnerData();
                                        mSpinMaterialCategory.setSelection(0);
                                    }
                                }
                            });
                    subDialog.setNegativeButton(getString(R.string.title_negative_btn_label), null);
                    subDialog.show();
                } else if (mCropImgDialog != null && mCropImgDialog.isDialogShowing()) {
                    /* Recycle the original bitmap from camera intent extra. */

                    mIvAddPhoto.setImageResource(R.drawable.selector_add_photo_status);
                    Utility.releaseBitmaps(mNewestBitmap);
                    mNewestBitmap = null;


                    Bitmap bitmap = mCropImgDialog.getCroppedImage();
                    mNewestBitmap = bitmap;

                    mIvAddPhoto.setImageBitmap(bitmap);
                    mCropImgDialog.setShowState(false);
                }
            } else if (AlertDialog.BUTTON_NEGATIVE == which) {
                if (mCropImgDialog != null) {
                    mIvAddPhoto.setImageResource(R.drawable.selector_add_photo_status);
                    Utility.releaseBitmaps(mNewestBitmap);
                    mNewestBitmap = null;
                    mCropImgDialog.setShowState(false);
                } else if (mMultiChoiceDialog != null || mInputDialog != null) {
                    mSpinMaterialCategory.setSelection(0);
                }

                mDatePickerDialog = null;
            }
        } else {
            if (mSelectPhotoDialog != null) {
                mSelectPhotoDialog.setShowState(false);

                if (which == 0) {
                    /* from album */
                    Intent albumIntent = new Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                    startActivityForResult(
                            Intent.createChooser(albumIntent, getString(R.string.title_image_chooser_title)),
                            REQ_SELECT_PICTURE);
                } else if (which == 1) {
                    /* from camera */
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(FileUtility.TEMP_PHOTO_FILE));
                    startActivityForResult(takePictureIntent, REQ_CAMERA_TAKE_PIC);
                }
            }
        }

        mCropImgDialog = null;
        mMultiChoiceDialog = null;
        mInputDialog = null;
        mSelectPhotoDialog = null;
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int id = v.getId();

        switch (id) {
            case R.id.rl_purchace_date_layout:
            case R.id.rl_validate_date_layout: {
                mCurPressDateBtnId = id;
                Calendar calendar = Calendar.getInstance();
                mDatePickerDialog = DatePickerDialog.newInstance(this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

                mDatePickerDialog.show(getActivity().getFragmentManager(), DATEPICKER_TAG);
            }
            break;

            case R.id.tv_barcode: {
                IntentIntegrator integrator = new IntentIntegrator(LoginMaterialFragment.this);
                integrator.initiateScan();
            }
            break;
            case R.id.iv_add_photo: {
                mSelectPhotoDialog = new SelectPhotoDialog(sActivity, getString(R.string.title_select_photo), new String[]{
                        getString(R.string.title_select_photo_from_album),
                        getString(R.string.title_select_photo_from_camera)}, this);

                mSelectPhotoDialog.show();
            }
            break;
        }
    }

    private void updateTextHistory(String... textAry) {
        StringBuilder textHistory = new StringBuilder("");

        for (String text : textAry) {
            if (!mTextHistoryList.contains(text)) {
                mTextHistoryList.add(text);
            }
        }

        for (String text : mTextHistoryList) {
            textHistory.append(text);
            textHistory.append(":");
        }
        textHistory.deleteCharAt(textHistory.length() - 1);
        Utility.setStringValueForKey(Utility.INPUT_TEXT_HISTORY, textHistory.toString());
        initAutoCompleteData();
    }

    @Override
    public void update(Object data) {
        if (sActivity == null) {
            return;
        }

        if (data != null) {
            if (data.equals(ACTION_BAR_BTN_ACTION_ADD)) {
                Material material = new Material();
                String notificationDays = mEtNotificationDays.getText().toString().trim();

                material.setName(mActMaterialName.getText().toString());
                material.setBarcode(mBarcode);
                material.setBarcodeFormat(mBarcodeFormat);
                material.setMaterialType((String) mSpinMaterialCategory.getSelectedItem());
                material.setIsAsPhotoType(0);
                material.setMaterialPlace(mActMaterialPlace.getText().toString());
                material.setPurchaceDate(mPurchaceDate);
                material.setValidDate(mValidDate);
                /* We consider the user has set up the valid date in LoginMaterialFragment. */
                material.setIsValidDateSetup(1);
                material.setNotificationDays(notificationDays.isEmpty() ? 0 : Integer.parseInt(notificationDays));
                material.setMaterialPic(mNewestBitmap);
                material.setComment(mActComment.getText().toString());

                String msg = isAllowSave(material);
                if (msg == null) {
                    DBUtility.insertMaterialInfo(material);
                    clearUserData();
                    showToast(getString(R.string.data_save_success));
                } else {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);

                    dialog.setTitle(sActivity.getResources().getString(R.string.msg_error_dialog_title));
                    dialog.setMessage(msg);
                    dialog.setPositiveButton(getString(R.string.title_positive_btn_label), null);
                    dialog.show();
                }

                updateTextHistory(material.getName(), material.getMaterialPlace(), material.getComment());
            } else if (data.equals(ACTION_BAR_BTN_ACTION_CLEAR)) {
                clearUserData();
            }
        } else {
            clearUserData();

            initSpinnerData();

            mTextHistAdapter = null;
            initAutoCompleteData();

            sActivity.setMenuItemVisibility(R.id.action_search, false);
            sActivity.setMenuItemVisibility(R.id.menu_action_add, true);
            sActivity.setMenuItemVisibility(R.id.menu_action_cancel, true);
            sActivity.setMenuItemVisibility(R.id.menu_sort_by_date, false);
            sActivity.setMenuItemVisibility(R.id.menu_sort_by_name, false);
            sActivity.setMenuItemVisibility(R.id.menu_sort_by_place, false);
            sActivity.setMenuItemVisibility(R.id.menu_grid_1x1, false);
            sActivity.setMenuItemVisibility(R.id.menu_grid_2x1, false);
            sActivity.setMenuItemVisibility(R.id.menu_clear_expired_items, false);
        }
        mImm.hideSoftInputFromWindow(mLayout.getApplicationWindowToken(), 0);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View arg1, int arg2, long arg3) {
        String item = (String) adapterView.getSelectedItem();
        String firstItem = (String) adapterView.getItemAtPosition(0);
        boolean isCanDel = !firstItem.trim().isEmpty() ? true : false;

        if (item.equals(getString(R.string.item_spinner_new_add))) {
            mInputDialog = new InputDialog(getActivity(), getString(R.string.title_input_dialog),
                    getString(R.string.title_input_dialog_body), this);
            mInputDialog.show();
        } else if (item.equals(getString(R.string.item_spinner_del)) && isCanDel) {
            SpinnerAdapter adapter = mSpinMaterialCategory.getAdapter();
            int itemCount = adapter.getCount();
            String[] items = new String[itemCount - 2];

            for (int i = 0; i < itemCount - 2; i++)
                items[i] = (String) adapter.getItem(i);

            mMultiChoiceDialog = new MultiChoiceDialog(getActivity(), getString(R.string.title_single_choice_dialog),
                    items, this);
            mMultiChoiceDialog.show();
        } else if (item.equals(getString(R.string.item_spinner_del)) && !isCanDel)
            /* for fixing a bug, but need to be refactor */
            mSpinMaterialCategory.setSelection(0);
    }

    @Override
    public void onDateSet(com.android.datetimepicker.date.DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();

        cal.set(year, monthOfYear, dayOfMonth);

        if (mCurPressDateBtnId >= 0) {
            if (mCurPressDateBtnId == R.id.rl_purchace_date_layout) {
                mPurchaceDate = cal;
                mTvPurchaceDate.setText(Utility.transDateToString(cal.getTime()));
            } else if (mCurPressDateBtnId == R.id.rl_validate_date_layout) {
                mValidDate = cal;
                mTvValidDate.setText(Utility.transDateToString(cal.getTime()));
            }
            mDatePickerDialog = null;
            mCurPressDateBtnId = -1;
        }
    }

    private class AutoFillTask extends AsyncTask<String, Void, Material> {
        private Material mMaterialInfo;

        @Override
        protected Material doInBackground(String... params) {
            String barcodeFormat = params[0];
            String barcode = params[1];
            mMaterialInfo = DBUtility.selectMaterialHistory(barcodeFormat, barcode);
            return null;
        }

        @Override
        protected void onPostExecute(Material result) {
            if (mMaterialInfo != null) {
                int selectedIndex = -1;
                String type = mMaterialInfo.getMaterialType();
                mNewestBitmap = mMaterialInfo.getMaterialPic();

                for (int i = 0, count = mSpinMaterialCategory.getCount(); i < count; i++) {
                    if (mSpinMaterialCategory.getItemAtPosition(i).equals(type)) {
                        selectedIndex = i;
                        break;
                    }
                }
                mIvAddPhoto.setImageBitmap(mNewestBitmap);
                mActMaterialName.setText(mMaterialInfo.getName());
                if (selectedIndex >= 0) {
                    mSpinMaterialCategory.setSelection(selectedIndex);
                }
                mActMaterialPlace.setText(mMaterialInfo.getMaterialPlace());
                mActComment.setText(mMaterialInfo.getComment());
            }
            super.onPostExecute(result);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }
}
