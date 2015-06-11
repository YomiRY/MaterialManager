package com.material.management.dialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.material.management.R;
import com.material.management.data.Material;
import com.material.management.utils.BarCodeUtility;
import com.material.management.utils.DBUtility;
import com.material.management.utils.FileUtility;
import com.material.management.utils.Utility;
import com.picasso.Callback;
import com.picasso.Picasso;

public class MaterialModifyDialog extends AlertDialog.Builder implements OnClickListener {
    private static final int REQ_CAMERA_TAKE_PIC = 1;
    private static final int REQ_SELECT_PICTURE = 2;

    private RelativeLayout mRlOnLoading;
    private View mLayout;
    private AutoCompleteTextView mActMaterialName;
    private TextView mTvBarcode;
    private Spinner mSpinMaterialCategory;
    private EditText mEtNotificationDays;
    private ImageView mImgvPicCamera;
    private ImageView mImgvPicAlbum;
    private ImageView mImgvPreviewPic;
    private AutoCompleteTextView mActMaterialPlace;
    private AutoCompleteTextView mActComment;

    private Activity mActivity;
    private Fragment mParFragment;
    private boolean mIsShown;
    private Material mMaterial;
    private DialogInterface.OnClickListener mListener;
    private Context mContext;
    private Bitmap mBarcodeBitmap;
    private String mBarcode = "";
    private String mBarcodeFormat = "";
    private ArrayAdapter<String> mTextHistAdapter = null;
    private ArrayList<String> mTextHistoryList;
    private LinkedHashSet<String> mMaterialTypes = null;

    public MaterialModifyDialog(Fragment fragment, Material material, DialogInterface.OnClickListener listener) {
        super(fragment.getActivity(), R.style.AlertDialogTheme);
        this.mContext = Utility.getContext();
        this.mMaterial = material;
        this.mIsShown = false;
        this.mActivity = fragment.getActivity();
        this.mParFragment = fragment;
        this.mListener = listener;

        initView();

        this.setTitle(mContext.getString(R.string.title_material_modify_dialog_title));
        this.setPositiveButton(mContext.getString(R.string.title_positive_btn_label), mListener);
        this.setNegativeButton(mContext.getString(R.string.title_negative_btn_label), mListener);
    }

    private void initView() {
        LayoutInflater layoutInflater = (LayoutInflater) Utility.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mLayout = layoutInflater.inflate(R.layout.dialog_material_modify_pop_win_layout, null);
        mRlOnLoading = (RelativeLayout) mLayout.findViewById(R.id.rl_on_loading);
        mActMaterialName = (AutoCompleteTextView) mLayout.findViewById(R.id.act_material_name);
        mTvBarcode = (TextView) mLayout.findViewById(R.id.tv_barcode);
        mSpinMaterialCategory = (Spinner) mLayout.findViewById(R.id.spin_material_category);
        mEtNotificationDays = (EditText) mLayout.findViewById(R.id.et_notification_days);
        mImgvPicCamera = (ImageView) mLayout.findViewById(R.id.imgv_material_pic_camera);
        mImgvPicAlbum = (ImageView) mLayout.findViewById(R.id.imgv_material_pic_album);
        mImgvPreviewPic = (ImageView) mLayout.findViewById(R.id.imgv_preview_pic);
        mActMaterialPlace = (AutoCompleteTextView) mLayout.findViewById(R.id.act_material_place);
        mActComment = (AutoCompleteTextView) mLayout.findViewById(R.id.act_comment);

        initSpinnerData();
        initAutoCompleteData();

        mActMaterialName.setText(mMaterial.getName());
        setBarcodeInfo(mMaterial.getBarcodeFormat(), mMaterial.getBarcode());
        mSpinMaterialCategory.setSelection(((ArrayAdapter<String>) mSpinMaterialCategory.getAdapter())
                .getPosition(mMaterial.getMaterialType()));
        mEtNotificationDays.setText(Integer.toString(mMaterial.getNotificationDays()));
//      mImgvPreviewPic.setImageBitmap(mMaterial.getMaterialPic());
        Picasso.with(mActivity).cancelRequest(mImgvPreviewPic);
        Picasso.with(mActivity).load(new File(mMaterial.getMaterialPicPath())).fit().into(mImgvPreviewPic, new Callback() {
            @Override
            public void onSuccess() {
                mImgvPreviewPic.setVisibility(View.VISIBLE);
                mRlOnLoading.setVisibility(View.GONE);
            }

            @Override
            public void onError() {
                mImgvPreviewPic.setVisibility(View.VISIBLE);
                mRlOnLoading.setVisibility(View.GONE);
            }
        });
        mActMaterialPlace.setText(mMaterial.getMaterialPlace());
        mActComment.setText(mMaterial.getComment());

        mImgvPicAlbum.setOnClickListener(this);
        mImgvPicCamera.setOnClickListener(this);
        mTvBarcode.setOnClickListener(this);
        mImgvPreviewPic.setVisibility(View.VISIBLE);
        this.setView(mLayout);
    }

    private void initSpinnerData() {
        boolean isInitialized = Utility.getBooleanValueForKey(Utility.SHARE_IS_INITIALIZED);

        if (isInitialized)
            mMaterialTypes = new LinkedHashSet<String>();
        else {
            mMaterialTypes = new LinkedHashSet<String>(Arrays.asList(mParFragment.getResources().getStringArray(
                    R.array.default_material_type)));
            Utility.setBooleanValueForKey(Utility.SHARE_IS_INITIALIZED, true);
        }

        for (String type : mMaterialTypes)
            DBUtility.insertMaterialTypeInfo(type);

        /* We don't need add/delete category item in the dialog */
        ArrayList<String> spinList = DBUtility.selectMaterialTypeInfo();
        ArrayAdapter categoryAdapter = new ArrayAdapter<String>(mContext, R.layout.view_spinner_item_layout, spinList) {
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);

                ((TextView) v).setGravity(Gravity.CENTER);
//               changeLayoutConfig(v);
                return v;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);

//              changeLayoutConfig(v);
                return v;
            }
        };
        ;

        mSpinMaterialCategory.setAdapter(categoryAdapter);
    }

    private void initAutoCompleteData() {
        if (mTextHistAdapter == null) {
            mTextHistoryList = new ArrayList<String>();

            mTextHistoryList.addAll(Arrays.asList(Utility.getStringValueForKey(Utility.SHARE_AUTO_COMPLETE_TEXT).split(":")));

            mTextHistAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_dropdown_item_1line, mTextHistoryList);
        } else {
            mTextHistAdapter.clear();
            mTextHistAdapter.addAll(mTextHistoryList);
        }
        mActMaterialName.setAdapter(mTextHistAdapter);
        mActMaterialPlace.setAdapter(mTextHistAdapter);
        mActComment.setAdapter(mTextHistAdapter);
        mTextHistAdapter.notifyDataSetChanged();
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
        Utility.setStringValueForKey(Utility.SHARE_AUTO_COMPLETE_TEXT, textHistory.toString());
    }

    public void setCameraPic(Bitmap newBitmap) {
        Bitmap oldBmp = ((BitmapDrawable) mImgvPreviewPic.getDrawable()).getBitmap();

        mImgvPreviewPic.setImageBitmap(null);
        Utility.releaseBitmaps(oldBmp);
        oldBmp = null;

        mImgvPreviewPic.setImageBitmap(newBitmap);
    }

    public void setBarcodeInfo(String barcodeFormat, String barcode) {
        try {
            /* Pre-reset as default */
            mBarcode = "";
            mBarcodeFormat = "";
            Drawable barcodeDrawable = mContext.getResources().getDrawable(R.drawable.selector_barcode);

            barcodeDrawable.setBounds(0, 0, barcodeDrawable.getIntrinsicWidth(), barcodeDrawable.getIntrinsicHeight());
            mTvBarcode.setText("x xxxxxx xxxxxx x");
            mTvBarcode.setCompoundDrawables(null, barcodeDrawable, null, null);

            Utility.releaseBitmaps(mBarcodeBitmap);
            mBarcodeBitmap = null;
            if (barcodeFormat != null && barcode != null && !barcodeFormat.isEmpty() && !barcode.isEmpty()) {
                mBarcode = barcode;
                mBarcodeFormat = barcodeFormat;
                mBarcodeBitmap = BarCodeUtility
                        .encodeAsBitmap(barcode, BarcodeFormat.valueOf(mBarcodeFormat), 600, 300);

                barcodeDrawable = new BitmapDrawable(mContext.getResources(), mBarcodeBitmap);
                barcodeDrawable.setBounds(0, 0, barcodeDrawable.getIntrinsicWidth(),
                        barcodeDrawable.getIntrinsicHeight());
                mTvBarcode.setText(barcode);
                mTvBarcode.setCompoundDrawables(null, barcodeDrawable, null, null);
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.imgv_material_pic_camera: {
            /* from camera */
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(FileUtility.TEMP_PHOTO_FILE));
                mParFragment.startActivityForResult(takePictureIntent, REQ_CAMERA_TAKE_PIC);
            }
            break;
            case R.id.imgv_material_pic_album: {
                Intent albumPictureIntent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                mParFragment.startActivityForResult(
                        Intent.createChooser(albumPictureIntent, mContext.getString(R.string.title_image_chooser_title)),
                        REQ_SELECT_PICTURE);
            }
            break;
            case R.id.tv_barcode: {
                IntentIntegrator integrator = new IntentIntegrator(mParFragment);
                integrator.initiateScan();
            }
            break;
        }
    }

    public Material getUpdatedMaterialInfo() {
        Material newMaterial = new Material();
        Bitmap bitmap = ((BitmapDrawable) mImgvPreviewPic.getDrawable()).getBitmap();

        newMaterial.setName(mActMaterialName.getText().toString());
        newMaterial.setMaterialType((String) mSpinMaterialCategory.getSelectedItem());
            /* Use the same barcode and barcode format temporarily */
        newMaterial.setBarcode(mBarcode);
        newMaterial.setBarcodeFormat(mBarcodeFormat);
            /* We can't allow user to modify the validate date and purchase date */
        newMaterial.setPurchaceDate(mMaterial.getPurchaceDate());
        newMaterial.setValidDate(mMaterial.getValidDate());
        newMaterial.setIsValidDateSetup(mMaterial.getIsValidDateSetup());
        newMaterial.setMaterialPic(bitmap);
        newMaterial.setMaterialPicPath(Utility.getPathFromUri(Utility.getImageUri(bitmap)));
        newMaterial.setNotificationDays(Integer.parseInt(mEtNotificationDays.getText().toString()));
        newMaterial.setMaterialPlace(mActMaterialPlace.getText().toString());
        newMaterial.setComment(mActComment.getText().toString());

        updateTextHistory(newMaterial.getName(), newMaterial.getMaterialPlace(), newMaterial.getComment());

        return newMaterial;
    }

    public Material getOldMaterialInfo() {
        return mMaterial;
    }

    @Override
    public AlertDialog show() {
        mIsShown = true;
        AlertDialog dialog = super.show();
        Window window = dialog.getWindow();

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        return dialog;
    }

    public void setShowState(boolean isShowing) {
        mIsShown = isShowing;
    }

    public boolean isShowing() {
        return mIsShown;
    }
}
