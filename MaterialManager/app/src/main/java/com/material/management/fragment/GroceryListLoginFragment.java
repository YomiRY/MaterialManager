package com.material.management.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;
import com.loopj.android.http.RequestParams;
import com.material.management.MMFragment;
import com.material.management.MainActivity;
import com.material.management.Observer;
import com.material.management.StoreMapActivity;
import com.material.management.R;
import com.material.management.api.module.ConnectionControl;
import com.material.management.data.GroceryListData;
import com.material.management.data.StoreData;
import com.material.management.utils.DBUtility;
import com.material.management.utils.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroceryListLoginFragment extends MMFragment implements Observer, TimePickerDialog.OnTimeSetListener, CompoundButton.OnCheckedChangeListener {
    public static final String ACTION_BAR_BTN_ACTION_ADD = "add_material";
    public static final String ACTION_BAR_BTN_ACTION_CLEAR = "clear_user_input";
    private static final String REQ_PLACE_SEARCH = "geo_transform";
    private static final String TIMEPICKER_TAG = "timepicker";
    private static MainActivity sActivity;

    private View mLayout;
    private LinearLayout mLlGroceryListSettings;
    private AutoCompleteTextView mActGroceryListName;
    private AutoCompleteTextView mActStoreName;
    private AutoCompleteTextView mActAddress;
    private AutoCompleteTextView mActPhone;
    private ImageView mIvStoreAddress;
    private ImageView mIvPhone;
    private ImageView mIvNavig;
    private CheckBox mCbNearbyAlert;
    private Button mBtnSun, mBtnPMSun;
    private Button mBtnMon, mBtnPMMon;
    private Button mBtnTue, mBtnPMTue;
    private Button mBtnWed, mBtnPMWed;
    private Button mBtnThu, mBtnPMThu;
    private Button mBtnFri, mBtnPMFri;
    private Button mBtnSat, mBtnPMSat;
    private Button mBtnCurTimeSetting;

    private Activity mOwnerActivity = null;
    private TimePickerDialog mTimePickerDialog = null;
    private ArrayAdapter<String> mTextHistAdapter = null;
    private ArrayList<String> mTextHistoryList;
    private String mTitle;
    private String mCurModifiedAddress;
    private StoreData mSelectedStoreData = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mLayout = inflater.inflate(R.layout.fragment_grocery_login, container, false);
        sActivity = (MainActivity) getActivity();

        findView();
        init();
        setListener();

        return mLayout;
    }

    @Override
    public void onResume() {
        sendScreenAnalytics(getString(R.string.ga_app_view_grocery_list_login_fragment));
        super.onResume();
    }

    private void findView() {
        mIvPhone = (ImageView) mLayout.findViewById(R.id.iv_phoneButton);
        mIvNavig = (ImageView) mLayout.findViewById(R.id.iv_navigateButton);
        mIvStoreAddress = (ImageView) mLayout.findViewById(R.id.iv_store_address);
        mActGroceryListName = (AutoCompleteTextView) mLayout.findViewById(R.id.act_grocery_list_name);
        mActStoreName = (AutoCompleteTextView) mLayout.findViewById(R.id.act_store_name);
        mActAddress = (AutoCompleteTextView) mLayout.findViewById(R.id.act_addressText);
        mActPhone = (AutoCompleteTextView) mLayout.findViewById(R.id.act_phoneText);
        mLlGroceryListSettings = (LinearLayout) mLayout.findViewById(R.id.ll_grocery_list_settings);
        mCbNearbyAlert = (CheckBox) mLayout.findViewById(R.id.cb_nearby_alert_enable);
        mBtnSun = (Button) mLayout.findViewById(R.id.btn_sunTimings);
        mBtnPMSun = (Button) mLayout.findViewById(R.id.btn_sunPMTimings);
        mBtnMon = (Button) mLayout.findViewById(R.id.btn_monTimings);
        mBtnPMMon = (Button) mLayout.findViewById(R.id.btn_monPMTimings);
        mBtnTue = (Button) mLayout.findViewById(R.id.btn_tueTimings);
        mBtnPMTue = (Button) mLayout.findViewById(R.id.btn_tuePMTimings);
        mBtnWed = (Button) mLayout.findViewById(R.id.btn_wedTimings);
        mBtnPMWed = (Button) mLayout.findViewById(R.id.btn_wedPMTimings);
        mBtnThu = (Button) mLayout.findViewById(R.id.btn_thuTimings);
        mBtnPMThu = (Button) mLayout.findViewById(R.id.btn_thuPMTimings);
        mBtnFri = (Button) mLayout.findViewById(R.id.btn_friTimings);
        mBtnPMFri = (Button) mLayout.findViewById(R.id.btn_friPMTimings);
        mBtnSat = (Button) mLayout.findViewById(R.id.btn_satTimings);
        mBtnPMSat = (Button) mLayout.findViewById(R.id.btn_satPMTimings);

    }

    private void init() {
        mOwnerActivity = getActivity();
        mTitle = mOwnerActivity.getActionBar().getTitle().toString();
        Calendar calendar = Calendar.getInstance();
        mTimePickerDialog = TimePickerDialog.newInstance(this, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);

        update(null);
        initAutoCompleteData();
        clearUserData();
    }

    private void setListener() {
        mIvPhone.setOnClickListener(this);
        mIvNavig.setOnClickListener(this);
        mIvStoreAddress.setOnClickListener(this);
        mBtnSun.setOnClickListener(this);
        mBtnPMSun.setOnClickListener(this);
        mBtnMon.setOnClickListener(this);
        mBtnPMMon.setOnClickListener(this);
        mBtnTue.setOnClickListener(this);
        mBtnPMTue.setOnClickListener(this);
        mBtnWed.setOnClickListener(this);
        mBtnPMWed.setOnClickListener(this);
        mBtnThu.setOnClickListener(this);
        mBtnPMThu.setOnClickListener(this);
        mBtnFri.setOnClickListener(this);
        mBtnPMFri.setOnClickListener(this);
        mBtnSat.setOnClickListener(this);
        mBtnPMSat.setOnClickListener(this);
        mCbNearbyAlert.setOnCheckedChangeListener(this);
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

    private void initAutoCompleteData() {
        if (mTextHistAdapter == null) {
            mTextHistoryList = new ArrayList<String>();

            mTextHistoryList.addAll(Arrays.asList(Utility.getStringValueForKey(Utility.INPUT_TEXT_HISTORY).split(":")));

            mTextHistAdapter = new ArrayAdapter<String>(sActivity, android.R.layout.simple_dropdown_item_1line, mTextHistoryList);
        } else {
            mTextHistAdapter.clear();
            mTextHistAdapter.addAll(mTextHistoryList);
        }
        mActGroceryListName.setAdapter(mTextHistAdapter);
        mActStoreName.setAdapter(mTextHistAdapter);
        mActAddress.setAdapter(mTextHistAdapter);
        mActPhone.setAdapter(mTextHistAdapter);
        mTextHistAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {}

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int id = v.getId();

        switch (id) {
            case R.id.iv_phoneButton: {
                String phone = Uri.encode(mActPhone.getText().toString());

                if (phone.isEmpty()) {
                    showToast(getString(R.string.grocery_login_err_empty_phone));
                } else {
                    Intent intent = new Intent(Intent.ACTION_DIAL);

                    intent.setData(Uri.parse("tel:" + phone));
                    startActivity(intent);
                }
            }
            break;

            case R.id.iv_navigateButton: {
                mCurModifiedAddress = mActAddress.getText().toString();

                if (mCurModifiedAddress.isEmpty()) {
                    showToast(getString(R.string.grocery_login_err_empty_store_address));
                } else {
                    RequestParams reqParams = new RequestParams();

                    reqParams.put("address", mCurModifiedAddress);
                    reqParams.put("sensor", Boolean.toString(true));
                    reqParams.put("language", mDeviceInfo.getLanguage() + "-" + mDeviceInfo.getLocale());

                    mControl.getData(ConnectionControl.GEO_ADDRESS_TRANSFORM, GroceryListLoginFragment.this, reqParams, REQ_PLACE_SEARCH);

                    showProgressDialog(null, getString(R.string.grocery_login_msg_geo_trans));
                }
            }
            break;

            case R.id.iv_store_address: {
                Intent intent = new Intent(getActivity(), StoreMapActivity.class);

                intent.putExtra("title", mTitle);
                mIvStoreAddress.startAnimation(AnimationUtils.loadAnimation(mOwnerActivity, R.anim.anim_press_bounce));
                startActivityForResult(intent, 0);
            }
            break;

            case R.id.btn_sunTimings:
            case R.id.btn_sunPMTimings:
            case R.id.btn_monTimings:
            case R.id.btn_monPMTimings:
            case R.id.btn_tueTimings:
            case R.id.btn_tuePMTimings:
            case R.id.btn_wedTimings:
            case R.id.btn_wedPMTimings:
            case R.id.btn_thuTimings:
            case R.id.btn_thuPMTimings:
            case R.id.btn_friTimings:
            case R.id.btn_friPMTimings:
            case R.id.btn_satTimings:
            case R.id.btn_satPMTimings: {
                mBtnCurTimeSetting = (Button) v;
                mTimePickerDialog.show(mOwnerActivity.getFragmentManager(), TIMEPICKER_TAG);
            }
            break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            mActStoreName.setText("");
            mActPhone.setText("");
            mActAddress.setText("");
            return;
        }

        mSelectedStoreData = data.getParcelableExtra("store_data");

        updateStoreInfo();
    }

    private void updateStoreInfo() {
        mActStoreName.setText(mSelectedStoreData.getStoreName());
        mActAddress.setText(mSelectedStoreData.getStoreAddress());
        mActPhone.setText(mSelectedStoreData.getStorePhone());
        String serviceTime = mSelectedStoreData.getStoreServiceTime();
        String defaultStr = getString(R.string.title_service_time_default);

        mBtnSun.setText(defaultStr);
        mBtnPMSun.setText(defaultStr);
        mBtnMon.setText(defaultStr);
        mBtnPMMon.setText(defaultStr);
        mBtnTue.setText(defaultStr);
        mBtnPMTue.setText(defaultStr);
        mBtnWed.setText(defaultStr);
        mBtnPMWed.setText(defaultStr);
        mBtnThu.setText(defaultStr);
        mBtnPMThu.setText(defaultStr);
        mBtnFri.setText(defaultStr);
        mBtnPMFri.setText(defaultStr);
        mBtnSat.setText(defaultStr);
        mBtnPMSat.setText(defaultStr);

        if (serviceTime != null && !serviceTime.isEmpty()) {
            String[] daysHours = serviceTime.split("\\|");
            Pattern patt = Pattern.compile("(\\d+)#(\\d+),(\\d+)");


            for (String dayHour : daysHours) {
                Matcher match = patt.matcher(dayHour);

                if (match.matches()) {
                    int day = Integer.parseInt(match.group(1));
                    String startTime = match.group(2);
                    String endTime = match.group(3);

                    switch (day) {
                        case 0:
                            mBtnSun.setText(startTime);
                            mBtnPMSun.setText(endTime);
                            break;
                        case 1:
                            mBtnMon.setText(startTime);
                            mBtnPMMon.setText(endTime);
                            break;
                        case 2:
                            mBtnTue.setText(startTime);
                            mBtnPMTue.setText(endTime);
                            break;
                        case 3:
                            mBtnWed.setText(startTime);
                            mBtnPMWed.setText(endTime);
                            break;
                        case 4:
                            mBtnThu.setText(startTime);
                            mBtnPMThu.setText(endTime);
                            break;
                        case 5:
                            mBtnFri.setText(startTime);
                            mBtnPMFri.setText(endTime);
                            break;
                        case 6:
                            mBtnSat.setText(startTime);
                            mBtnPMSat.setText(endTime);
                            break;
                    }
                }
            }
        }
    }

    private String fixTime(int c) {
        if (c >= 10)
            return String.valueOf(c);
        else
            return "0" + String.valueOf(c);
    }


    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
        mBtnCurTimeSetting.setText(fixTime(hourOfDay) + fixTime(minute));
    }

    @Override
    public void callbackFromController(JSONObject jsonObj, String req) {
        try {
            if (req.equals(REQ_PLACE_SEARCH)) {
                String status = jsonObj.getString("status");

                if (status.equals("OK")) {
                    JSONArray resultAry = jsonObj.getJSONArray("results");

                    for (int i = 0, len = resultAry.length(); i < len; i++) {
                        JSONObject geoJsonObject = resultAry.getJSONObject(i);
                        String formattedAddress = geoJsonObject.getString("formatted_address");

                        if (formattedAddress.contains(mCurModifiedAddress)) {
                            JSONObject locJsonObject = geoJsonObject.getJSONObject("geometry").getJSONObject("location");
                            String lat = locJsonObject.getString("lat");
                            String lng = locJsonObject.getString("lng");
                            String uri = String.format("geo:%s,%s?q=%s,%s(%s)", lat, lng, lat, lng, mCurModifiedAddress);
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));

                            startActivity(intent);
                            break;
                        }
                    }
                } else {
//                "OK" 表示沒有發生任何錯誤；地址的剖析已成功完成且至少傳回一個地理編碼。
//                "ZERO_RESULTS" 表示地理編碼成功，但是並未傳回任何結果。如果該地理編碼收到了不存在的 address 或遠端位置的 latlng，就有可能發生這種情況。
//                "OVER_QUERY_LIMIT" 表示您超過配額了。
//                "REQUEST_DENIED" 表示您的要求已遭拒絕，通常是因為缺少 sensor 參數。
//                "INVALID_REQUEST" 一般表示查詢 (address 或 latlng) 遺失了。
                    showAlertDialog(null, getString(R.string.grocery_login_err_dialog_msg), getString(R.string.title_positive_btn_label), null, null, null);

                }
                closeProgressDialog();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Object data) {
        if (sActivity == null) {
            return;
        }

        if (data != null) {
            if (data.equals(ACTION_BAR_BTN_ACTION_ADD)) {
                GroceryListData groceryListData = new GroceryListData();
                StringBuilder serviceTimeStr = new StringBuilder("");

                groceryListData.setGroceryListName(mActGroceryListName.getText().toString());
                groceryListData.setIsAlertWhenNearBy(mCbNearbyAlert.isChecked() ? 1 : 0);
                groceryListData.setStoreName(mActStoreName.getText().toString());
                groceryListData.setAddress(mActAddress.getText().toString());
                groceryListData.setPhone(mActPhone.getText().toString());
                /* The service time for each is  [week day]:[start time]~[end time] and each day is separated by "|"*/
                serviceTimeStr.append("1:").append(mBtnMon.getText().toString()).append("~").append(mBtnPMMon.getText()).append("|");
                serviceTimeStr.append("2:").append(mBtnTue.getText().toString()).append("~").append(mBtnPMThu.getText()).append("|");
                serviceTimeStr.append("3:").append(mBtnWed.getText().toString()).append("~").append(mBtnPMWed.getText()).append("|");
                serviceTimeStr.append("4:").append(mBtnThu.getText().toString()).append("~").append(mBtnPMThu.getText()).append("|");
                serviceTimeStr.append("5:").append(mBtnFri.getText().toString()).append("~").append(mBtnPMFri.getText()).append("|");
                serviceTimeStr.append("6:").append(mBtnSat.getText().toString()).append("~").append(mBtnPMSat.getText()).append("|");
                serviceTimeStr.append("7:").append(mBtnSun.getText().toString()).append("~").append(mBtnPMSun.getText());
                /* replace the default string in service time string.*/
                String serviceTimeDefaultInDb = getString(R.string.title_service_time_default_in_db);
                String serviceTimeDefault = getString(R.string.title_service_time_default);
                int index;
                int len = serviceTimeDefault.length();

                while((index = serviceTimeStr.indexOf(serviceTimeDefault)) != -1) {
                    serviceTimeStr.replace(index, index + len , serviceTimeDefaultInDb);
                }
                groceryListData.setServiceTime(serviceTimeStr.toString());
                groceryListData.setLat(mSelectedStoreData.getStoreLat());
                groceryListData.setLong(mSelectedStoreData.getStoreLong());

                updateTextHistory(groceryListData.getGroceryListName(), groceryListData.getStoreName(), groceryListData.getPhone(), groceryListData.getAddress());
                DBUtility.insertGroceryListInfo(groceryListData);
            } else if (data.equals(ACTION_BAR_BTN_ACTION_CLEAR)) {}
            clearUserData();
        } else {
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
    }


    private void clearUserData() {
        mActGroceryListName.setText("");
        mActStoreName.setText("");
        mActAddress.setText("");
        mActPhone.setText("");

        String defaultStr = getString(R.string.title_service_time_default);

        mBtnSun.setText(defaultStr);
        mBtnPMSun.setText(defaultStr);
        mBtnMon.setText(defaultStr);
        mBtnPMMon.setText(defaultStr);
        mBtnTue.setText(defaultStr);
        mBtnPMTue.setText(defaultStr);
        mBtnWed.setText(defaultStr);
        mBtnPMWed.setText(defaultStr);
        mBtnThu.setText(defaultStr);
        mBtnPMThu.setText(defaultStr);
        mBtnFri.setText(defaultStr);
        mBtnPMFri.setText(defaultStr);
        mBtnSat.setText(defaultStr);
        mBtnPMSat.setText(defaultStr);
    }
}
