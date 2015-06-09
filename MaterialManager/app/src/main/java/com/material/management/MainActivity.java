package com.material.management;

import java.util.ArrayList;

import com.material.management.component.slidemenu.SlidingActivity;
import com.material.management.component.slidemenu.SlidingMenu;
import com.material.management.data.BundleInfo;
import com.material.management.data.BundleInfo.BundleType;
import com.material.management.data.DeviceInfo;
import com.material.management.dialog.AboutDialog;
import com.material.management.fragment.GroceryHistoryFragment;
import com.material.management.fragment.GroceryListFragment;
import com.material.management.fragment.LoginGroceryListFragment;
import com.material.management.fragment.LoginMaterialFragment;
import com.material.management.fragment.MaterialManagerFragment;
import com.material.management.fragment.SettingsFragment;
import com.material.management.monitor.MonitorService;
import com.material.management.utils.Utility;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

public class MainActivity extends SlidingActivity {
    private static long SPLASH_DISPLAY_LENGTH = 5000;

    private Menu mOptionMenu;
    private ActionBar mActionBar;
    private ListView mLvSlideMenu;
    private SearchView mSvSearchView;

    private Fragment mCurFragment;
    private SlidingMenu mSlideMenu;
    private MenuAdapter mMenuAdapter;
    private Class<? extends Fragment> mCurrFragmentClass = null;
    private String mCurrFragmentTag = null;
    private Class<? extends Fragment> mNewFragmentClass = null;
    private String mNewFragmentTag = null;
    private Bundle mBundle;
    private boolean mResumed;
    /* for detect database restore but need to be refined because of too generic */
    private boolean mIsSettingPressed;
    /* If user press the back key two times in same fragment, then it will leave the app. */
    private boolean mIsFirstBackKeyPress;
    private int mCurHomeUpIconResId;
    private Runnable mSplashScreenRunnable = new Runnable() {
        @Override
        public void run() {
            mMenuAdapter = new MenuAdapter(MainActivity.this);
            mLayout = mInflater.inflate(R.layout.activity_main_layout, null);

            setContentView(mLayout);
            mActionBar.show();
            mActionBar.setDisplayShowTitleEnabled(true);
            mActionBar.setDisplayHomeAsUpEnabled(true);
            changeHomeAsUpIcon(R.drawable.ic_drawer);
            mSlideMenu.setSlidingEnabled(true);
            mLvSlideMenu.setCacheColorHint(0);
            mLvSlideMenu.setAdapter(mMenuAdapter);
            mLvSlideMenu.setOnItemClickListener(mMenuAdapter);
            mSlideMenu.setOnOpenedListener(new SlidingMenu.OnOpenedListener() {
                @Override
                public void onOpened() {
                    mImm.hideSoftInputFromWindow(mLayout.getApplicationWindowToken(), 0);
                }
            });

            /* Check the notification's pending intent, if the notification is clicked */
            Intent intent = getIntent();

            if (intent != null) {
                mBundle = intent.getExtras();

                if (mBundle != null) {
                    int bundleType = mBundle.getInt(BundleInfo.BUNDLE_KEY_BUNDLE_TYPE);
                    if (bundleType == BundleType.BUNDLE_TYPE_NOTIFICATION.value()) {
                        MenuAdapter.MenuItem item = (MenuAdapter.MenuItem) mMenuAdapter.getItem(2);

                        setFragment(item.fragmentClass, item.name);
                        mBundle = null;
                    }
                } else {
                        /* Triggle the first sub item. */
                    MenuAdapter.MenuItem item = (MenuAdapter.MenuItem) mMenuAdapter.getItem(1);

                    Log.d(MaterialManagerApplication.TAG, "setFragment is called...");
                    setFragment(item.fragmentClass, item.name);
                }
            } else {
                        /* Triggle the first sub item. */
                MenuAdapter.MenuItem item = (MenuAdapter.MenuItem) mMenuAdapter.getItem(1);

                Log.d(MaterialManagerApplication.TAG, "setFragment is called...");
                setFragment(item.fragmentClass, item.name);
            }
            mHandler.removeCallbacksAndMessages(null);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get a Tracker (should auto-report)
        ((MaterialManagerApplication) getApplication()).getTracker(MaterialManagerApplication.TrackerName.APP_TRACKER);
        getWindow().setBackgroundDrawable(null);
        Utility.setMainActivity(this);

        mActionBar = getActionBar();
        Intent i = new Intent(this, MonitorService.class);
        mSlideMenu = getSlidingMenu();
        mIsSettingPressed = false;
        mIsFirstBackKeyPress = false;
        mLvSlideMenu = (ListView) getLayoutInflater().inflate(R.layout.sliding_menu, null);

        setBehindContentView(mLvSlideMenu);
        setContentView(R.layout.splash_screen_layout);
        mActionBar.hide();

        mSlideMenu.setShadowWidthRes(R.dimen.shadow_width);
        mSlideMenu.setShadowDrawable(R.drawable.shadow);
        mSlideMenu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        mSlideMenu.setFadeDegree(0.35f);
        mSlideMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        mSlideMenu.setTouchModeBehind(SlidingMenu.TOUCHMODE_FULLSCREEN);
        mSlideMenu.setOnClosedListener(new SlidingMenu.OnClosedListener() {
            public void onClosed() {
                changeFragmentImpl();
            }
        });
        mSlideMenu.setSlidingEnabled(false);
        /*
        /* if immeditly_triggered== false ,
        /* it will delay one ExpireMonitorRunnable checking
         */
        i.putExtra("monitor_type", MonitorService.MonitorType.MONITOR_TYPE_ALL.value());
        i.putExtra("immeditly_triggered", false);
        sendBroadcast(i);

        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(mSplashScreenRunnable, SPLASH_DISPLAY_LENGTH);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        mResumed = true;

        if (mCurrFragmentClass == null && mMenuAdapter != null) {
            setFragment(mMenuAdapter.getDefaultFragment(), mMenuAdapter.getDefaultTag());
        }
        super.onResume();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        /*
        *  FIXME: Too generic. Only setting pressed, to update tab status for database restore detection
        *  TODO: Refresh current fragment status when resume to avid restore database not been updated immediately
        * */
        if (mIsSettingPressed) {
            if (mCurFragment != null && mCurFragment instanceof Observer) {
                if (mCurFragment instanceof MaterialManagerFragment) {
                    mSvSearchView.setOnQueryTextListener((MaterialManagerFragment) mCurFragment);
                    ((Observer) mCurFragment).update(mBundle);
                } else {
                    ((Observer) mCurFragment).update(null);
                }
            }
        }

        mIsSettingPressed = false;
    }

    @Override
    protected void onPause() {
        mResumed = false;
        mIsFirstBackKeyPress = false;
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_bar_menu, menu);

        mOptionMenu = menu;
        /* the search view is disabled by default */
        int searchBtnId = getResources().getIdentifier("android:id/search_button", null, null);
        mSvSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        int searchPlateId = mSvSearchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        EditText searchPlate = (EditText) mSvSearchView.findViewById(searchPlateId);
        ImageView imgSearchBtn = (ImageView) mSvSearchView.findViewById(searchBtnId);

        imgSearchBtn.setImageResource(R.drawable.search_view);
        mSvSearchView.setQueryHint(getString(R.string.title_material_search));
        searchPlate.setTextColor(getResources().getColor(R.color.white));

        setMenuItemVisibility(R.id.action_search, false);
        setMenuItemVisibility(R.id.menu_action_add, false);
        setMenuItemVisibility(R.id.menu_action_cancel, false);
        setMenuItemVisibility(R.id.menu_sort_by_date, false);
        setMenuItemVisibility(R.id.menu_sort_by_name, false);
        setMenuItemVisibility(R.id.menu_sort_by_place, false);
        setMenuItemVisibility(R.id.menu_grid_1x1, false);
        setMenuItemVisibility(R.id.menu_grid_2x1, false);
        setMenuItemVisibility(R.id.menu_clear_expired_items, false);

        return super.onCreateOptionsMenu(menu);
    }

    /* a workaround to detect page 2 back key event */
    @Override
    public void onBackPressed() {
        if (mCurFragment != null
                && (mCurFragment instanceof Observer && mCurFragment instanceof MaterialManagerFragment) && ((MaterialManagerFragment) mCurFragment).isViewMaterialList()) {
            mIsFirstBackKeyPress = false;

            ((Observer) mCurFragment).update(null);
            changeHomeAsUpIcon(R.drawable.ic_drawer);
        } else {
            if (!mIsFirstBackKeyPress) {
                mIsFirstBackKeyPress = true;
                showToast(getString(R.string.slidemenu_msg_second_back_key_quit));
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home: {
                if (mCurFragment != null && mCurFragment instanceof Observer
                        && mCurFragment instanceof MaterialManagerFragment &&
                        mCurHomeUpIconResId == R.drawable.ic_ab_back_holo_dark_am) {
                    ((Observer) mCurFragment).update(null);

                    changeHomeAsUpIcon(R.drawable.ic_drawer);
                } else if (mCurHomeUpIconResId == R.drawable.ic_drawer) {
                    mSlideMenu.toggle();
                }
            }
            break;
            case R.id.menu_grid_1x1: {
                if (mCurFragment != null && mCurFragment instanceof Observer
                        && mCurFragment instanceof MaterialManagerFragment) {
                    ((MaterialManagerFragment) mCurFragment).setMaterialTypeGridColumnNum(1);
                }
            }
            break;
            case R.id.menu_grid_2x1: {
                if (mCurFragment != null && mCurFragment instanceof Observer
                        && mCurFragment instanceof MaterialManagerFragment) {
                    ((MaterialManagerFragment) mCurFragment).setMaterialTypeGridColumnNum(2);
                }
            }
            break;
            case R.id.menu_sort_by_date: {
                if (mCurFragment != null && mCurFragment instanceof Observer
                        && mCurFragment instanceof MaterialManagerFragment) {
                    ((MaterialManagerFragment) mCurFragment).sortMaterial(R.id.menu_sort_by_date);
                }
            }
            break;
            case R.id.menu_sort_by_name: {
                if (mCurFragment != null && mCurFragment instanceof Observer
                        && mCurFragment instanceof MaterialManagerFragment) {
                    ((MaterialManagerFragment) mCurFragment).sortMaterial(R.id.menu_sort_by_name);
                }
            }
            case R.id.menu_sort_by_place: {
                if (mCurFragment != null && mCurFragment instanceof Observer
                        && mCurFragment instanceof MaterialManagerFragment) {
                    ((MaterialManagerFragment) mCurFragment).sortMaterial(R.id.menu_sort_by_place);
                }
            }
            break;
            case R.id.menu_clear_expired_items: {
                if (mCurFragment != null && mCurFragment instanceof Observer
                        && mCurFragment instanceof MaterialManagerFragment) {
                    ((MaterialManagerFragment) mCurFragment).clearExpiredMaterials();
                }
            }
            break;
            case R.id.menu_action_add: {
                if (mCurFragment != null && mCurFragment instanceof Observer) {
                    if (mCurFragment instanceof LoginMaterialFragment) {
                        ((Observer) mCurFragment).update(LoginMaterialFragment.ACTION_BAR_BTN_ACTION_ADD);
                    } else if (mCurFragment instanceof LoginGroceryListFragment) {
                        ((Observer) mCurFragment).update(LoginGroceryListFragment.ACTION_BAR_BTN_ACTION_ADD);
                    }
                }
            }
            break;
            case R.id.menu_action_cancel: {
                if (mCurFragment != null && mCurFragment instanceof Observer
                        && mCurFragment instanceof LoginMaterialFragment) {
                    ((Observer) mCurFragment).update(LoginMaterialFragment.ACTION_BAR_BTN_ACTION_CLEAR);
                } else if (mCurFragment instanceof LoginGroceryListFragment) {
                    ((Observer) mCurFragment).update(LoginGroceryListFragment.ACTION_BAR_BTN_ACTION_CLEAR);
                }
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setMenuItemVisibility(int id, boolean visible) {
        if (mOptionMenu != null) {
            MenuItem item = mOptionMenu.findItem(id);

            if (item != null)
                item.setVisible(visible);
        }
    }

    /* For updating the sliding menu text. */
    public void updateLayoutConfig() {
        mOrigFontSizeMap.clear();
        mMenuAdapter = new MenuAdapter(MainActivity.this);

        mLvSlideMenu.setAdapter(mMenuAdapter);
    }

    public void changeHomeAsUpIcon(int resId) {
        ImageView res = null;
        View decorView = getWindow().getDecorView();
        mCurHomeUpIconResId = resId;


        if (Build.VERSION.SDK_INT < 21) {
            int upId = Resources.getSystem().getIdentifier("up", "id", "android");
            if (upId > 0) {
                res = (ImageView) decorView.findViewById(upId);

                res.setImageResource(resId);
            } else {

                if (android.os.Build.VERSION.SDK_INT <= 10) {
                    ViewGroup acbOverlay = (ViewGroup) ((ViewGroup) decorView).getChildAt(0);
                    ViewGroup abcFrame = (ViewGroup) acbOverlay.getChildAt(0);
                    ViewGroup actionBar = (ViewGroup) abcFrame.getChildAt(0);
                    ViewGroup abLL = (ViewGroup) actionBar.getChildAt(0);
                    ViewGroup abLL2 = (ViewGroup) abLL.getChildAt(1);
                    res = (ImageView) abLL2.getChildAt(0);
                } else if (android.os.Build.VERSION.SDK_INT > 10 && android.os.Build.VERSION.SDK_INT < 16) {
                    ViewGroup acbOverlay = (ViewGroup) ((ViewGroup) decorView).getChildAt(0);
                    ViewGroup abcFrame = (ViewGroup) acbOverlay.getChildAt(0);
                    ViewGroup actionBar = (ViewGroup) abcFrame.getChildAt(0);
                    ViewGroup abLL = (ViewGroup) actionBar.getChildAt(1);
                    res = (ImageView) abLL.getChildAt(0);
                } else {
                    ViewGroup acbOverlay = (ViewGroup) ((ViewGroup) decorView).getChildAt(0);
                    ViewGroup abcFrame = (ViewGroup) acbOverlay.getChildAt(1);
                    ViewGroup actionBar = (ViewGroup) abcFrame.getChildAt(0);
                    ViewGroup abLL = (ViewGroup) actionBar.getChildAt(0);
                    ViewGroup abF = (ViewGroup) abLL.getChildAt(0);
                    res = (ImageView) abF.getChildAt(0);
                }
                res.setImageResource(resId);
            }
        } else {
            mActionBar.setHomeAsUpIndicator(resId);
        }

    }

    public void setFragment(Class<? extends Fragment> fragmentClass, String tag) {
        /* Reset the flag for pressing back key two times to leave app. */
        mIsFirstBackKeyPress = false;

        changeHomeAsUpIcon(R.drawable.ic_drawer);
        if (!mResumed || fragmentClass == null) {
            if (tag.equals(getString(R.string.slidemenu_material_about))) {
                AboutDialog about = new AboutDialog(this);

                about.show();
            } else if (tag.equals(getString(R.string.slidemenu_material_feedback))) {
                Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", getString(R.string.feedback_mail), null));
                DeviceInfo deviceInfo = Utility.getDeviceInfo();

                i.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.feedback_mail)});
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
                i.putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_content, deviceInfo.getPlatformVersion(), deviceInfo.getAppVersion(), deviceInfo.getDevice()));

                startActivity(Intent.createChooser(i, getString(R.string.feedback_subject)));
            } else if(tag.equals(getString(R.string.slidemenu_material_global_search))) {
                Intent intent = new Intent(this, GlobalSearchActivity.class);

                startActivity(intent);
            }

            return;
        }

        if (mCurrFragmentClass != fragmentClass) {
            mNewFragmentClass = fragmentClass;
            mNewFragmentTag = tag;

            if (isMenuShowing()) {
                setEmptyFragment();
            } else {
                changeFragmentImpl();
            }
        }

        if (isMenuShowing()) {
            mSlideMenu.toggle(true);
        }
    }

    private void setEmptyFragment() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment f = new Fragment();

        mSlideMenu.setOnScrollListener(null);
        ft.replace(R.id.fragment_container, f);
        ft.setTransition(FragmentTransaction.TRANSIT_NONE);
        ft.commit();
        getFragmentManager().executePendingTransactions();
    }

    private void changeFragmentImpl() {
        if (mCurrFragmentClass == mNewFragmentClass) {
            return;
        }

        mCurrFragmentClass = mNewFragmentClass;
        mCurrFragmentTag = mNewFragmentTag;

        mActionBar.setTitle(mMenuAdapter.getCategoryTitle(mCurrFragmentTag) + " - " + mCurrFragmentTag);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        mActionBar.removeAllTabs();

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        try {
            mCurFragment = mCurrFragmentClass.newInstance();

            if (mCurFragment != null && mCurFragment instanceof SlidingMenu.OnScrollListener) {
                mSlideMenu.setOnScrollListener((SlidingMenu.OnScrollListener) mCurFragment);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ft.replace(R.id.fragment_container, mCurFragment, mCurrFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();

        if (mCurFragment != null && mCurFragment instanceof Observer) {
            if (mCurFragment instanceof MaterialManagerFragment) {
                mSvSearchView.setOnQueryTextListener((MaterialManagerFragment) mCurFragment);
                ((Observer) mCurFragment).update(mBundle);
            } else {
                ((Observer) mCurFragment).update(null);
            }
        }
    }

    private class MenuAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
        private ArrayList<MenuItem> mMenus = new ArrayList<MenuItem>();
        private MainActivity mActivity;

        private class MenuItem {
            boolean isSeperator;
            String name;
            int resourceId;
            Class<? extends Fragment> fragmentClass;

            MenuItem(String n, int r, Class<? extends Fragment> clz) {
                name = n;
                resourceId = r;
                fragmentClass = clz;
                isSeperator = false;
            }

            MenuItem(String n) {
                name = n;
                isSeperator = true;
            }
        }

        /* must ensure the order */
        public MenuAdapter(MainActivity activity) {
            mActivity = activity;

            mMenus.add(new MenuItem(getString(R.string.slidemenu_material_management_title)));
            mMenus.add(new MenuItem(getString(R.string.slidemenu_material_login_title), R.drawable.ic_login_material, LoginMaterialFragment.class));
            mMenus.add(new MenuItem(getString(R.string.slidemenu_material_view_title), R.drawable.ic_menu_find, MaterialManagerFragment.class));

            mMenus.add(new MenuItem(getString(R.string.slidemenu_grocery_title)));
            mMenus.add(new MenuItem(getString(R.string.slidemenu_gcrocery_login_list_title), R.drawable.ic_login_grocery_list, LoginGroceryListFragment.class));
            mMenus.add(new MenuItem(getString(R.string.slidemenu_gcrocery_list_view_title), R.drawable.ic_view_grocery_list, GroceryListFragment.class));
            mMenus.add(new MenuItem(getString(R.string.slidemenu_grocery_history), R.drawable.ic_grocery_history, GroceryHistoryFragment.class));

            mMenus.add(new MenuItem(getString(R.string.slidemenu_material_other)));
            mMenus.add(new MenuItem(getString(R.string.slidemenu_material_global_search), R.drawable.ic_search, null));
            mMenus.add(new MenuItem(getString(R.string.slidemenu_material_settings), R.drawable.ic_setting, SettingsFragment.class));
            mMenus.add(new MenuItem(getString(R.string.slidemenu_material_feedback), R.drawable.ic_spanner, null));
            mMenus.add(new MenuItem(getString(R.string.slidemenu_material_about), R.drawable.ic_about, null));
        }

        @Override
        public int getCount() {
            return mMenus.size();
        }

        @Override
        public Object getItem(int position) {
            return mMenus.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            int type = mMenus.get(position).isSeperator ? 0 : 1;
            return type;
        }

        @Override
        public boolean isEnabled(int position) {
            return !mMenus.get(position).isSeperator;
        }

        private final class ViewHolder {
            ImageView img;
            TextView text;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MenuItem item = mMenus.get(position);
            ViewHolder holder = null;

            if (convertView == null) {
                holder = new ViewHolder();

                if (item.isSeperator) {
                    convertView = mInflater.inflate(R.layout.menu_title_item, null);
                    holder.text = (TextView) convertView.findViewById(R.id.txv_menu_title_item);
                } else {
                    convertView = mInflater.inflate(R.layout.sub_menu_item, null);
                    holder.text = (TextView) convertView.findViewById(R.id.tv_menu_sub_item);
                    holder.img = (ImageView) convertView.findViewById(R.id.iv_menu_sub_item_img);
                }

                changeLayoutConfig((ViewGroup) convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.text.setText(item.name);

            if (!item.isSeperator) {
                holder.img.setImageResource(item.resourceId);
            }

            return convertView;
        }

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            MenuItem item = mMenus.get(position);

            if (item.isSeperator) {
                return;
            }

            mActivity.setFragment(item.fragmentClass, item.name);
        }

        public String getCategoryTitle(String tag) {
            String title = null;

            for (MenuItem item : mMenus) {
                if (item.isSeperator) {
                    title = item.name;
                } else if (item.name.equals(tag)) {
                    break;
                }
            }
            return title;
        }

        public Class<? extends Fragment> getDefaultFragment() {
            for (MenuItem item : mMenus) {
                if (!item.isSeperator) {
                    return item.fragmentClass;
                }
            }

            return null;
        }

        public String getDefaultTag() {
            for (MenuItem item : mMenus) {
                if (!item.isSeperator) {
                    return item.name;
                }
            }
            return null;
        }
    }

}
