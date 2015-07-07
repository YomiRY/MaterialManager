package com.material.management.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import com.android.datetimepicker.date.DatePickerDialog;
import com.material.management.MMFragment;
import com.material.management.MainActivity;
import com.material.management.MaterialModifyActivity;
import com.material.management.Observer;
import com.material.management.R;
import com.material.management.dialog.BarcodeDialog;
import com.material.management.dialog.MaterialMenuDialog;
import com.material.management.component.RoundedImageView;
import com.material.management.data.BundleInfo;
import com.material.management.data.Material;
import com.material.management.data.StreamItem;
import com.material.management.interf.ISearchUpdate;
import com.material.management.utils.DBUtility;
import com.material.management.utils.Utility;
import com.picasso.Callback;
import com.picasso.Picasso;

public class MaterialManagerFragment extends MMFragment implements Observer, SearchView.OnQueryTextListener,
        OnItemClickListener, DialogInterface.OnClickListener, DatePickerDialog.OnDateSetListener {
    private static final String DATEPICKER_TAG = "datepicker";

    private View mLayout;
    private GridView mGvMaterialType;

    private Locale mLocale = null;
    private MaterialMenuDialog mMaterialMenu = null;
    private AlertDialog mMaterialMenuDialog = null;
    private DatePickerDialog mDatePickerDialog = null;
    private BarcodeDialog mBarcodeDialog;
    private StreamAdapter mMaterialTypeAdapter;
    private Options mOptions = new Options();
    private Object mData = null;
    private Material mSelectedMaterial = null;
    private String mSearchString = null;
    private int mSelectedPosition;
    private int mMaterialTypGridNum = -1;
    private boolean mIsNeedResumeRefresh = false;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mLayout = inflater.inflate(R.layout.fragment_material_manager_layout, container, false);
        mLocale = Locale.getDefault();
        mOptions.inPurgeable = true;
        mOptions.inInputShareable = true;
        mOptions.inScaled = false;
        mOptions.inDensity = Utility.getDisplayMetrics().densityDpi;

        initView();
        update(mData);

        return mLayout;
    }

    @Override
    public void onResume() {
        sendScreenAnalytics(getString(R.string.ga_app_view_material_manager_fragment));

        ListAdapter adapter = mGvMaterialType.getAdapter();

        if(adapter != null && (adapter instanceof MaterialAdapter) && mIsNeedResumeRefresh) {
            String materialType = mSelectedMaterial.getMaterialType();
            Bundle bundle = new Bundle();

            bundle.putString(BundleInfo.BUNDLE_KEY_MATERIAL_TYPE, materialType);
            bundle.putString(BundleInfo.BUNDLE_KEY_MATERIAL_NAME, null);
            update(bundle);
        }
        mIsNeedResumeRefresh = false;

        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        mGvMaterialType = null;
        mMaterialTypeAdapter = null;
        mSearchString = null;
        mLocale = null;
        mLayout = null;
        mMaterialMenu = null;
        mMaterialMenuDialog = null;
        mBarcodeDialog = null;

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initView() {
        /* TODO: It may be duplicate with loadMaterialType */
        mMaterialTypGridNum = (mMaterialTypGridNum == -1) ? Utility.getIntValueForKey(Utility.MATERIAL_TYPE_GRID_COLUMN_NUM) : mMaterialTypGridNum;
        mGvMaterialType = (GridView) mLayout.findViewById(R.id.gv_material_grid);
        RelativeLayout rlEmptyData = (RelativeLayout) mLayout.findViewById(R.id.rl_empty_data);
        mMaterialTypeAdapter = new StreamAdapter();

        mGvMaterialType.setAdapter(mMaterialTypeAdapter);
        mGvMaterialType.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                mSelectedPosition = position;
                return false;
            }
        });
        setMaterialTypeGridColumnNum(mMaterialTypGridNum);
        mGvMaterialType.setEmptyView(rlEmptyData);
        registerForContextMenu(mGvMaterialType);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        ArrayList<String> items = new ArrayList<String>();
        ListAdapter adapter = mGvMaterialType.getAdapter();

        items.add(getString(R.string.title_menu_item_del));

        if (adapter instanceof MaterialAdapter) {
            mSelectedMaterial = (Material) adapter.getItem(mSelectedPosition);

            items.add(getString(R.string.title_menu_item_as_type_photo));
            items.add(getString(R.string.title_menu_item_modify));
            items.add(getString(R.string.title_menu_item_barcode));

            if (mSelectedMaterial.getIsValidDateSetup() == 0) {
                items.add(getString(R.string.title_menu_item_set_up_valid_date));
            }
        }
        mMaterialMenu = new MaterialMenuDialog(mOwnerActivity, getString(R.string.title_menu_head),
                items.toArray(new String[0]), this);

        mMaterialMenuDialog = mMaterialMenu.show();
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
        if (mMaterialMenu == null)
            return;

        ListAdapter adapter = mGvMaterialType.getAdapter();
        String item = mMaterialMenu.getItemAtPos(pos);
        boolean isDel = item.equals(getString(R.string.title_menu_item_del));
        boolean isTypePhoto = item.equals(getString(R.string.title_menu_item_as_type_photo));
        boolean isModify = item.equals(getString(R.string.title_menu_item_modify));
        boolean isViewBarcode = item.equals(getString(R.string.title_menu_item_barcode));
        boolean isSetupValidDate = item.equals(getString(R.string.title_menu_item_set_up_valid_date));

        if (adapter instanceof StreamAdapter) {
            final StreamItem materialItem = (StreamItem) adapter.getItem(mSelectedPosition);
            final StreamAdapter streamAdapter = (StreamAdapter) adapter;

            if (isDel) {
                DialogInterface.OnClickListener confirmListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        streamAdapter.remove(materialItem);
                        DBUtility.deleteMaterialInfoByType(materialItem.getMaterialType());
                        streamAdapter.refreshSearch(null);
                    }
                };

                showAlertDialog(getString(R.string.title_confirm_del_dialog), getString(R.string.msg_confirm_del_dialog)
                        , getString(R.string.title_positive_btn_label), getString(R.string.title_negative_btn_label)
                        , confirmListener, null);
            }
        } else if (adapter instanceof MaterialAdapter) {
            final Material materialItem = (Material) adapter.getItem(mSelectedPosition);
            final MaterialAdapter materialAdapter = (MaterialAdapter) adapter;
            boolean isNeedRefresh = false;

            if (isDel) {
                DialogInterface.OnClickListener confirmListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        materialAdapter.remove(materialItem);
                        DBUtility.deleteMaterialInfo(materialItem);
                        materialAdapter.refreshSearch(null);
                    }
                };

                showAlertDialog(getString(R.string.title_confirm_del_dialog), getString(R.string.msg_confirm_del_dialog)
                        , getString(R.string.title_positive_btn_label), getString(R.string.title_negative_btn_label)
                        , confirmListener, null);
            } else if (isTypePhoto) {
                materialItem.setIsAsPhotoType(1);
                DBUtility.updateMaterialIsAsPhotoType(materialItem);
                showToast(getString(R.string.msg_set_as_face_success));
            }
            else if (isModify) {
                Intent intent = new Intent(mOwnerActivity, MaterialModifyActivity.class);
                mIsNeedResumeRefresh = true;

                intent.putExtra("material_item", mSelectedMaterial);
                startActivity(intent);
            }
            else if (isViewBarcode) {
                String barcode = materialItem.getBarcode();
                String barcodeFormat = materialItem.getBarcodeFormat();

                if ((barcode != null && !barcode.isEmpty()) && (barcodeFormat != null && !barcodeFormat.isEmpty())) {
                    mBarcodeDialog = new BarcodeDialog(mOwnerActivity, this);

                    mBarcodeDialog.setShowState(true);
                    mBarcodeDialog.setBarcode(barcodeFormat, barcode);
                    mBarcodeDialog.show();
                } else {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);

                    dialog.setTitle(getString(R.string.title_barcode_dialog));
                    dialog.setMessage(getString(R.string.title_no_barcode_msg));
                    dialog.setPositiveButton(getString(R.string.title_positive_btn_label), null);
                    dialog.show();
                }
            } else if (isSetupValidDate) {
                Calendar calendar = Calendar.getInstance();
                mDatePickerDialog = DatePickerDialog.newInstance(this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

                mDatePickerDialog.show(getActivity().getFragmentManager(), DATEPICKER_TAG);
            }

            if (isNeedRefresh) {
                materialAdapter.refreshSearch(null);
            }
        }

        mMaterialMenu.setShowState(false);
        mMaterialMenuDialog.dismiss();
    }

    @Override
    public void onDateSet(DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();

        DBUtility.deleteMaterialInfo(mSelectedMaterial);
        cal.set(year, monthOfYear, dayOfMonth);
        mSelectedMaterial.setValidDate(cal);
        mSelectedMaterial.setIsValidDateSetup(1);
        DBUtility.insertMaterialInfo(mSelectedMaterial);
        ((MaterialAdapter) mGvMaterialType.getAdapter()).refreshSearch(null);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (AlertDialog.BUTTON_POSITIVE == which) {
            if (mBarcodeDialog != null && mBarcodeDialog.isDialogShowing()) {
                mBarcodeDialog.setShowState(false);
            }
        }
        dialog.dismiss();
    }

    public boolean isViewMaterialList() {
        return (mGvMaterialType.getAdapter() instanceof MaterialAdapter);
    }

    private void loadMaterialType(ArrayList<Material> materialInfos) {
        ListAdapter adapter = mGvMaterialType.getAdapter();

        /* use grid view to display material type */
        mMaterialTypGridNum = (mMaterialTypGridNum == -1) ? Utility.getIntValueForKey(Utility.MATERIAL_TYPE_GRID_COLUMN_NUM) : mMaterialTypGridNum;
        setMaterialTypeGridColumnNum(mMaterialTypGridNum);

        if (mGvMaterialType == null || mMaterialTypeAdapter == null || adapter == null)
            return;

        if (!(adapter instanceof StreamAdapter)) {
            mGvMaterialType.setAdapter(mMaterialTypeAdapter);
        }
        mMaterialTypeAdapter.clear();

        for (Material materialInfo : materialInfos) {
            boolean isExpired = checkIsExpired(materialInfo);
            StreamItem item = new StreamItem(getActivity(), materialInfo.getMaterialPicPath(),
                    materialInfo.getMaterialType(), materialInfo.getIsAsPhotoType(), isExpired);

            mMaterialTypeAdapter.add(item);
            mMaterialTypeAdapter.addMaterialInfo(materialInfo);
        }
        mMaterialTypeAdapter.refreshSearch(null);
    }

    private boolean checkIsExpired(Material materialInfo) {
        Calendar validateDate = materialInfo.getValidDate();
        long todayTimeInMillis = Calendar.getInstance().getTimeInMillis();

        return validateDate.getTimeInMillis() < todayTimeInMillis;
    }

    public void setMaterialTypeGridColumnNum(int num) {
        mMaterialTypGridNum = num;
        mGvMaterialType.setNumColumns(num);
        Utility.setIntValueForKey(Utility.MATERIAL_TYPE_GRID_COLUMN_NUM, num);
    }

    public void sortMaterial(int mode) {
        mMaterialTypeAdapter.sortMaterial(mode);
    }

    public void clearExpiredMaterials() {
        mMaterialTypeAdapter.clearExpiredMaterials();
    }

    @Override
    public void update(Object data) {
        mData = data;

        if (mOwnerActivity == null) {
            return;
        }
        MainActivity mainActivity = (MainActivity) mOwnerActivity;

        mainActivity.setMenuItemVisibility(R.id.action_search, true);
        mainActivity.setMenuItemVisibility(R.id.menu_action_add, false);
        mainActivity.setMenuItemVisibility(R.id.menu_action_cancel, false);
        mainActivity.setMenuItemVisibility(R.id.menu_sort_by_date, false);
        mainActivity.setMenuItemVisibility(R.id.menu_sort_by_name, false);
        mainActivity.setMenuItemVisibility(R.id.menu_sort_by_place, false);
        mainActivity.setMenuItemVisibility(R.id.menu_grid_1x1, true);
        mainActivity.setMenuItemVisibility(R.id.menu_grid_2x1, true);
        mainActivity.setMenuItemVisibility(R.id.menu_clear_expired_items, false);

        ArrayList<Material> materialInfos = DBUtility.selectMaterialInfos();

        loadMaterialType(materialInfos);

        /* triggered by a notification */
        if (data != null && data instanceof Bundle) {
            Bundle bundle = (Bundle) data;
            String materialType = bundle.getString(BundleInfo.BUNDLE_KEY_MATERIAL_TYPE);
            String materialName = bundle.getString(BundleInfo.BUNDLE_KEY_MATERIAL_NAME);

            ListAdapter adapter = mGvMaterialType.getAdapter();
            if (adapter instanceof StreamAdapter) {
                ((StreamAdapter) adapter).triggerSelectMaterialType(materialType, materialName);
            }
        }
    }

    private class StreamAdapter extends ArrayAdapter<StreamItem> implements ISearchUpdate {
        private final LayoutInflater mInflater;
        private HashMap<String, Integer> mTypeCountMap;
        private HashMap<String, Integer> mExpiredCountMap;
        private HashMap<String, Integer> mUnExpiredCountMap;
        private HashMap<String, ArrayList<Material>> mTypeMaterialMap;
        private MaterialAdapter mMaterialAdapter;
        private ArrayList<StreamItem> mSearchedItem;
        private ArrayList<StreamItem> mtotalItem;
        private String mSearchStr;
        private int mScaledSize;

        public StreamAdapter() {
            super(getActivity(), 0);
            mInflater = LayoutInflater.from(getActivity());
            mTypeCountMap = new HashMap<String, Integer>();
            mExpiredCountMap = new HashMap<String, Integer>();
            mUnExpiredCountMap = new HashMap<String, Integer>();
            mMaterialAdapter = new MaterialAdapter();
            mTypeMaterialMap = new HashMap<String, ArrayList<Material>>();
            mSearchedItem = new ArrayList<StreamItem>();
            mtotalItem = new ArrayList<StreamItem>();
            mSearchStr = null;
            int px;
            float h = (float) (mMetrics.heightPixels / mMetrics.density);

            if (h > 900) {
                px = (int) ((8 * (mMaterialTypGridNum + 1) * mMetrics.density) + 0.5f);
            } else {
                px = (int) ((5 * (mMaterialTypGridNum + 1) * mMetrics.density) + 0.5f);
            }
            int totalSize = mMetrics.widthPixels - px;
            mScaledSize = totalSize / mMaterialTypGridNum;
        }

        public void sortMaterial(int mode) {
            mMaterialAdapter.sort(mode);
        }

        public void clearExpiredMaterials() {
            mMaterialAdapter.clearExpiredMaterials();
        }

        public int getCount() {
            if (mSearchedItem == null)
                return 0;

            return mSearchedItem.size();
        }

        public StreamItem getItem(int position) {
            return mSearchedItem.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        @Override
        public void add(StreamItem materialItem) {
            String type = materialItem.getMaterialType();
            StreamItem materialTypeItem = null;

            if (!mTypeCountMap.containsKey(type)) {
                mTypeCountMap.put(type, 0);
                mExpiredCountMap.put(type, 0);
                mUnExpiredCountMap.put(type, 0);
                mtotalItem.add(materialItem);
            } else {
                for (int i = 0, len = mtotalItem.size(); i < len; i++) {
                    materialTypeItem = mtotalItem.get(i);

                    /* replace the type default face if the IsAsPhotoType is not 1 */
                    if ((materialTypeItem.getMaterialType().equals(materialItem.getMaterialType()))
                            && (materialTypeItem.getIsAsPhotoType() != 1))
                        mtotalItem.set(i, materialItem);
                }
            }

            if (materialItem.isExpired()) {
                mExpiredCountMap.put(type, mExpiredCountMap.get(type) + 1);
            } else {
                mUnExpiredCountMap.put(type, mUnExpiredCountMap.get(type) + 1);
            }

            mTypeCountMap.put(type, mTypeCountMap.get(type) + 1);
        }

        @Override
        public void remove(StreamItem materialItem) {
            mtotalItem.remove(materialItem);
            mTypeCountMap.remove(materialItem.getMaterialType());
        }

        public void addMaterialInfo(Material materialItem) {
            ArrayList<Material> materialList;
            String materialType = materialItem.getMaterialType();

            if (!mTypeMaterialMap.containsKey(materialType)) {
                materialList = new ArrayList<Material>();
                mTypeMaterialMap.put(materialType, materialList);
            }

            materialList = mTypeMaterialMap.get(materialType);
            materialList.add(materialItem);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewGroup view;

            if (convertView == null) {
                view = (ViewGroup) mInflater.inflate(R.layout.rounded_item_layout, parent, false);

                changeLayoutConfig(view);
            } else {
                view = (ViewGroup) convertView;
            }

            final StreamItem item = getItem(position);
            RoundedImageView roundedImgView = ((RoundedImageView) view.findViewById(R.id.iv_rounded_view_pic));
            final RelativeLayout rlOnLoading = (RelativeLayout) view.findViewById(R.id.rl_on_loading);

            rlOnLoading.setVisibility(View.VISIBLE);
            Picasso.with(mOwnerActivity).cancelRequest(roundedImgView);
            Picasso.with(mOwnerActivity).load(new File(item.getMaterialPicPath())).centerInside().resize(mScaledSize, mScaledSize).into(roundedImgView, new Callback() {
                @Override
                public void onSuccess() {
                    rlOnLoading.setVisibility(View.GONE);
                }

                @Override
                public void onError() {
                    rlOnLoading.setVisibility(View.GONE);
                }
            });
            roundedImgView.setScaleType(item.getScaleType());
            roundedImgView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    String type = item.getMaterialType();

                    triggerSelectMaterialType(type, null);
                }
            });
            roundedImgView.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    return false;
                }
            });

            ((TextView) view.findViewById(R.id.tv_rounded_text2)).setText(Utility.formatMatchedString(
                    item.getMaterialType(), mSearchStr));
            ((TextView) view.findViewById(R.id.tv_rounded_text3)).setText(getString(
                    R.string.format_material_type_count, mTypeCountMap.get(item.getMaterialType())));
            ((TextView) view.findViewById(R.id.tv_material_unexpired_count)).setText(Integer.toString(mUnExpiredCountMap.get(item.getMaterialType())));
            ((TextView) view.findViewById(R.id.tv_material_expired_count)).setText(Integer.toString(mExpiredCountMap.get(item.getMaterialType())));

            return view;
        }

        public void triggerSelectMaterialType(String materialType, String searchString) {
            MainActivity activity = (MainActivity) mOwnerActivity;

            /* use 1 x 1 grid view to display material items */
            // setMaterialTypeGridColumnNum(1);
            mGvMaterialType.setNumColumns(1);

            mMaterialAdapter.clear();
            mMaterialAdapter.addAll(mTypeMaterialMap.get(materialType));
            mGvMaterialType.setAdapter(mMaterialAdapter);
            mMaterialAdapter.refreshSearch(searchString);
            activity.changeHomeAsUpIcon(R.drawable.ic_ab_back_holo_dark_am);

            if (activity != null) {
                activity.setMenuItemVisibility(R.id.action_search, true);
                activity.setMenuItemVisibility(R.id.menu_action_add, false);
                activity.setMenuItemVisibility(R.id.menu_action_cancel, false);
                activity.setMenuItemVisibility(R.id.menu_sort_by_date, true);
                activity.setMenuItemVisibility(R.id.menu_sort_by_name, true);
                activity.setMenuItemVisibility(R.id.menu_sort_by_place, true);
                activity.setMenuItemVisibility(R.id.menu_grid_1x1, false);
                activity.setMenuItemVisibility(R.id.menu_grid_2x1, false);
                activity.setMenuItemVisibility(R.id.menu_clear_expired_items, false);
            }
            mMaterialAdapter.notifyDataSetChanged();
        }

        @Override
        public void clear() {
            super.clear();
            mMaterialAdapter.clear();
            mTypeCountMap.clear();
            mTypeMaterialMap.clear();
            mtotalItem.clear();
            mSearchedItem.clear();
            mSearchStr = null;
        }

        @Override
        public void refreshSearch(String searchStr) {
            mSearchStr = searchStr;
            mSearchedItem.clear();

            if (searchStr == null || searchStr.isEmpty()) {
                mSearchedItem.addAll(mtotalItem);
            } else {
                String search = searchStr.toLowerCase(mLocale);
                String type;
                for (StreamItem item : mtotalItem) {
                    type = item.getMaterialType();

                    if (item.getMaterialType() != null && type.toLowerCase(mLocale).contains(search))
                        mSearchedItem.add(item);
                }
            }
            notifyDataSetChanged();
        }
    }

    private class MaterialAdapter extends ArrayAdapter<Material> implements ISearchUpdate {
        private final LayoutInflater mInflater;
        private ArrayList<Material> mSearchedItem;
        private ArrayList<Material> mTotalItem;
        private String mSearchStr;
        private int mScaledSize;

        public MaterialAdapter() {
            super(getActivity(), 0);
            mInflater = LayoutInflater.from(getActivity());
            mSearchedItem = new ArrayList<Material>();
            mTotalItem = new ArrayList<Material>();
            mSearchStr = null;
            float h = mMetrics.heightPixels / mMetrics.density;

            if (h > 900) {
                mScaledSize = (int) ((500 * mMetrics.density) + 0.5f);
            } else {
                mScaledSize = (int) ((300 * mMetrics.density) + 0.5f);
            }
        }

        public void sort(int mode) {
            switch (mode) {
                case R.id.menu_sort_by_date:
                    Collections.sort(mSearchedItem, new Comparator<Material>() {

                        @Override
                        public int compare(Material m1, Material m2) {
                            long m1Time = m1.getValidDate().getTimeInMillis();
                            long m2Time = m2.getValidDate().getTimeInMillis();

                            if (m1Time > m2Time)
                                return 1;
                            else if (m1Time < m2Time)
                                return -1;
                            else
                                return 0;

                        }
                    });
                    break;
                case R.id.menu_sort_by_name:
                    Collections.sort(mSearchedItem, new Comparator<Material>() {

                        @Override
                        public int compare(Material m1, Material m2) {
                            String m1Name = m1.getName();
                            String m2Name = m2.getName();

                            return m1Name.compareTo(m2Name);
                        }
                    });
                    break;
                case R.id.menu_sort_by_place:
                    Collections.sort(mSearchedItem, new Comparator<Material>() {

                        @Override
                        public int compare(Material m1, Material m2) {
                            String m1Place = m1.getMaterialPlace();
                            String m2Plcae = m2.getMaterialPlace();

                            return m1Place.compareTo(m2Plcae);
                        }
                    });
                    break;
            }

            notifyDataSetChanged();
        }

        public void clearExpiredMaterials() {
            Calendar today = Calendar.getInstance();
            long todayTimeInMillis = today.getTimeInMillis();
            Iterator<Material> ite = mSearchedItem.iterator();
            Material material;

            while (ite.hasNext()) {
                material = ite.next();
                Calendar validateDate = material.getValidDate();

                if (validateDate.getTimeInMillis() < todayTimeInMillis) {
                    ite.remove();
                    DBUtility.deleteMaterialInfo(material);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public void add(Material materialItem) {
            mTotalItem.add(materialItem);
        }

        @Override
        public void addAll(Collection<? extends Material> collection) {
            /* Avoid a bug that the last material is delete for a specific type*/
            if (collection == null) {
                return;
            }

            mTotalItem.addAll(collection);
        }

        @Override
        public void remove(Material materialItem) {
            mTotalItem.remove(materialItem);
        }

        public int getCount() {
            if (mSearchedItem == null)
                return 0;

            return mSearchedItem.size();
        }

        public Material getItem(int position) {
            return mSearchedItem.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewGroup view;
            final ViewHolder viewHolder;
            Material item = getItem(position);

            if (convertView == null) {
                view = (ViewGroup) mInflater.inflate(R.layout.material_item_layout, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.materialName = (TextView) view.findViewById(R.id.tv_material_item_name);
                viewHolder.materialType = (TextView) view.findViewById(R.id.tv_material_item_type);
                viewHolder.purchaceDate = (TextView) view.findViewById(R.id.tv_material_purchace_date);
                viewHolder.validDate = (TextView) view.findViewById(R.id.tv_material_valid_date);
                viewHolder.place = (TextView) view.findViewById(R.id.tv_material_place);
                viewHolder.comment = (TextView) view.findViewById(R.id.tv_material_comment);
                viewHolder.onnLoading = (RelativeLayout) view.findViewById(R.id.rl_on_loading);
                viewHolder.materialPic = (ImageView) view.findViewById(R.id.iv_material_pic);
                viewHolder.unit = (TextView) view.findViewById(R.id.tv_unit);
                viewHolder.expired = (TextView) view.findViewById(R.id.tv_material_expired);
                viewHolder.restDay = (TextView) view.findViewById(R.id.tv_rest_days);
                viewHolder.noValidDate = (TextView) view.findViewById(R.id.tv_valid_date_no_set_up);
                viewHolder.remindTick = view.findViewById(R.id.ll_reminder_label);

                view.setTag(viewHolder);
                changeLayoutConfig(view);
            } else {
                view = (ViewGroup) convertView;
                viewHolder = (ViewHolder) view.getTag();
            }

            String restDay = item.getResetDaysInfo();
            boolean isNeedBarcodeIc = (item.getBarcode() != null && !item.getBarcode().isEmpty()) ? true : false;

            viewHolder.restDay.setSelected(true);
            viewHolder.noValidDate.setSelected(true);
            viewHolder.onnLoading.setVisibility(View.VISIBLE);
            Picasso.with(mOwnerActivity).cancelRequest(viewHolder.materialPic);
            Picasso.with(mOwnerActivity).load(new File(item.getMaterialPicPath())).centerCrop().resize(mScaledSize, mScaledSize).into(viewHolder.materialPic, new Callback() {
                @Override
                public void onSuccess() {
                    viewHolder.onnLoading.setVisibility(View.GONE);
                    viewHolder.materialPic.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                        }
                    });
                }

                @Override
                public void onError() {
                    viewHolder.onnLoading.setVisibility(View.GONE);
                }
            });

            if (!isNeedBarcodeIc) {
                ((TextView) view.findViewById(R.id.tv_material_barcode)).setCompoundDrawables(null, null, null, null);
            } else {
                Drawable defaultBarcodeImg = getResources().getDrawable(R.drawable.ic_barcode_indicator);

                defaultBarcodeImg.setBounds(0, 0, defaultBarcodeImg.getIntrinsicWidth(),
                        defaultBarcodeImg.getIntrinsicHeight());
                ((TextView) view.findViewById(R.id.tv_material_barcode)).setCompoundDrawables(null, null,
                        defaultBarcodeImg, null);
            }

            viewHolder.materialName.setText(Utility.formatMatchedString(item.getName(), mSearchStr));
            viewHolder.materialName.setSelected(true);

            viewHolder.materialType.setText(Utility.formatMatchedString(item.getMaterialType(), mSearchStr));
            viewHolder.materialType.setSelected(true);

            viewHolder.purchaceDate.setText(Utility.formatMatchedString(Utility.transDateToString(item.getPurchaceDate().getTime()), mSearchStr));
            viewHolder.purchaceDate.setSelected(true);

            viewHolder.validDate.setText(Utility.formatMatchedString(Utility.transDateToString(item.getValidDate().getTime()), mSearchStr));
            viewHolder.validDate.setSelected(true);

            viewHolder.place.setText(Utility.formatMatchedString(item.getMaterialPlace(), mSearchStr));
            viewHolder.place.setSelected(true);

            viewHolder.comment.setText(Utility.formatMatchedString(item.getComment(), mSearchStr));
            viewHolder.comment.setSelected(true);

            /* Check out */
            if (item.getIsValidDateSetup() == 1) {
                viewHolder.noValidDate.setVisibility(View.GONE);
                /* if material has been expired, then change the text color as red. */
                if (restDay.equals(mResources.getString(R.string.msg_expired))) {
                /* View item maybe cached before, so we need to re-assign the background color */
                    viewHolder.restDay.setVisibility(View.GONE);
                    viewHolder.unit.setVisibility(View.GONE);
                    viewHolder.remindTick.setVisibility(View.GONE);
                    viewHolder.expired.setVisibility(View.VISIBLE);
                    viewHolder.expired.setBackgroundResource(R.color.red);

                    viewHolder.expired.setText(Utility.formatMatchedString(restDay, mSearchStr));
                } else {
                /* View item maybe cached before, so we need to re-assign the background color */
                    viewHolder.restDay.setVisibility(View.VISIBLE);
                    viewHolder.unit.setVisibility(View.VISIBLE);
                    viewHolder.remindTick.setVisibility(View.VISIBLE);
                    viewHolder.expired.setVisibility(View.GONE);

                    int days = Integer.parseInt(restDay);

                    if (days <= 10) {
                        viewHolder.remindTick.setBackgroundResource(R.color.light_brown_label);
                    } else {
                        viewHolder.remindTick.setBackgroundResource(R.color.light_green_label);
                    }

                    viewHolder.restDay.setText(Utility.formatMatchedString(restDay, mSearchStr));
                }
            } else {
                viewHolder.noValidDate.setVisibility(View.VISIBLE);
                viewHolder.noValidDate.setBackgroundResource(R.color.gray);
                viewHolder.restDay.setVisibility(View.GONE);
                viewHolder.unit.setVisibility(View.GONE);
                viewHolder.remindTick.setVisibility(View.GONE);
                viewHolder.expired.setVisibility(View.GONE);
                viewHolder.validDate.setText("N/A");
            }

            return view;
        }

        private class ViewHolder {
            RelativeLayout onnLoading;
            ImageView materialPic;
            TextView materialName;
            TextView materialType;
            TextView purchaceDate;
            TextView validDate;
            TextView place;
            TextView comment;
            TextView restDay;
            TextView unit;
            TextView expired;
            TextView noValidDate;
            View remindTick;
        }

        @Override
        public void refreshSearch(String searchStr) {
            mSearchStr = searchStr;
            mSearchedItem.clear();

            if (searchStr == null || searchStr.isEmpty()) {
                for (Material item : mTotalItem) {
                    /* Calculate the diff days , FIXME: Check the memory and performance... */
                    String reset_days_str;
                    Calendar c1 = Calendar.getInstance();
                    Calendar c2 = Calendar.getInstance();
                    Calendar c3 = item.getValidDate();
                    c1.set(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH), c1.get(Calendar.DAY_OF_MONTH));
                    c2.set(c3.get(Calendar.YEAR), c3.get(Calendar.MONTH), c3.get(Calendar.DAY_OF_MONTH));
                    long daysDiff = (c2.getTimeInMillis() - c1.getTimeInMillis()) / (24 * 60 * 60 * 1000);
                    reset_days_str = (daysDiff <= 0) ? mResources.getString(R.string.msg_expired) : Long
                            .toString(daysDiff);
                    item.setResetDaysInfo(reset_days_str);
                }

                mSearchedItem.addAll(mTotalItem);
            } else {
                String search = searchStr.toLowerCase(mLocale);
                String name;
                String type;
                String comment;
                String place;
                String purchDate;
                String validDate;
                String reset_days_str;
                for (Material item : mTotalItem) {
                    name = item.getName();
                    type = item.getMaterialType();
                    comment = item.getComment();
                    place = item.getMaterialPlace();
                    purchDate = Utility.transDateToString(item.getPurchaceDate().getTime());
                    validDate = Utility.transDateToString(item.getValidDate().getTime());

                    /* To update the material information after modifying material info */
                    Calendar c1 = Calendar.getInstance();
                    Calendar c2 = Calendar.getInstance();
                    Calendar c3 = item.getValidDate();
                    c1.set(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH), c1.get(Calendar.DAY_OF_MONTH));
                    c2.set(c3.get(Calendar.YEAR), c3.get(Calendar.MONTH), c3.get(Calendar.DAY_OF_MONTH));
                    long daysDiff = (c2.getTimeInMillis() - c1.getTimeInMillis()) / (24 * 60 * 60 * 1000);
                    reset_days_str = (daysDiff <= 0) ? mResources.getString(R.string.msg_expired) : Long
                            .toString(daysDiff);
                    item.setResetDaysInfo(reset_days_str);

                    if ((name != null && name.toLowerCase(mLocale).contains(search))
                            || (type != null && type.toLowerCase(mLocale).contains(search))
                            || (comment != null && comment.toLowerCase(mLocale).contains(search))
                            || (place != null && place.toLowerCase(mLocale).contains(search))
                            || (validDate != null && validDate.toLowerCase(mLocale).contains(search))
                            || (purchDate != null && purchDate.toLowerCase(mLocale).contains(search))
                            || (reset_days_str != null && reset_days_str.toLowerCase(mLocale).contains(search)))
                        mSearchedItem.add(item);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public void clear() {
            mTotalItem.clear();
            mSearchedItem.clear();
            mSearchStr = null;
            super.clear();
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mSearchString = newText;

        if (mGvMaterialType.getAdapter() instanceof ISearchUpdate) {
            ISearchUpdate searchUpdate = (ISearchUpdate) mGvMaterialType.getAdapter();

            searchUpdate.refreshSearch(mSearchString);
        }
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {

        return false;
    }
}