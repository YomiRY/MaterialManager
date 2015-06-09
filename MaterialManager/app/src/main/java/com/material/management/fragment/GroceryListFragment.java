package com.material.management.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.material.management.GroceryItemLoginActivity;
import com.material.management.GroceryListModifyActivity;
import com.material.management.MMFragment;
import com.material.management.MainActivity;
import com.material.management.Observer;
import com.material.management.R;
import com.material.management.data.GroceryItem;
import com.material.management.data.GroceryListData;
import com.material.management.data.Material;
import com.material.management.dialog.MaterialMenuDialog;
import com.material.management.dialog.ReceiptDialog;
import com.material.management.utils.DBUtility;
import com.picasso.Picasso;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;


public class GroceryListFragment extends MMFragment implements Observer, AdapterView.OnItemClickListener, DialogInterface.OnClickListener {
    private static final int REQ_ADD_GROCERY_ITEMS = 0;
    private static final int REQ_MODIFY_GROCERY_LIST_INFO = 1;
    private static MainActivity sActivity;

    private View mLayout;
    private RelativeLayout mRlEmptyData;
    private ListView mLvGroceryList;

    private ArrayList<GroceryListData> mGroceryListInfos = null;
    private GroceryListAdapter mGroceryListAdapter = null;
    private MaterialMenuDialog mGroceryMenu = null;
    private AlertDialog mGroceryMenuDialog = null;
    private ReceiptDialog mReceiptDialog = null;
    private GroceryListData mCurSelectedGroceryList = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mLayout = inflater.inflate(R.layout.fragment_grocery_list, container, false);
        sActivity = (MainActivity) getActivity();

        findView();
        init();
        changeLayoutConfig(mLayout);

        return mLayout;
    }


    private void findView() {
        mRlEmptyData = (RelativeLayout) mLayout.findViewById(R.id.rl_empty_data);
        mLvGroceryList = (ListView) mLayout.findViewById(R.id.lv_grocery_list);
    }

    private void init() {
        mGroceryListInfos = DBUtility.selectGroceryListInfos();
        mGroceryListAdapter = new GroceryListAdapter(mGroceryListInfos);

        mLvGroceryList.setAdapter(mGroceryListAdapter);
        update(null);
        if (mGroceryListAdapter.getCount() == 0) {
            mLvGroceryList.setVisibility(View.GONE);
            mRlEmptyData.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        ArrayList<String> items = new ArrayList<String>();

        items.add(getString(R.string.title_menu_item_modify));
        items.add(getString(R.string.title_menu_item_del));

        mGroceryMenu = new MaterialMenuDialog(sActivity, getString(R.string.title_menu_head), items.toArray(new String[0]), this);
        mGroceryMenuDialog = mGroceryMenu.show();
    }


    @Override
    public void onResume() {
        sendScreenAnalytics(getString(R.string.ga_app_view_grocery_list_fragment));

        if (mGroceryListAdapter != null) {
            mGroceryListAdapter.reLoadGroceryItems();
        }
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_MODIFY_GROCERY_LIST_INFO) {
            init();
        } else if (requestCode == REQ_ADD_GROCERY_ITEMS) {

        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        if (parent == mLvGroceryList) {

        } else {
            if (mGroceryMenu == null)
                return;

            String item = mGroceryMenu.getItemAtPos(pos);
            boolean isDel = item.equals(getString(R.string.title_menu_item_del));
            boolean isModify = item.equals(getString(R.string.title_menu_item_modify));

            if (isDel) {
                mGroceryListAdapter.delGroceryItem();
                mGroceryListAdapter.reLoadGroceryItems();
            } else if (isModify) {
                GroceryItem groceryItem = mGroceryListAdapter.getFocusedGroceryItem();
                Intent intent = new Intent(mOwnerActivity, GroceryItemLoginActivity.class);

                intent.putExtra("grocery_item", groceryItem);
                startActivity(intent);
            }
        }

        mGroceryMenu.setShowState(false);
        mGroceryMenuDialog.dismiss();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mReceiptDialog != null && mReceiptDialog.isDialogShowing() && which == DialogInterface.BUTTON_POSITIVE) {
            /* TODO: Maybe need to do in background. */
            if (mCurSelectedGroceryList != null) {
                final Calendar nowDate = Calendar.getInstance();

                /*
                * 1.Delete the grocery list.
                *
                * */
                DBUtility.deleteGroceryList(mCurSelectedGroceryList.getId());
                /* 2.Assign the current date as the checkout date. */
                mCurSelectedGroceryList.setCheckOutTime(nowDate.getTime());
                /* 3.Insert the grocery list to grocery list history table. */
                DBUtility.insertGroceryListHistoryInfo(mCurSelectedGroceryList);

                /* 4.Refresh the list*/
                mGroceryListInfos = DBUtility.selectGroceryListInfos();
                mGroceryListAdapter = new GroceryListAdapter(mGroceryListInfos);

                if (mGroceryListAdapter.getCount() == 0) {
                    mLvGroceryList.setVisibility(View.GONE);
                    mRlEmptyData.setVisibility(View.VISIBLE);
                } else {
                    mLvGroceryList.setAdapter(mGroceryListAdapter);
                }

                /* 5. Ask user if the grocery items need to be imported into Expired Monitor. */
                showAlertDialog(getString(R.string.title_notice_dialog), getString(R.string.msg_if_or_not_import_in_expired)
                        , getString(R.string.title_positive_btn_label), getString(R.string.title_negative_btn_label)
                        , new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ArrayList<GroceryItem> groceryItemList = DBUtility.selectGroceryItemsById(mCurSelectedGroceryList.getId());

                        if (groceryItemList != null && groceryItemList.size() == 0) {
                            return;
                        }

                        for (GroceryItem item : groceryItemList) {
                            Material material = new Material();
                            String comment = getString(R.string.title_grocery_comment_content, item.getComment(), item.getPrice(), item.getQty());

                            /* trim the ',' if the first character is ','. */
                            while (comment.indexOf(',') == 0 && comment.length() >= 2) {
                                comment = comment.substring(1);
                            }

                            material.setName(item.getName());
                            material.setMaterialPicPath(item.getGroceryPicPath());
                            material.setBarcode(item.getBarcode());
                            material.setBarcodeFormat(item.getBarcodeFormat());
                            material.setMaterialType(item.getGroceryType());
                            material.setIsAsPhotoType(0);
                            material.setIsValidDateSetup(0);
                            material.setPurchaceDate(nowDate);
                            material.setValidDate(nowDate);
                            material.setNotificationDays(0);
                            material.setMaterialPlace("");
                            material.setComment(comment);
                            DBUtility.insertMaterialInfo(material);
                        }
                    }
                }, null);
            }

            mReceiptDialog = null;
        }
        dialog.dismiss();
    }

    @Override
    public void update(Object data) {
        if (sActivity == null) {
            return;
        }

        sActivity.setMenuItemVisibility(R.id.action_search, false);
        sActivity.setMenuItemVisibility(R.id.menu_action_add, false);
        sActivity.setMenuItemVisibility(R.id.menu_action_cancel, false);
        sActivity.setMenuItemVisibility(R.id.menu_sort_by_date, false);
        sActivity.setMenuItemVisibility(R.id.menu_sort_by_name, false);
        sActivity.setMenuItemVisibility(R.id.menu_sort_by_place, false);
        sActivity.setMenuItemVisibility(R.id.menu_grid_1x1, false);
        sActivity.setMenuItemVisibility(R.id.menu_grid_2x1, false);
        sActivity.setMenuItemVisibility(R.id.menu_clear_expired_items, false);
    }

    private class GroceryListAdapter extends BaseAdapter {
        private RelativeLayout mCurRlGroceryButtomContent = null;
        private ListView mCurLvGroceryItems = null;
        private TextView mCurStaticsTotal = null;

        private int mCurGroceryListId;
        private int mCurGroceryItemPos;
        private ArrayList<GroceryListData> mGroceryList;

        public GroceryListAdapter(ArrayList<GroceryListData> groceryList) {
            mGroceryList = groceryList;
        }

        @Override
        public int getCount() {
            return mGroceryList.size();
        }

        @Override
        public Object getItem(int position) {
            return mGroceryList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewGroup view;
            final GroceryListData groceryListInfo = mGroceryList.get(position);
            final ViewHolder viewHolder;

            if (convertView == null) {
                view = (ViewGroup) mInflater.inflate(R.layout.view_grocery_list_item_layout, parent, false);
                viewHolder = new ViewHolder();

                viewHolder.groceryHeadContent = (RelativeLayout) view.findViewById(R.id.rl_grocery_head_content);
                viewHolder.groceryBottomContent = (RelativeLayout) view.findViewById(R.id.rl_grocery_buttom_content);
                viewHolder.add = (ImageView) view.findViewById(R.id.iv_add);
                viewHolder.checkout = (ImageView) view.findViewById(R.id.iv_checkout);
                viewHolder.storeStatus = (ImageView) view.findViewById(R.id.iv_store_service_status);
                viewHolder.spinMenu = (ImageView) view.findViewById(R.id.iv_spinner_menu);
                viewHolder.groceryName = (TextView) view.findViewById(R.id.tv_grocery_name);
                viewHolder.storeName = (TextView) view.findViewById(R.id.tv_store_name);
                viewHolder.storeAddress = (TextView) view.findViewById(R.id.tv_address);
                viewHolder.staticTotal = (TextView) view.findViewById(R.id.tv_statistic_total);
                viewHolder.groceryItems = (ListView) view.findViewById(R.id.lv_grocery_items);
                viewHolder.verticalBar = view.findViewById(R.id.v_vertical_bar);

                view.setTag(viewHolder);
                changeLayoutConfig(view);
            } else {
                view = (ViewGroup) convertView;
                viewHolder = (ViewHolder) view.getTag();
            }

            /*
            /* Store Status:
            /*     0:close,
            /*     1: open,
            /*    -1: no status
            */
            int storeServiceStatus = checkStoreServiceStatus(groceryListInfo.getServiceTime());

            if (mCurGroceryListId != groceryListInfo.getId()) {
               viewHolder.groceryBottomContent.setVisibility(View.GONE);
            }
            viewHolder.groceryName.setText(groceryListInfo.getGroceryListName());
            viewHolder.storeName.setText(groceryListInfo.getStoreName());
            viewHolder.storeAddress.setText(groceryListInfo.getAddress());

            switch (storeServiceStatus) {
                case 0:
                    viewHolder.storeStatus.setImageResource(R.drawable.ic_store_close);
                    break;
                case 1:
                    viewHolder.storeStatus.setImageResource(R.drawable.ic_store_open);
                    break;
                case -1:
                    viewHolder.storeStatus.setImageResource(R.drawable.ic_no_service_time);
                    break;
            }


            viewHolder.groceryHeadContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCurRlGroceryButtomContent = viewHolder.groceryBottomContent;
                    mCurLvGroceryItems = viewHolder.groceryItems;
                    mCurStaticsTotal = viewHolder.staticTotal;
                    mCurGroceryListId = groceryListInfo.getId();
                    int visibility = mCurRlGroceryButtomContent.getVisibility();

                    if (visibility == View.VISIBLE) {
                        mCurSelectedGroceryList = null;
                        Animation goneAnim = AnimationUtils.loadAnimation(mOwnerActivity, R.anim.push_up_out);

                        goneAnim.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {}

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                mCurRlGroceryButtomContent.setVisibility(View.GONE);

                                mCurRlGroceryButtomContent = null;
                                mCurLvGroceryItems = null;
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {}
                        });
                        unregisterForContextMenu(mCurLvGroceryItems);
                        mCurRlGroceryButtomContent.startAnimation(goneAnim);
                    } else if (visibility == View.GONE) {
                        mCurSelectedGroceryList = groceryListInfo;

                        mCurRlGroceryButtomContent.startAnimation(AnimationUtils.loadAnimation(mOwnerActivity, R.anim.push_up_in));
                        mCurRlGroceryButtomContent.setVisibility(View.VISIBLE);
                        mCurLvGroceryItems.setOnTouchListener(new ListView.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                int action = event.getAction();
                                switch (action) {
                                    case MotionEvent.ACTION_DOWN:
                                        // Disallow ScrollView to intercept touch events.
                                        v.getParent().requestDisallowInterceptTouchEvent(true);
                                        break;

                                    case MotionEvent.ACTION_UP:
                                        // Allow ScrollView to intercept touch events.
                                        v.getParent().requestDisallowInterceptTouchEvent(false);
                                        break;
                                }

                                // Handle ListView touch events.
                                v.onTouchEvent(event);
                                return true;
                            }
                        });
                        mCurLvGroceryItems.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                            @Override
                            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                                mCurGroceryItemPos = pos;

                                return false;
                            }
                        });
                        reLoadGroceryItems();
                        notifyDataSetChanged();
                    }
                    registerForContextMenu(mCurLvGroceryItems);
                }
            });

            viewHolder.add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mOwnerActivity, GroceryItemLoginActivity.class);

                    v.startAnimation(AnimationUtils.loadAnimation(mOwnerActivity, R.anim.anim_press_bounce));
                    intent.putExtra("grocery_list_id", mCurGroceryListId);
                    startActivityForResult(intent, REQ_ADD_GROCERY_ITEMS);
                }
            });

            viewHolder.checkout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.startAnimation(AnimationUtils.loadAnimation(mOwnerActivity, R.anim.anim_press_bounce));

                    GroceryItemAdapter groceryItemAdapter = (GroceryItemAdapter) mCurLvGroceryItems.getAdapter();
                    mReceiptDialog = new ReceiptDialog(sActivity, getString(R.string.title_receipt_dialog_title), groceryListInfo.getGroceryListName(), groceryItemAdapter.getItems(), GroceryListFragment.this, false, null);

                    mReceiptDialog.setShowState(true);
                    mReceiptDialog.show();
                }
            });

            viewHolder.spinMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(sActivity, v);

                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            int id = item.getItemId();

                            switch (id) {
                                case R.id.menu_del: {
                                    DBUtility.deleteGroceryList(groceryListInfo.getId());
                                    DBUtility.deleteGreceryItemsByListId(groceryListInfo.getId());

                                    mGroceryListInfos = DBUtility.selectGroceryListInfos();
                                    mGroceryListAdapter = new GroceryListAdapter(mGroceryListInfos);

                                    if (mGroceryListAdapter.getCount() == 0) {
                                        mLvGroceryList.setVisibility(View.GONE);
                                        mRlEmptyData.setVisibility(View.VISIBLE);
                                    } else {
                                        mLvGroceryList.setAdapter(mGroceryListAdapter);
                                    }
                                }
                                break;

                                case R.id.menu_nav: {
                                    String lat = groceryListInfo.getLat();
                                    String lon = groceryListInfo.getLong();
                                    String address = groceryListInfo.getAddress();

                                    if (lat == null || lon == null || lat.isEmpty() || lon.isEmpty()) {
                                        showToast(getString(R.string.msg_err_store_address));
                                    } else {
                                        String uri = String.format("geo:%s,%s?q=%s,%s(%s)", lat, lon, lat, lon, address);
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));

                                        startActivity(intent);
                                    }
                                }
                                break;

                                case R.id.menu_modify: {
                                    Intent intent = new Intent(mOwnerActivity, GroceryListModifyActivity.class);

                                    intent.putExtra("grocery_list_info", groceryListInfo);
                                    startActivityForResult(intent, REQ_MODIFY_GROCERY_LIST_INFO);
                                }
                                break;

                                case R.id.menu_phone: {
                                    Intent intent = new Intent(Intent.ACTION_DIAL);
                                    String phone = Uri.encode(groceryListInfo.getPhone());

                                    if (phone == null || phone.isEmpty()) {
                                        showToast(getString(R.string.msg_err_store_phone));

                                        break;
                                    }

                                    intent.setData(Uri.parse("tel:" + phone));
                                    startActivity(intent);
                                }
                                break;

                            }
                            return false;
                        }
                    });
                    popup.getMenuInflater().inflate(R.menu.grocery_list_popup_menu, popup.getMenu());
                    popup.show();
                }
            });
            return view;
        }

        class ViewHolder {
            RelativeLayout groceryHeadContent;
            RelativeLayout groceryBottomContent;
            ImageView add;
            ImageView checkout;
            ImageView storeStatus;
            ImageView spinMenu;
            TextView groceryName;
            TextView storeName;
            TextView storeAddress;
            TextView staticTotal;
            ListView groceryItems;
            View verticalBar;
        }

        private int checkStoreServiceStatus(String serviceTimesStr) {
            String[] serviceTimes = serviceTimesStr.split("\\|");
            Calendar today = Calendar.getInstance();
            String todayServiceDayStr = serviceTimes[today.get(Calendar.DAY_OF_WEEK) - 1];

            if (todayServiceDayStr.contains("N/A")) {
                return -1;
            }

            String todayServiceTimeStr = todayServiceDayStr.substring(todayServiceDayStr.indexOf(':') + 1);
            /* nowTimeInteger transformed from [Hour of Day][Minute] string of current time. */
            int nowTimeInteger = today.get(Calendar.HOUR_OF_DAY) * 100 + today.get(Calendar.MINUTE);
            int todayServiceStartInt = Integer.parseInt(todayServiceTimeStr.substring(0, todayServiceTimeStr.indexOf('~')));
            int todayServiceEndInt = Integer.parseInt(todayServiceTimeStr.substring(todayServiceTimeStr.indexOf('~') + 1));

            if ((todayServiceEndInt >= todayServiceStartInt) && nowTimeInteger >= todayServiceStartInt && nowTimeInteger <= todayServiceEndInt) {
                return 1;
            }

            if ((todayServiceEndInt < todayServiceStartInt) && !(nowTimeInteger <= todayServiceStartInt && nowTimeInteger >= todayServiceEndInt)) {
                return 1;
            }

            return 0;
        }

        public void reLoadGroceryItems() {
            if (mCurRlGroceryButtomContent == null || mCurLvGroceryItems == null || mCurStaticsTotal == null) {
                return;
            }

            ArrayList<GroceryItem> groceryItemList = DBUtility.selectGroceryItemsById(mCurGroceryListId);
            GroceryItemAdapter groceryItemAdapter = new GroceryItemAdapter(groceryItemList);
            int count = groceryItemList.size();
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, Math.min(mMetrics.heightPixels * 3 / 4, (int) ((count + 2) * (int) getResources().getDimension(R.dimen.grocery_list_item_height_size) * mMetrics.density)));


            mCurLvGroceryItems.setAdapter(groceryItemAdapter);
            params.addRule(RelativeLayout.BELOW, R.id.rl_grocery_head_content);
            mCurRlGroceryButtomContent.setLayoutParams(params);
            mCurRlGroceryButtomContent.setBackgroundResource(R.color.gray);
            updateStatisticTotal(groceryItemList);
        }

        public void delGroceryItem() {
            GroceryItemAdapter adapter = (GroceryItemAdapter) mCurLvGroceryItems.getAdapter();
            GroceryItem groceryItem = (GroceryItem) adapter.getItem(mCurGroceryItemPos);

            DBUtility.deleteGroceryItem(groceryItem);
        }

        public GroceryItem getFocusedGroceryItem() {
            GroceryItemAdapter adapter = (GroceryItemAdapter) mCurLvGroceryItems.getAdapter();

            return (GroceryItem) adapter.getItem(mCurGroceryItemPos);
        }

        private void updateStatisticTotal(ArrayList<GroceryItem> groceryItemList) {
            double statisticTotal = 0;

            for (GroceryItem item : groceryItemList) {
                String qty = item.getQty();
                String price = item.getPrice();

                if (qty == null || price == null || qty.isEmpty() || price.isEmpty()) {
                    continue;
                }

                statisticTotal += Long.parseLong(item.getQty()) * Double.parseDouble(item.getPrice());
            }
            mCurStaticsTotal.setText(getString(R.string.title_layout_bottom_checkout_total, Double.toString(statisticTotal)));
        }
    }

    private class GroceryItemAdapter extends BaseAdapter {
        private ArrayList<GroceryItem> mGroceryItemList;

        public GroceryItemAdapter(ArrayList<GroceryItem> groceryItemList) {
            mGroceryItemList = groceryItemList;
        }

        @Override
        public int getCount() {
            return mGroceryItemList.size();
        }

        public ArrayList<GroceryItem> getItems() {
            return mGroceryItemList;
        }

        @Override
        public Object getItem(int position) {
            return mGroceryItemList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewGroup view = null;
            GroceryItem groceryItem = mGroceryItemList.get(position);
            ViewHolder viewHolder = null;

            if (convertView == null) {
                view = (ViewGroup) mInflater.inflate(R.layout.view_grocery_item_layout, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.notPurchasedGrayMask = (ImageView) view.findViewById(R.id.iv_not_purchased_gray);
                viewHolder.groceryThumbnail = (ImageView) view.findViewById(R.id.iv_grocery_thumbnail);
                viewHolder.groceryType = (TextView) view.findViewById(R.id.tv_grocery_type);
                viewHolder.groceryName = (TextView) view.findViewById(R.id.tv_grocery_name);
                viewHolder.groceryQty = (TextView) view.findViewById(R.id.tv_grocery_qty);
                viewHolder.price = (TextView) view.findViewById(R.id.tv_grocery_price);

                view.setTag(viewHolder);
                changeLayoutConfig(view);
            } else {
                view = (ViewGroup) convertView;
                viewHolder = (ViewHolder) view.getTag();
            }

            long qty = (groceryItem.getQty() == null || groceryItem.getQty().isEmpty()) ? 0 : Long.parseLong(groceryItem.getQty());
            double price = (groceryItem.getPrice() == null || groceryItem.getPrice().isEmpty()) ? 0 : Double.parseDouble(groceryItem.getPrice());

            viewHolder.notPurchasedGrayMask.setVisibility(View.GONE);
            viewHolder.groceryName.setText(groceryItem.getName());
            viewHolder.groceryType.setText(groceryItem.getGroceryType());
            viewHolder.groceryQty.setText("x " + groceryItem.getQty());
            viewHolder.price.setText("$ " + (qty * price));
            Picasso.with(sActivity).load(new File(groceryItem.getGroceryPicPath())).fit().into(viewHolder.groceryThumbnail);

            return view;
        }

        class ViewHolder {
            ImageView notPurchasedGrayMask;
            ImageView groceryThumbnail;
            TextView groceryType;
            TextView groceryName;
            TextView groceryQty;
            TextView price;
        }
    }
}
