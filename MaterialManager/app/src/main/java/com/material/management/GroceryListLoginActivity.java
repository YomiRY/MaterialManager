package com.material.management;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.loopj.android.http.RequestParams;
import com.material.management.api.module.ConnectionControl;
import com.material.management.data.StoreData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Deprecated Activity
 */
public class GroceryListLoginActivity extends MMActivity implements TimePickerDialog.OnTimeSetListener, CompoundButton.OnCheckedChangeListener {
    private static final String REQ_PLACE_SEARCH = "geo_transform";
    private static final String TIMEPICKER_TAG = "timepicker";

    private View mLayout;
    private LinearLayout mLlGroceryListSettings;
    private EditText mEtStoreName;
    private EditText mEtAddress;
    private EditText mEtPhone;
    private ImageView mIvStoreAddress;
    private ImageView mIvPhone;
    private ImageView mIvNavig;
    private AutoCompleteTextView mActGroceryListName;
    private CheckBox mCbNearbyAlert;
    private Button mBtnSun, mBtnPMSun;
    private Button mBtnMon, mBtnPMMon;
    private Button mBtnTue, mBtnPMTue;
    private Button mBtnWed, mBtnPMWed;
    private Button mBtnThu, mBtnPMThu;
    private Button mBtnFri, mBtnPMFri;
    private Button mBtnSat, mBtnPMSat;
    private Button mBtnCurTimeSetting;

    private TimePickerDialog mTimePickerDialog = null;
    private String mTitle;
    private String mCurModifiedAddress;
    private StoreData mSelectedStoreData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_grocery_login);
//        mLayout = mInflater.inflate(R.layout.fragment_grocery_login, null);
//        setContentView(mLayout);
        setTitle(mTitle);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        findView();
        init();
        setListener();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

    private void findView() {
//        mIvPhone = (ImageView) mLayout.findViewById(R.id.iv_phoneButton);
//        mIvNavig = (ImageView) mLayout.findViewById(R.id.iv_navigateButton);
//        mIvStoreAddress = (ImageView) mLayout.findViewById(R.id.iv_store_address);
//        mEtStoreName = (EditText) mLayout.findViewById(R.id.act_store_name);
//        mEtAddress = (EditText) mLayout.findViewById(R.id.act_addressText);
//        mEtPhone = (EditText) mLayout.findViewById(R.id.act_phoneText);
//        mLlGroceryListSettings = (LinearLayout) mLayout.findViewById(R.id.ll_grocery_list_settings);
//        mActGroceryListName = (AutoCompleteTextView) mLayout.findViewById(R.id.act_grocery_list_name);
//        mCbNearbyAlert = (CheckBox) mLayout.findViewById(R.id.cb_nearby_alert_enable);
//        mBtnSun = (Button) mLayout.findViewById(R.id.btn_sunTimings);
//        mBtnPMSun = (Button) mLayout.findViewById(R.id.btn_sunPMTimings);
//        mBtnMon = (Button) mLayout.findViewById(R.id.btn_monTimings);
//        mBtnPMMon = (Button) mLayout.findViewById(R.id.btn_monPMTimings);
//        mBtnTue = (Button) mLayout.findViewById(R.id.btn_tueTimings);
//        mBtnPMTue = (Button) mLayout.findViewById(R.id.btn_tuePMTimings);
//        mBtnWed = (Button) mLayout.findViewById(R.id.btn_wedTimings);
//        mBtnPMWed = (Button) mLayout.findViewById(R.id.btn_wedPMTimings);
//        mBtnThu = (Button) mLayout.findViewById(R.id.btn_thuTimings);
//        mBtnPMThu = (Button) mLayout.findViewById(R.id.btn_thuPMTimings);
//        mBtnFri = (Button) mLayout.findViewById(R.id.btn_friTimings);
//        mBtnPMFri = (Button) mLayout.findViewById(R.id.btn_friPMTimings);
//        mBtnSat = (Button) mLayout.findViewById(R.id.btn_satTimings);
//        mBtnPMSat = (Button) mLayout.findViewById(R.id.btn_satPMTimings);

        mIvPhone = (ImageView) findViewById(R.id.iv_phoneButton);
        mIvNavig = (ImageView) findViewById(R.id.iv_navigateButton);
        mIvStoreAddress = (ImageView) findViewById(R.id.iv_store_address);
        mEtStoreName = (EditText) findViewById(R.id.act_store_name);
        mEtAddress = (EditText) findViewById(R.id.act_addressText);
        mEtPhone = (EditText) findViewById(R.id.act_phoneText);
        mLlGroceryListSettings = (LinearLayout) findViewById(R.id.ll_grocery_list_settings);
        mActGroceryListName = (AutoCompleteTextView) findViewById(R.id.act_grocery_list_name);
        mCbNearbyAlert = (CheckBox) findViewById(R.id.cb_nearby_alert_enable);
        mBtnSun = (Button) findViewById(R.id.btn_sunTimings);
        mBtnPMSun = (Button) findViewById(R.id.btn_sunPMTimings);
        mBtnMon = (Button) findViewById(R.id.btn_monTimings);
        mBtnPMMon = (Button) findViewById(R.id.btn_monPMTimings);
        mBtnTue = (Button) findViewById(R.id.btn_tueTimings);
        mBtnPMTue = (Button) findViewById(R.id.btn_tuePMTimings);
        mBtnWed = (Button) findViewById(R.id.btn_wedTimings);
        mBtnPMWed = (Button) findViewById(R.id.btn_wedPMTimings);
        mBtnThu = (Button) findViewById(R.id.btn_thuTimings);
        mBtnPMThu = (Button) findViewById(R.id.btn_thuPMTimings);
        mBtnFri = (Button) findViewById(R.id.btn_friTimings);
        mBtnPMFri = (Button) findViewById(R.id.btn_friPMTimings);
        mBtnSat = (Button) findViewById(R.id.btn_satTimings);
        mBtnPMSat = (Button) findViewById(R.id.btn_satPMTimings);
    }

    private void init() {
        mTitle = getString(R.string.title_grocery_list_modify_actionbar_title);
        Calendar calendar = Calendar.getInstance();
        mTimePickerDialog = TimePickerDialog.newInstance(this, calendar.get(Calendar.HOUR_OF_DAY) ,calendar.get(Calendar.MINUTE), false);
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

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int id = v.getId();

        switch (id) {
            case R.id.iv_phoneButton: {
                String phone = Uri.encode(mEtPhone.getText().toString());

                if(phone.isEmpty()) {
                    showToast(getString(R.string.grocery_login_err_empty_phone));
                } else {
                    Intent intent = new Intent(Intent.ACTION_DIAL);

                    intent.setData(Uri.parse("tel:" + phone));
                    startActivity(intent);
                }
            }
            break;

            case R.id.iv_navigateButton: {
                mCurModifiedAddress = mEtAddress.getText().toString();

                if(mCurModifiedAddress.isEmpty()) {
                    showToast(getString(R.string.grocery_login_err_empty_store_address));
                } else {
                    RequestParams reqParams = new RequestParams();

                    reqParams.put("address", mCurModifiedAddress);
                    reqParams.put("sensor", Boolean.toString(true));
                    reqParams.put("language", mDeviceInfo.getLanguage() + "-" + mDeviceInfo.getLocale());

                    mControl.getData(ConnectionControl.GEO_ADDRESS_TRANSFORM, GroceryListLoginActivity.this, reqParams, REQ_PLACE_SEARCH);

                    showProgressDialog(null, getString(R.string.grocery_login_msg_geo_trans));
                }
            }
            break;

            case R.id.iv_store_address: {
                Intent intent = new Intent(GroceryListLoginActivity.this, StoreMapActivity.class);

                intent.putExtra("title", mTitle);
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
                mTimePickerDialog.show(getFragmentManager(), TIMEPICKER_TAG);
            }
            break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(data == null) {
            mEtStoreName.setText("");
            mEtPhone.setText("");
            mEtAddress.setText("");
            return;
        }

        mSelectedStoreData = data.getParcelableExtra("store_data");

        updateStoreInfo();
    }

    private void updateStoreInfo() {
        mEtStoreName.setText(mSelectedStoreData.getStoreName());
        mEtAddress.setText(mSelectedStoreData.getStoreAddress());
        mEtPhone.setText(mSelectedStoreData.getStorePhone());
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

        if(serviceTime != null && !serviceTime.isEmpty()) {
            String[] daysHours = serviceTime.split("\\|");
            Pattern patt = Pattern.compile("(\\d+)#(\\d+),(\\d+)");


            for(String dayHour : daysHours) {
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

    private String fixTime(int c){
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

                    for(int i = 0 ,len = resultAry.length() ; i < len ; i++) {
                        JSONObject geoJsonObject = resultAry.getJSONObject(i);
                        String formattedAddress = geoJsonObject.getString("formatted_address");

                        if(formattedAddress.contains(mCurModifiedAddress)) {
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

}
