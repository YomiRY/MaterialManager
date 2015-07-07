package com.material.management;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.material.management.data.GroceryItem;
import com.material.management.dialog.CropImageDialog;
import com.material.management.dialog.InputDialog;
import com.material.management.dialog.MultiChoiceDialog;
import com.material.management.dialog.SelectPhotoDialog;
import com.material.management.utils.BarCodeUtility;
import com.material.management.utils.DBUtility;
import com.material.management.utils.FileUtility;
import com.material.management.utils.Utility;
import com.picasso.Callback;
import com.picasso.Picasso;
import com.squareup.okhttp.internal.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashSet;


public class GroceryItemLoginActivity extends MMActivity implements DialogInterface.OnClickListener, AdapterView.OnItemSelectedListener {
    private static final int REQ_CAMERA_TAKE_PIC = 1;
    private static final int REQ_SELECT_PICTURE = 2;

    private View mLayout;
    private Spinner mSpinUnit;
    private Spinner mSpinItemCategory;
    private AutoCompleteTextView mActGroceryItemName;
    private EditText mEtSize;
    private EditText mEtQty;
    private EditText mEtPrice;
    private EditText mEtItemNote;
    private ImageView mIvAddPhoto;
    private ImageView mIvQtyPlus;
    private ImageView mIvQtyMinus;
    private TextView mTvBarcode;

    private ActionBar mActionBar;
    private Menu mOptionMenu;
    private InputDialog mInputDialog;
    private MultiChoiceDialog mMultiChoiceDialog;
    private ArrayAdapter<String> mUnitAdapter = null;
    private ArrayAdapter<String> mCategoryAdapter = null;
    private LinkedHashSet<String> mMaterialTypes = null;
    private SelectPhotoDialog mSelectPhotoDialog;
    private CropImageDialog mCropImgDialog;
    private ArrayAdapter<String> mTextHistAdapter = null;
    private ArrayList<String> mTextHistoryList;
    private Bitmap mBarcodeBitmap = null;
    private Bitmap mNewestBitmap = null;
    private BitmapFactory.Options mOptions = null;
    private String mBarcode = "";
    private String mBarcodeFormat = "";
    private int mGroceryListId;
    private GroceryItem mGroceryItem = null;
    private Calendar mPurchaceDate;
    private Calendar mValidDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLayout = mInflater.inflate(R.layout.activity_grocery_item_login, null, false);
        boolean isInitialized = Utility.getBooleanValueForKey(Utility.CATEGORY_IS_INITIALIZED);

        setContentView(mLayout);

        if (isInitialized) {
            mMaterialTypes = new LinkedHashSet<String>();
        } else {
            mMaterialTypes = new LinkedHashSet<String>(Arrays.asList(getResources().getStringArray(
                    R.array.default_material_type)));
            Utility.setBooleanValueForKey(Utility.CATEGORY_IS_INITIALIZED, true);
        }

        findView();
        setListener();
        init();
        changeLayoutConfig(mLayout);
    }

    private void findView() {
        mActGroceryItemName = (AutoCompleteTextView) mLayout.findViewById(R.id.act_item_name);
        mEtSize = (EditText) mLayout.findViewById(R.id.et_size);
        mEtQty = (EditText) mLayout.findViewById(R.id.et_qty);
        mEtPrice = (EditText) mLayout.findViewById(R.id.et_price);
        mEtItemNote = (EditText) mLayout.findViewById(R.id.et_item_note);
        mSpinItemCategory = (Spinner) mLayout.findViewById(R.id.spin_item_category);
        mSpinUnit = (Spinner) mLayout.findViewById(R.id.spin_unit);
        mIvAddPhoto = (ImageView) mLayout.findViewById(R.id.iv_add_photo);
        mIvQtyPlus = (ImageView) mLayout.findViewById(R.id.iv_quantity_plus);
        mIvQtyMinus = (ImageView) mLayout.findViewById(R.id.iv_quantity_minus);
        mTvBarcode = (TextView) mLayout.findViewById(R.id.tv_barcode);
    }

    private void setListener() {
        mIvAddPhoto.setOnClickListener(this);
        mTvBarcode.setOnClickListener(this);
        mIvQtyPlus.setOnClickListener(this);
        mIvQtyMinus.setOnClickListener(this);
        mSpinItemCategory.setOnItemSelectedListener(this);
    }

    private void init(){
        mActionBar = getActionBar();
        mOptions = new BitmapFactory.Options();
        mGroceryListId = getIntent().getIntExtra("grocery_list_id", -1);
        mGroceryItem = getIntent().getParcelableExtra("grocery_item");
        mPurchaceDate = Calendar.getInstance();
        mValidDate = Calendar.getInstance();

        mActionBar.show();
        mActionBar.setHomeButtonEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowTitleEnabled(true);

        mOptions.inDensity = Utility.getDisplayMetrics().densityDpi;
        mOptions.inScaled = false;
        mOptions.inPurgeable = true;
        mOptions.inInputShareable = true;

        initSpinnerData();

        setTitle(getString(R.string.title_grocery_item_login_actionbar_title));

        if(mGroceryListId < 0 && mGroceryItem != null) {
            mGroceryListId = mGroceryItem.getGroceryListId();
            String groceryPicPath = mGroceryItem.getGroceryPicPath();
            mBarcode = mGroceryItem.getBarcode();
            mBarcodeFormat = mGroceryItem.getBarcodeFormat();

            if(groceryPicPath != null) {
                Picasso.with(mContext).load(new File(groceryPicPath)).fit().into(mIvAddPhoto, new Callback() {
                    @Override
                    public void onSuccess() {
                        mNewestBitmap = ((BitmapDrawable)mIvAddPhoto.getDrawable()).getBitmap();
                    }

                    @Override
                    public void onError() {}
                });
            }

            if (mBarcode != null && mBarcodeFormat != null && !mBarcode.isEmpty() && !mBarcodeFormat.isEmpty()) {
                try {
                    mBarcodeBitmap = BarCodeUtility.encodeAsBitmap(mBarcode,
                            BarcodeFormat.valueOf(mBarcodeFormat), 600, 300);
                    Drawable barcodeDrawable = new BitmapDrawable(getResources(), mBarcodeBitmap);

                    barcodeDrawable.setBounds(0, 0, barcodeDrawable.getIntrinsicWidth(),
                            barcodeDrawable.getIntrinsicHeight());
                    mTvBarcode.setText(mBarcode);
                    mTvBarcode.setCompoundDrawables(null, barcodeDrawable, null, null);
                } catch (WriterException e) {
                    e.printStackTrace();
                }
            }

            mActGroceryItemName.setText(mGroceryItem.getName());
            mSpinItemCategory.setSelection(((ArrayAdapter<String>) mSpinItemCategory.getAdapter()).getPosition(mGroceryItem.getGroceryType()));
            mEtSize.setText(mGroceryItem.getSize());
            mSpinUnit.setSelection(((ArrayAdapter<String>)mSpinUnit.getAdapter()).getPosition(mGroceryItem.getSizeUnit()));
            mEtQty.setText(mGroceryItem.getQty());
            mEtPrice.setText(mGroceryItem.getPrice());
            mEtItemNote.setText(mGroceryItem.getComment());
            mPurchaceDate = mGroceryItem.getPurchaceDate();
            mValidDate = mGroceryItem.getValidDate();
        }

        initAutoCompleteData();
    }

    private void initAutoCompleteData() {
        if (mTextHistAdapter == null) {
            mTextHistoryList = new ArrayList<String>();

            mTextHistoryList.addAll(Arrays.asList(Utility.getStringValueForKey(Utility.INPUT_TEXT_HISTORY).split(":")));

            mTextHistAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, mTextHistoryList);
        } else {
            mTextHistAdapter.clear();
            mTextHistAdapter.addAll(mTextHistoryList);
        }

        mActGroceryItemName.setAdapter(mTextHistAdapter);
        mTextHistAdapter.notifyDataSetChanged();
    }

    private void updateTextHistory(String... textAry) {
        StringBuilder textHistory = new StringBuilder("");

        for (String text : textAry) {
            if (!mTextHistoryList.contains(text)) {
                mTextHistoryList.add(text);
            }
        }

        for(String text : mTextHistoryList) {
            textHistory.append(text);
            textHistory.append(":");
        }
        textHistory.deleteCharAt(textHistory.length() - 1);
        Utility.setStringValueForKey(Utility.INPUT_TEXT_HISTORY, textHistory.toString());
        initAutoCompleteData();
    }

    private void clearUserData() {
        /* set the default time */
        mPurchaceDate = Calendar.getInstance();
        mValidDate = Calendar.getInstance();
        mNewestBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.ic_no_image_available)).getBitmap();
        mBarcode = "";
        mBarcodeFormat = "";
        Drawable defaultBarcodeImg = getResources().getDrawable(R.drawable.selector_barcode);

        defaultBarcodeImg.setBounds(0, 0, defaultBarcodeImg.getIntrinsicWidth(),
                defaultBarcodeImg.getIntrinsicHeight());

        mIvAddPhoto.setImageResource(R.drawable.selector_add_photo_status);
        mTvBarcode.setText("x xxxxxx xxxxxx x");
        mTvBarcode.setCompoundDrawables(null, defaultBarcodeImg, null, null);
        mActGroceryItemName.setText("");
        mSpinItemCategory.setSelection(0);
        mEtSize.setText("");
        mSpinUnit.setSelection(0);
        mEtQty.setText("0");
        mEtPrice.setText("0");
        mEtItemNote.setText("");
    }

    private String isAllowSave(GroceryItem groceryItem) {
        StringBuffer msg = new StringBuffer(getString(R.string.msg_grocery_item_login_error_head));
        boolean isAllow = true;
        String qty = groceryItem.getQty();
        String name = groceryItem.getName();
        String category = groceryItem.getGroceryType();

        if(name.trim().isEmpty()) {
            msg = msg.append(getString(R.string.msg_no_grocery_name));
            isAllow = isAllow && false;
        }

        if (category.isEmpty() || category.equals(getString(R.string.item_spinner_del))) {
            msg.append(getString(R.string.msg_error_no_spercify_grocery_type));
            isAllow = isAllow && false;
        }

        if(qty.trim().isEmpty()) {
            msg = msg.append(getString(R.string.msg_no_grocery_qty));
            isAllow = isAllow && false;
        } else if(qty.equals("0")) {
            msg = msg.append(getString(R.string.msg_no_grocery_zero_qty_err));
            isAllow = isAllow && false;
        }

        return !isAllow ? msg.toString() : null;
    }

    private void initSpinnerData() {
        for (String type : mMaterialTypes)
            DBUtility.insertMaterialTypeInfo(type);

        ArrayList<String> spinList = DBUtility.selectMaterialTypeInfo();

        spinList.add(getString(R.string.item_spinner_new_add));
        spinList.add(getString(R.string.item_spinner_del));

        if (spinList.size() == 2) {
            spinList.add(0, getString(R.string.item_spinner_empty));
        }

        if(mCategoryAdapter == null) {
            mCategoryAdapter = new ArrayAdapter<String>(this, R.layout.view_spinner_item_layout, spinList) {
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

        mSpinItemCategory.setAdapter(mCategoryAdapter);
        mCategoryAdapter.notifyDataSetChanged();

        mUnitAdapter = new ArrayAdapter<String>(this, R.layout.view_spinner_item_layout, getResources().getStringArray(R.array.title_grocery_item_units)) {
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
        mSpinUnit.setAdapter(mUnitAdapter);
        mUnitAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_bar_menu, menu);
        mOptionMenu = menu;

        setMenuItemVisibility(R.id.action_search, false);
        setMenuItemVisibility(R.id.menu_action_add, true);
        setMenuItemVisibility(R.id.menu_action_cancel, true);
        setMenuItemVisibility(R.id.menu_sort_by_date, false);
        setMenuItemVisibility(R.id.menu_sort_by_name, false);
        setMenuItemVisibility(R.id.menu_sort_by_place, false);
        setMenuItemVisibility(R.id.menu_grid_1x1, false);
        setMenuItemVisibility(R.id.menu_grid_2x1, false);
        setMenuItemVisibility(R.id.menu_clear_expired_items, false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home: {
                finish();
            }
            break;

            case R.id.menu_action_add: {
                if(mGroceryListId < 0) {
                    showToast(getString(R.string.data_save_fail));
                }

                GroceryItem groceryItem = new GroceryItem();

                groceryItem.setGroceryListId(mGroceryListId);
                groceryItem.setGroceryPic(mNewestBitmap);
                groceryItem.setBarcode(mBarcode);
                groceryItem.setBarcodeFormat(mBarcodeFormat);
                groceryItem.setName(mActGroceryItemName.getText().toString());
                groceryItem.setGroceryType((String) mSpinItemCategory.getSelectedItem());
                groceryItem.setSize(mEtSize.getText().toString());
                groceryItem.setSizeUnit((String) mSpinUnit.getSelectedItem());
                groceryItem.setQty(mEtQty.getText().toString());
                groceryItem.setPrice(mEtPrice.getText().toString());
                groceryItem.setComment(mEtItemNote.getText().toString());
                groceryItem.setPurchaceDate(mPurchaceDate);
                groceryItem.setValidDate(mValidDate);

                String msg = isAllowSave(groceryItem);

                if(msg == null) {
                    if (mGroceryItem != null) {
                    /* Delete the previous data, if user modify the selected grocery item */
                        DBUtility.deleteGroceryItem(mGroceryItem);
                    }

                    DBUtility.insertGroceryItemInfo(groceryItem);
                    updateTextHistory(groceryItem.getName(), groceryItem.getComment());
                    clearUserData();
                    showToast(getString(R.string.data_save_success));
                    finish();
                } else {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme);

                    dialog.setTitle(getString(R.string.msg_error_dialog_title));
                    dialog.setMessage(msg);
                    dialog.setPositiveButton(getString(R.string.title_positive_btn_label), null);
                    dialog.show();
                }
            }
            break;

            case R.id.menu_action_cancel: {
                clearUserData();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setMenuItemVisibility(int id, boolean visible) {
        if(mOptionMenu != null) {
            MenuItem item = mOptionMenu.findItem(id);

            if(item != null)
                item.setVisible(visible);
        }
    }

    @Override
    protected void onStart() {
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCropImgDialog != null && mCropImgDialog.isDialogShowing())
            mCropImgDialog.setShowState(false);
        if (mInputDialog != null && mInputDialog.isDialogShowing())
            mInputDialog.setShowState(false);
        if (mMultiChoiceDialog != null && mMultiChoiceDialog.isDialogShowing())
            mMultiChoiceDialog.setShowState(false);
    }

    @Override
    protected void onStop() {
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int id = v.getId();

        switch (id) {
            case R.id.iv_add_photo: {
                mSelectPhotoDialog = new SelectPhotoDialog(this, getString(R.string.title_select_photo), new String[] {
                        getString(R.string.title_select_photo_from_album),
                        getString(R.string.title_select_photo_from_camera) }, this);

                mSelectPhotoDialog.show();
            }
            break;

            case R.id.tv_barcode: {
                IntentIntegrator integrator = new IntentIntegrator(this);

                integrator.initiateScan();
            }
            break;

            case R.id.iv_quantity_plus: {
                String qtyStr = mEtQty.getText().toString();

                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.anim_press_bounce));
                if(qtyStr == null || qtyStr.isEmpty()) {
                    mEtQty.setText("0");
                    return;
                }

                mEtQty.setText(Integer.toString(Integer.parseInt(qtyStr) + 1));
            }
            break;

            case  R.id.iv_quantity_minus: {
                String qtyStr = mEtQty.getText().toString();

                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.anim_press_bounce));
                if(qtyStr == null || qtyStr.isEmpty()) {
                    mEtQty.setText("0");
                    return;
                }

                int qty = Integer.parseInt(qtyStr);
                qty = (qty - 1 < 0) ? 0 : qty - 1;

                mEtQty.setText(Integer.toString(qty));
            }
            break;

        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case REQ_CAMERA_TAKE_PIC: {
                if (Activity.RESULT_OK == resultCode) {
                    mIvAddPhoto.setImageResource(R.drawable.selector_add_photo_status);
                    Utility.releaseBitmaps(mNewestBitmap);
                    mNewestBitmap = null;

                    try {
                        mNewestBitmap = BitmapFactory.decodeFile(FileUtility.TEMP_PHOTO_FILE.getAbsolutePath(), mOptions);
                    } catch (OutOfMemoryError e) {
                        e.printStackTrace();
                        System.gc();
                    }

                    if (mNewestBitmap != null) {
                        mCropImgDialog = new CropImageDialog(this, mNewestBitmap, this);

                        mCropImgDialog.show();
                    }
                }
            }
            break;

            case REQ_SELECT_PICTURE: {
                if (Activity.RESULT_OK == resultCode && intent != null && intent.getData() != null) {
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
                        mCropImgDialog = new CropImageDialog(this, mNewestBitmap, this);

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
                        } catch (WriterException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            break;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
         /* which < 0, then the dialog has positive/negative button */
        if (which < 0) {
            if (AlertDialog.BUTTON_POSITIVE == which) {
                if (mCropImgDialog != null && mCropImgDialog.isDialogShowing()) {
                    /* Recycle the original bitmap from camera intent extra. */
                    if (mNewestBitmap != null && !mNewestBitmap.isRecycled()) {
                        mIvAddPhoto.setImageResource(R.drawable.selector_add_photo_status);
                        Utility.releaseBitmaps(mNewestBitmap);
                        mNewestBitmap = null;
                    }

                    mNewestBitmap = mCropImgDialog.getCroppedImage();

                    mIvAddPhoto.setImageBitmap(mNewestBitmap);
                    mCropImgDialog.setShowState(false);
                } else if (mInputDialog != null && mInputDialog.isDialogShowing()) {
                    String input = mInputDialog.getInputString();

                    if (input.trim().isEmpty()) {
                        mSpinItemCategory.setSelection(0);
                        return;
                    }

                    mMaterialTypes.add(input);
                    mInputDialog.setShowState(false);
                    initSpinnerData();
                    mSpinItemCategory.setSelection(0);
                } else if (mMultiChoiceDialog != null && mMultiChoiceDialog.isDialogShowing()) {
                    final String[] selectedItems = mMultiChoiceDialog.getSelectedItemsString();

                    if (selectedItems == null || selectedItems.length == 0) {
                        mSpinItemCategory.setSelection(0);
                        return;
                    }

                    mMultiChoiceDialog.setShowState(false);

                    AlertDialog.Builder subDialog = new AlertDialog.Builder(GroceryItemLoginActivity.this, R.style.AlertDialogTheme);

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
                                        mSpinItemCategory.setSelection(0);
                                    }
                                }
                            });
                    subDialog.setNegativeButton(getString(R.string.title_negative_btn_label), null);
                    subDialog.show();
                }
            } else if (AlertDialog.BUTTON_NEGATIVE == which) {
                if (mCropImgDialog != null && mNewestBitmap != null && !mNewestBitmap.isRecycled()) {
                    mIvAddPhoto.setImageResource(R.drawable.selector_add_photo_status);
                    Utility.releaseBitmaps(mNewestBitmap);
                    mCropImgDialog.setShowState(false);
                    mNewestBitmap = null;
                } else if (mMultiChoiceDialog != null || mInputDialog != null) {
                    mSpinItemCategory.setSelection(0);
                }
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
        mSelectPhotoDialog = null;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        String item = (String) adapterView.getSelectedItem();
        String firstItem = (String) adapterView.getItemAtPosition(0);
        boolean isCanDel = !firstItem.trim().isEmpty() ? true : false;

        if (item.equals(getString(R.string.item_spinner_new_add))) {
            mInputDialog = new InputDialog(this, getString(R.string.title_input_dialog),
                    getString(R.string.title_input_dialog_body), this);
            mInputDialog.show();
        } else if (item.equals(getString(R.string.item_spinner_del)) && isCanDel) {
            SpinnerAdapter adapter = mSpinItemCategory.getAdapter();
            int itemCount = adapter.getCount();
            String[] items = new String[itemCount - 2];

            for (int i = 0; i < itemCount - 2; i++)
                items[i] = (String) adapter.getItem(i);

            mMultiChoiceDialog = new MultiChoiceDialog(this, getString(R.string.title_single_choice_dialog),
                    items, this);
            mMultiChoiceDialog.show();
        } else if (item.equals(getString(R.string.item_spinner_del)) && !isCanDel)
            /* for fixing a bug, but need to be refactor */
            mSpinItemCategory.setSelection(0);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

}
