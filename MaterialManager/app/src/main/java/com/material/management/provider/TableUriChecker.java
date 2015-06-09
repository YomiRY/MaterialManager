package com.material.management.provider;

import com.material.management.provider.MaterialProvider.UtilSQLiteOpenHelper;

import android.content.UriMatcher;
import android.net.Uri;

public class TableUriChecker {
    private static final int TB_MATERIAL_OPERATION = 1;
    private static final int TB_MATERIAL_OPERATION_ID = 2;
    private static final int TB_MATERIAL_TYPE_OPERATION = 3;
    private static final int TB_MATERIAL_TYPE_OPERATION_ID = 4;
    private static final int TB_MATERIAL_HISTORY_OPERATION = 5;
    private static final int TB_MATERIAL_HISTORY_OPERATION_ID = 6;
    private static final int TB_GROCERY_LIST = 7;
    private static final int TB_GROCERY_LIST_ID = 8;
    private static final int TB_GROCERY_ITEMS = 9;
    private static final int TB_GROCERY_ITEMS_ID = 10;
    private static final int TB_GROCERY_LIST_HISTORY = 11;
    private static final int TB_GROCERY_LIST_HISTORY_ID = 12;
    private static TableUriChecker sTableUriChecker = null;

    private UriMatcher mUriMatcher = null;

    private TableUriChecker() {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(MaterialProvider.AUTHORITH, UtilSQLiteOpenHelper.TB_MATERIAL, TB_MATERIAL_OPERATION);
        mUriMatcher.addURI(MaterialProvider.AUTHORITH, UtilSQLiteOpenHelper.TB_MATERIAL + "/#", TB_MATERIAL_OPERATION);
        mUriMatcher.addURI(MaterialProvider.AUTHORITH, UtilSQLiteOpenHelper.TB_MATERIAL_TYPE, TB_MATERIAL_TYPE_OPERATION);
        mUriMatcher.addURI(MaterialProvider.AUTHORITH, UtilSQLiteOpenHelper.TB_MATERIAL_TYPE + "/#", TB_MATERIAL_TYPE_OPERATION_ID);
        mUriMatcher.addURI(MaterialProvider.AUTHORITH, UtilSQLiteOpenHelper.TB_MATERIAL_HISTORY, TB_MATERIAL_HISTORY_OPERATION);
        mUriMatcher.addURI(MaterialProvider.AUTHORITH, UtilSQLiteOpenHelper.TB_MATERIAL_HISTORY + "/#", TB_MATERIAL_HISTORY_OPERATION_ID);
        mUriMatcher.addURI(MaterialProvider.AUTHORITH, UtilSQLiteOpenHelper.TB_GROCERY_LIST, TB_GROCERY_LIST);
        mUriMatcher.addURI(MaterialProvider.AUTHORITH, UtilSQLiteOpenHelper.TB_GROCERY_LIST + "/#", TB_GROCERY_LIST_ID);
        mUriMatcher.addURI(MaterialProvider.AUTHORITH, UtilSQLiteOpenHelper.TB_GROCERY_ITEMS, TB_GROCERY_ITEMS);
        mUriMatcher.addURI(MaterialProvider.AUTHORITH, UtilSQLiteOpenHelper.TB_GROCERY_ITEMS + "/#", TB_GROCERY_ITEMS_ID);
        mUriMatcher.addURI(MaterialProvider.AUTHORITH, UtilSQLiteOpenHelper.TB_GROCERY_LIST_HISTORY, TB_GROCERY_LIST_HISTORY);
        mUriMatcher.addURI(MaterialProvider.AUTHORITH, UtilSQLiteOpenHelper.TB_GROCERY_LIST_HISTORY + "/#", TB_GROCERY_LIST_HISTORY_ID);
    }

    public static TableUriChecker getInstance() {
        if (sTableUriChecker == null)
            sTableUriChecker = new TableUriChecker();

        return sTableUriChecker;
    }

    public String retriveTableByUri(Uri uri) {
        int table_id = mUriMatcher.match(uri);
        String table = null;

        switch (table_id) {
            case TB_MATERIAL_OPERATION:
            case TB_MATERIAL_OPERATION_ID:
                table = UtilSQLiteOpenHelper.TB_MATERIAL;
                break;
            case TB_MATERIAL_TYPE_OPERATION:
            case TB_MATERIAL_TYPE_OPERATION_ID:
                table = UtilSQLiteOpenHelper.TB_MATERIAL_TYPE;
                break;
            case TB_MATERIAL_HISTORY_OPERATION:
            case TB_MATERIAL_HISTORY_OPERATION_ID:
                table = UtilSQLiteOpenHelper.TB_MATERIAL_HISTORY;
                break;
            case TB_GROCERY_LIST:
            case TB_GROCERY_LIST_ID:
                table = UtilSQLiteOpenHelper.TB_GROCERY_LIST;
                break;
            case TB_GROCERY_ITEMS:
            case TB_GROCERY_ITEMS_ID:
                table = UtilSQLiteOpenHelper.TB_GROCERY_ITEMS;
                break;
            case TB_GROCERY_LIST_HISTORY:
            case TB_GROCERY_LIST_HISTORY_ID:
                table = UtilSQLiteOpenHelper.TB_GROCERY_LIST_HISTORY;
                break;
        }

        return table;
    }
}
