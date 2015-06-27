package com.material.management;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.material.management.data.GroceryItem;
import com.material.management.data.Material;
import com.material.management.data.GlobalSearchData;
import com.material.management.utils.DBUtility;

import java.util.ArrayList;
import java.util.Calendar;

public class GlobalSearchActivity extends MMActivity {
    private RelativeLayout mRlOnLoading;
    private ImageButton mIbHideKeyboard;
    private ListView mLvContentListView;
    private EditText mEtSearchText;

    private GlobalDataLoaderTask mGlobalDataLoaderTask = null;
    private ShortCutSearchListAdapter mSearchResultAdapter = null;
    private String mKeyword = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.global_search_activity);

        findView();
        setListener();
        init();
    }

    @Override
    protected void onStart() {
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
        super.onStart();
    }

    @Override
    protected void onResume() {
        if(mKeyword != null && !mKeyword.isEmpty()) {
            mGlobalDataLoaderTask = new GlobalDataLoaderTask(mKeyword);

            mGlobalDataLoaderTask.execute();
        }
        super.onResume();
    }

    @Override
    protected void onStop() {
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

    private void findView() {
        mRlOnLoading = (RelativeLayout) findViewById(R.id.rl_on_loading);
        mEtSearchText = (EditText) findViewById(R.id.et_search_text);
        mLvContentListView = (ListView) findViewById(R.id.lv_content_list);
        mIbHideKeyboard = (ImageButton) findViewById(R.id.ib_finish_btn);
    }

    private void init() {
        ActionBar actionBar = getActionBar();

        actionBar.setTitle(getString(R.string.app_name));
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void setListener() {
        mEtSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(mGlobalDataLoaderTask != null && !mGlobalDataLoaderTask.isCancelled()) {
                    mGlobalDataLoaderTask.cancel(true);
                    mGlobalDataLoaderTask = null;
                }

                mKeyword = s.toString();

                if(mKeyword.isEmpty()) {
                    mSearchResultAdapter.clear();
                    mSearchResultAdapter.notifyDataSetChanged();
                } else {
                    mGlobalDataLoaderTask = new GlobalDataLoaderTask(mKeyword);

                    mGlobalDataLoaderTask.execute();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        mLvContentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                GlobalSearchData searchData = (GlobalSearchData) mSearchResultAdapter.getItem(pos);
                GlobalSearchData.ItemType type = searchData.getItemType();
                Intent intent = new Intent();

                if(type == GlobalSearchData.ItemType.MATERIAL_ITEM) {
                    intent.setClass(GlobalSearchActivity.this, MaterialModifyActivity.class);
                    intent.putExtra("material_item", searchData.getMaterial());
                } else if (type == GlobalSearchData.ItemType.GROCERY_ITEM) {
                    intent.setClass(GlobalSearchActivity.this, GroceryItemLoginActivity.class);
                    intent.putExtra("grocery_item", searchData.getGroceryItem());
                }
                startActivity(intent);
            }
        });

        mIbHideKeyboard.setOnClickListener(this);
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

     class GlobalDataLoaderTask extends AsyncTask<Void, Void, Void> {
        private String mSearchKeyword;
        private ArrayList<GlobalSearchData> mGlobalSearchDataList;

        private GlobalDataLoaderTask(String keyword) {
            this.mSearchKeyword = keyword.toLowerCase();
            this.mGlobalSearchDataList = new ArrayList<GlobalSearchData>();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mRlOnLoading.setVisibility(View.VISIBLE);
            mLvContentListView.setVisibility(View.GONE);
        }


         private String calRestDay(Material material) {
             /* Calculate the diff days , FIXME: Check the memory and performance... */
             String reset_days_str;
             Calendar c1 = Calendar.getInstance();
             Calendar c2 = Calendar.getInstance();
             Calendar c3 = material.getValidDate();
             c1.set(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH), c1.get(Calendar.DAY_OF_MONTH));
             c2.set(c3.get(Calendar.YEAR), c3.get(Calendar.MONTH), c3.get(Calendar.DAY_OF_MONTH));
             long daysDiff = (c2.getTimeInMillis() - c1.getTimeInMillis()) / (24 * 60 * 60 * 1000);
             reset_days_str = (daysDiff <= 0) ? getString(R.string.msg_expired) :getString(R.string.global_search_rest_days_exp, Long.toString(daysDiff));

             return reset_days_str;
         }

        @Override
        protected Void doInBackground(Void... params) {
            ArrayList<Material> materialInfoList = DBUtility.selectMaterialInfos();
            ArrayList<GroceryItem> groceryItemList = DBUtility.selectGroceryItems();

            mGlobalSearchDataList.add(createHeadItem(mContext.getString(R.string.global_search_material_group)));
            for(Material material : materialInfoList) {
                String name = material.getName();

                if(name != null && name.toLowerCase().contains(mSearchKeyword)) {
                    GlobalSearchData searchData = new GlobalSearchData();
                    String restExpDays = calRestDay(material);

                    searchData.setItemType(GlobalSearchData.ItemType.MATERIAL_ITEM);
                    searchData.setItemName(name);
                    searchData.setItemRestExpDays(restExpDays);
                    searchData.setMaterial(material);
                    mGlobalSearchDataList.add(searchData);
                }
            }

            mGlobalSearchDataList.add(createHeadItem(mContext.getString(R.string.global_search_grocery_group)));
            for(GroceryItem groceryItem : groceryItemList) {
                String name = groceryItem.getName();

                if(name != null && name.toLowerCase().contains(mSearchKeyword)) {
                    GlobalSearchData searchData = new GlobalSearchData();
                    double totalCost = Double.parseDouble(groceryItem.getPrice()) * Integer.parseInt(groceryItem.getQty());

                    searchData.setItemType(GlobalSearchData.ItemType.GROCERY_ITEM);
                    searchData.setItemName(name);
                    searchData.setItemCount(groceryItem.getQty());
                    searchData.setItemTotalCost(getString(R.string.global_search_total_cost, Double.toString(totalCost)));
                    searchData.setGroceryItem(groceryItem);
                    mGlobalSearchDataList.add(searchData);
                }
            }

            return null;
        }

         private GlobalSearchData createHeadItem(String headTitle) {
             GlobalSearchData head = new GlobalSearchData();

             head.setIsHead(true);
             head.setItemName(headTitle);

             return head;
         }

        @Override
        protected void onPostExecute(Void aVoid) {
            mSearchResultAdapter = new ShortCutSearchListAdapter(mContext, mGlobalSearchDataList);

            mLvContentListView.setAdapter(mSearchResultAdapter);
            mRlOnLoading.setVisibility(View.GONE);
            mLvContentListView.setVisibility(View.VISIBLE);
        }
    }

    class ShortCutSearchListAdapter extends BaseAdapter {
        private ArrayList<GlobalSearchData> mList;
        private Context context;

        public ShortCutSearchListAdapter(Context context, ArrayList<GlobalSearchData> globalDataList) {
            this.context = context;
            this.mList = globalDataList;
        }

        public void clear() {
            mList.clear();
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            GlobalSearchData item = mList.get(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(
                        R.layout.shortcut_search_list_item, null);
                holder = new ViewHolder();
                holder.groupLayout = (RelativeLayout) convertView.findViewById(R.id.rl_group_layout);
                holder.contentLayout = (RelativeLayout) convertView.findViewById(R.id.rl_content_layout);
                holder.groupTitle = (TextView) convertView.findViewById(R.id.tv_group_text);

                holder.materialContentLayout = (LinearLayout) convertView.findViewById(R.id.ll_material_content_layout);
                holder.materialItemName = (TextView) convertView.findViewById(R.id.tv_matrial_name);
                holder.materialRestExpDays = (TextView) convertView.findViewById(R.id.tv_matrial_rest_expired_days);

                holder.groceryContentLayout = (LinearLayout) convertView.findViewById(R.id.ll_grocery_content_layout);
                holder.groceryItemName = (TextView) convertView.findViewById(R.id.tv_grocery_item_name);
                holder.groceryItemCount = (TextView) convertView.findViewById(R.id.tv_grocery_item_count);
                holder.groceryItemTotalCost = (TextView) convertView.findViewById(R.id.tv_grocery_item_total_cost);

                holder.divider = convertView.findViewById(R.id.v_divider);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (item.isHead()) {
                holder.groupLayout.setVisibility(View.VISIBLE);
                holder.contentLayout.setVisibility(View.GONE);
                holder.groupTitle.setText(item.getItemName());
            } else {
                GlobalSearchData.ItemType itemType = item.getItemType();
                holder.groupLayout.setVisibility(View.GONE);
                holder.contentLayout.setVisibility(View.VISIBLE);
                holder.materialContentLayout.setVisibility(View.GONE);
                holder.groceryContentLayout.setVisibility(View.GONE);

                switch (itemType) {
                    case MATERIAL_ITEM: {
                        holder.materialContentLayout.setVisibility(View.VISIBLE);

                        holder.materialItemName.setText(item.getItemName());
                        holder.materialRestExpDays.setText(item.getItemRestExpDays());
                    }
                    break;

                    case GROCERY_ITEM: {
                        holder.groceryContentLayout.setVisibility(View.VISIBLE);

                        holder.groceryItemName.setText(item.getItemName());
                        holder.groceryItemCount.setText(item.getItemCount());
                        holder.groceryItemTotalCost.setText(item.getItemTotalCost());
                    }
                    break;
                }
            }

            return convertView;
        }

        public class ViewHolder {
            public RelativeLayout groupLayout;
            public RelativeLayout contentLayout;
            public TextView groupTitle;

            public LinearLayout materialContentLayout;
            public TextView materialItemName;
            public TextView materialRestExpDays;

            public LinearLayout groceryContentLayout;
            public TextView groceryItemName;
            public TextView groceryItemCount;
            public TextView groceryItemTotalCost;

            public View divider;
        }
    }
}
