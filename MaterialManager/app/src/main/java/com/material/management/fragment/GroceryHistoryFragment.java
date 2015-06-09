package com.material.management.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.datetimepicker.date.DatePickerDialog;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ValueFormatter;
import com.material.management.MMFragment;
import com.material.management.MainActivity;
import com.material.management.Observer;
import com.material.management.R;
import com.material.management.data.GroceryItem;
import com.material.management.data.GroceryListData;
import com.material.management.dialog.ReceiptDialog;
import com.material.management.utils.DBUtility;
import com.material.management.utils.Utility;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;


public class GroceryHistoryFragment extends MMFragment implements Observer, OnChartValueSelectedListener, DatePickerDialog.OnDateSetListener {
    private static final String DATEPICKER_TAG = "datepicker";

    private View mLayout;
    private Button mBtnStartMonth;
    private Button mBtnEndMonth;
    private ImageView mIvFilter;
    private LineChart mLcChart;
    private ListView mLvItemDetails;
    private TextView mTvNoChartDataUpper;
    private TextView mTvNoChartDataBottom;

    private int mCurPressDateBtnId = -1;
    private DatePickerDialog mDatePickerDialog;
    private ReceiptDialog mReceiptDialog = null;
    private Calendar mEndDate = null;
    private Calendar mStartDate = null;
    private ArrayList<GroceryListData> mGreceryList = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mLayout = inflater.inflate(R.layout.fragment_grocery_history, container, false);

        findView();
        init();
        initListener();
        changeLayoutConfig(mLayout);

        return mLayout;
    }

    private void findView() {
        mBtnStartMonth = (Button) mLayout.findViewById(R.id.btn_start_month);
        mBtnEndMonth = (Button) mLayout.findViewById(R.id.btn_end_month);
        mIvFilter = (ImageView) mLayout.findViewById(R.id.iv_grocery_list_history_filter);
        mLcChart = (LineChart) mLayout.findViewById(R.id.lc_chart);
        mLvItemDetails = (ListView) mLayout.findViewById(R.id.lv_grocery_item_details);
        mTvNoChartDataUpper = (TextView) mLayout.findViewById(R.id.tv_no_chart_data_upper);
        mTvNoChartDataBottom = (TextView) mLayout.findViewById(R.id.tv_no_chart_data_bottom);
    }

    private void init() {
        mGreceryList = new ArrayList<GroceryListData>();
        {
            /* Statistic total count for each grocery filtered by day range. */
            mLcChart.setUnit(" $");
            mLcChart.setDrawUnitsInChart(true);
            // if enabled, the chart will always start at zero on the y-axis
            mLcChart.setStartAtZero(false);
            // disable the drawing of values into the chart
            mLcChart.setDrawYValues(false);
            mLcChart.setDrawBorder(true);
            mLcChart.setBorderPositions(new BarLineChartBase.BorderPosition[]{
                    BarLineChartBase.BorderPosition.BOTTOM
            });
            // enable value highlighting
            mLcChart.setHighlightEnabled(true);
            // enable touch gestures
            mLcChart.setTouchEnabled(true);
            // enable scaling and dragging
            mLcChart.setDragEnabled(true);
            mLcChart.setScaleEnabled(true);
            mLcChart.setDrawVerticalGrid(true);
            mLcChart.setDrawHorizontalGrid(true);
            // if disabled, scaling can be done on x- and y-axis separately
            mLcChart.setPinchZoom(true);
            mLcChart.setOnChartValueSelectedListener(this);
            /* Avoid the first and last label be clipped if too long. */
            mLcChart.getXLabels().setAvoidFirstLastClipping(true);
            mLcChart.getYLabels().setFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float v) {
                    return Utility.convertDecimalFormat((long) v, "#,###,###,###");
                }
            });
            mLcChart.setNoDataTextDescription("");
            mLcChart.setDescription("");
            mLcChart.setNoDataText("");
        }

        update(null);
    }

    private void initListener() {
        mLcChart.setOnChartValueSelectedListener(this);
        mBtnStartMonth.setOnClickListener(this);
        mBtnEndMonth.setOnClickListener(this);
        mIvFilter.setOnClickListener(this);
    }

    private void initGroceryListHistoryChart() {
        ArrayList<String> xVals = new ArrayList<String>();
        ArrayList<Entry> yVals = new ArrayList<Entry>();
        int i = 0;

        for (GroceryListData groceryListData : mGreceryList) {
            xVals.add(Utility.transDateToString("yyyy-MM-dd HH:mm:ss", groceryListData.getCheckOutTime()));
            yVals.add(new Entry((float) groceryListData.getTotalCost(), i));
            ++i;
        }

        // create a dataset and give it a type
        LineDataSet set = new LineDataSet(yVals, getString(R.string.title_grocery_history_line_chart_desc));

        // set.enableDashedLine(10f, 5f, 0f);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(ColorTemplate.getHoloBlue());
        set.setLineWidth(2f);
        set.setCircleSize(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));

        // add the datasets
        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();

        dataSets.add(set);

        // create a data object with the datasets
        LineData data = new LineData(xVals, dataSets);
        // set data
        mLcChart.clear();
        mLcChart.setData(data);
        mLcChart.invalidate();
    }

    @Override
    public void onResume() {
        sendScreenAnalytics(getString(R.string.ga_app_view_grocery_history_fragment));

        super.onResume();
    }


    @Override
    public void onValueSelected(Entry entry, int i) {
        if (entry != null) {
            GroceryListData groceryListData = mGreceryList.get(entry.getXIndex());
            String groceryListName = groceryListData.getGroceryListName();
            ArrayList<GroceryItem> groceryItems = DBUtility.selectGroceryItemsById(groceryListData.getId());
            mReceiptDialog = new ReceiptDialog(mOwnerActivity, getString(R.string.title_grocery_item_receipt, groceryListName), groceryListName, groceryItems, null, true, groceryListData.getCheckOutTime());

            mReceiptDialog.setShowState(true);
            mReceiptDialog.show();
        }
    }

    @Override
    public void onNothingSelected() {
    }

    @Override
    public void update(Object data) {
        if (mOwnerActivity == null) {
            return;
        }

        ((MainActivity) mOwnerActivity).setMenuItemVisibility(R.id.action_search, false);
        ((MainActivity) mOwnerActivity).setMenuItemVisibility(R.id.menu_action_add, false);
        ((MainActivity) mOwnerActivity).setMenuItemVisibility(R.id.menu_action_cancel, false);
        ((MainActivity) mOwnerActivity).setMenuItemVisibility(R.id.menu_sort_by_date, false);
        ((MainActivity) mOwnerActivity).setMenuItemVisibility(R.id.menu_sort_by_name, false);
        ((MainActivity) mOwnerActivity).setMenuItemVisibility(R.id.menu_sort_by_place, false);
        ((MainActivity) mOwnerActivity).setMenuItemVisibility(R.id.menu_grid_1x1, false);
        ((MainActivity) mOwnerActivity).setMenuItemVisibility(R.id.menu_grid_2x1, false);
        ((MainActivity) mOwnerActivity).setMenuItemVisibility(R.id.menu_clear_expired_items, false);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int id = v.getId();

        v.startAnimation(AnimationUtils.loadAnimation(mOwnerActivity, R.anim.anim_press_bounce));

        switch (id) {
            case R.id.btn_start_month:
            case R.id.btn_end_month: {
                mCurPressDateBtnId = id;
                Calendar calendar = Calendar.getInstance();
                mDatePickerDialog = DatePickerDialog.newInstance(this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

                mDatePickerDialog.show(getActivity().getFragmentManager(), DATEPICKER_TAG);
            }
            break;

            case R.id.iv_grocery_list_history_filter: {
                String startMonth = mBtnStartMonth.getText().toString();
                String endMonth = mBtnEndMonth.getText().toString();
                String defaultSetStr = getString(R.string.title_service_time_default);

                mGreceryList.clear();

                if (startMonth.equals(defaultSetStr)) {
                    showToast(getString(R.string.msg_error_start_date_not_set));
                    break;
                }

                if (endMonth.equals(defaultSetStr)) {
                    showToast(getString(R.string.msg_error_end_date_not_set));
                    break;
                }


                if (mEndDate.before(mStartDate)) {
                    showToast(getString(R.string.msg_error_end_date_before_start_date));
                    break;
                }

                ArrayList<GroceryListData> groceryListHistoryList = DBUtility.selectGroceryListHistoryInfosByDates(mStartDate.getTime(), mEndDate.getTime());
                HashMap<String, ItemDetails> itemDetailsMap = new HashMap<String, ItemDetails>();

                for (GroceryListData groceryListData : groceryListHistoryList) {
                    ArrayList<GroceryItem> groceryItems = DBUtility.selectGroceryItemsById(groceryListData.getId());
                    double statisticTotal = 0;

                    for (GroceryItem item : groceryItems) {
                        String qty = item.getQty();
                        String price = item.getPrice();

                        if (qty == null || price == null || qty.isEmpty() || price.isEmpty()) {
                            continue;
                        }

                        statisticTotal += Long.parseLong(item.getQty()) * Double.parseDouble(item.getPrice());
                        ItemDetails itemDetails = null;

                        /* Use [item name][item price] as map key*/
                        if (!itemDetailsMap.containsKey(item.getName() + item.getPrice())) {
                            itemDetails = new ItemDetails();
                            itemDetails.itemName = item.getName();
                            itemDetails.price = Double.parseDouble(item.getPrice());

                            itemDetailsMap.put(item.getName() + item.getPrice(), itemDetails);
                        }
                        itemDetails = itemDetailsMap.get(item.getName() + item.getPrice());
                        itemDetails.qty += Long.parseLong(item.getQty());
                        itemDetails.total += itemDetails.price * Long.parseLong(item.getQty());
                    }
                    groceryListData.setTotalCost(statisticTotal);
                    mGreceryList.add(groceryListData);
                }

                mLvItemDetails.setAdapter(new ItemDetailsAdapter(new ArrayList<ItemDetails>(itemDetailsMap.values())));
                initGroceryListHistoryChart();
                mTvNoChartDataUpper.setVisibility(View.GONE);
                mTvNoChartDataBottom.setVisibility(View.GONE);
            }
            break;
        }
    }

    @Override
    public void onDateSet(DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();

        cal.set(year, monthOfYear, dayOfMonth);

        if (mCurPressDateBtnId >= 0) {
            if (mCurPressDateBtnId == R.id.btn_start_month) {
                /* Set the start date is yyyy-MM-dd 00:00:00 */
                mStartDate = cal;

                mStartDate.set(Calendar.HOUR_OF_DAY, 00);
                mStartDate.set(Calendar.MINUTE, 00);
                mStartDate.set(Calendar.SECOND, 00);
                mBtnStartMonth.setText(Utility.transDateToString(cal.getTime()));
            } else if (mCurPressDateBtnId == R.id.btn_end_month) {
                /* Set the start date is yyyy-MM-dd 23:59:59 */
                mEndDate = cal;

                mEndDate.set(Calendar.HOUR_OF_DAY, 23);
                mEndDate.set(Calendar.MINUTE, 59);
                mEndDate.set(Calendar.SECOND, 59);
                mBtnEndMonth.setText(Utility.transDateToString(cal.getTime()));
            }
            mDatePickerDialog = null;
            mCurPressDateBtnId = -1;
        }
    }

    private class ItemDetailsAdapter extends BaseAdapter {
        private ArrayList<ItemDetails> mItemDetailsList;

        ItemDetailsAdapter(ArrayList<ItemDetails> itemDetailsList) {
            mItemDetailsList = itemDetailsList;
            Collections.sort(mItemDetailsList, new Comparator<ItemDetails>() {
                @Override
                public int compare(ItemDetails lhs, ItemDetails rhs) {
                    if (lhs.total == rhs.total) {
                        return 0;
                    } else if (lhs.total > rhs.total) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });
        }

        @Override
        public int getCount() {
            return mItemDetailsList.size();
        }

        @Override
        public Object getItem(int position) {
            return mItemDetailsList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewGroup view;
            final ItemDetails itemDetails = mItemDetailsList.get(position);

            if (convertView == null) {
                view = (ViewGroup) mInflater.inflate(R.layout.view_grocery_item_details, parent, false);

                changeLayoutConfig(view);
            } else {
                view = (ViewGroup) convertView;
            }
            TextView tvItemName = (TextView) view.findViewById(R.id.tv_item_name);
            TextView tvPrice = (TextView) view.findViewById(R.id.tv_item_price);
            TextView tvQty = (TextView) view.findViewById(R.id.tv_item_qty);
            TextView tvTotal = (TextView) view.findViewById(R.id.tv_item_total);

            tvItemName.setText(itemDetails.itemName);
            tvPrice.setText(Double.toString(itemDetails.price));
            tvQty.setText(Long.toString(itemDetails.qty));
            tvTotal.setText(Long.toString(itemDetails.total));

            return view;
        }
    }

    private class ItemDetails {
        String itemName = null;
        double price = 0;
        long qty = 0;
        long total = 0;
    }
}
