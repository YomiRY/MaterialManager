package com.material.management.provider;

import com.material.management.MaterialManagerApplication;
import com.material.management.utils.Utility;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.net.Uri;
import android.util.Log;

public class MaterialProvider extends ContentProvider {
    //    public static final String DB_UPGRADE_FLAG_1to2 = "db_upgrade_flag";
    public static final String AUTHORITH = "com.materialmgr.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITH);
    public static final Uri URI_MATERIAL = Uri.withAppendedPath(MaterialProvider.CONTENT_URI,
            UtilSQLiteOpenHelper.TB_MATERIAL);
    public static final Uri URI_MATERIAL_TYPE = Uri.withAppendedPath(MaterialProvider.CONTENT_URI,
            UtilSQLiteOpenHelper.TB_MATERIAL_TYPE);
    public static final Uri URI_MATERIAL_HISTORY = Uri.withAppendedPath(MaterialProvider.CONTENT_URI,
            UtilSQLiteOpenHelper.TB_MATERIAL_HISTORY);
    public static final Uri URI_GROCERY_LIST = Uri.withAppendedPath(MaterialProvider.CONTENT_URI,
            UtilSQLiteOpenHelper.TB_GROCERY_LIST);
    public static final Uri URI_GROCERY_ITEMS = Uri.withAppendedPath(MaterialProvider.CONTENT_URI,
            UtilSQLiteOpenHelper.TB_GROCERY_ITEMS);
    public static final Uri URI_GROCERY_LIST_HISTORY = Uri.withAppendedPath(MaterialProvider.CONTENT_URI,
            UtilSQLiteOpenHelper.TB_GROCERY_LIST_HISTORY);
    public static final String SINGLE_RECORD_MIME_TYPE = "vnd.android.cursor.item/vnd.material.status";
    public static final String MULTIPLE_RECORD_MIME_TYPE = "v.android.cursor.dir/vnd.material.mstatus";
    public static final String DB_NAME = "Material.db";

    private UtilSQLiteOpenHelper mDbHelper = null;
    private TableUriChecker mTableUriChecker;

    @Override
    public String getType(Uri uri) {
        return this.getId(uri) < 0 ? MULTIPLE_RECORD_MIME_TYPE : SINGLE_RECORD_MIME_TYPE;
    }

    private long getId(Uri uri) {
        String lastPathSegment = uri.getLastPathSegment();

        if (lastPathSegment != null) {
            try {
                return Long.parseLong(lastPathSegment);
            } catch (NumberFormatException e) {
            }
        }

        return -1;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int updatedColumns = 0;

        db.beginTransaction();
        try {
            for (ContentValues value : values) {
                if (insertInternal(uri, value, db) != null)
                    updatedColumns++;
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.w(MaterialManagerApplication.TAG, e.getMessage(), e);
        } finally {
            db.endTransaction();
        }
        if (updatedColumns > 0)
            getContext().getContentResolver().notifyChange(uri, null);
        return values.length;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri newUri = insertInternal(uri, values, null);

        if (newUri != null)
            getContext().getContentResolver().notifyChange(newUri, null);

        return newUri;
    }

    /* the last segment of uri must be a specific table */
    private Uri insertInternal(Uri uri, ContentValues values, SQLiteDatabase db) {
        long rowId;
        Uri newUri = null;
        String table = uri.getLastPathSegment();

        if (db == null)
            db = mDbHelper.getWritableDatabase();

        try {
            rowId = db.insertOrThrow(table, null, values);

            if (rowId > 0) {
                newUri = ContentUris.withAppendedId(uri, rowId);

                return newUri;
            }
        } catch (SQLException e) {
            Log.e(MaterialManagerApplication.TAG, e.getMessage(), e);

        }
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String table = mTableUriChecker.retriveTableByUri(uri);
        int rowAffected = db.delete(table, selection, selectionArgs);

        if (rowAffected > 0)
            getContext().getContentResolver().notifyChange(uri, null);
        return rowAffected;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String table = mTableUriChecker.retriveTableByUri(uri);
        Cursor c = null;

        c = db.query(table, projection, selection, selectionArgs, null, null, sortOrder);

        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String table = mTableUriChecker.retriveTableByUri(uri);
        int rowAffected = db.update(table, values, selection, selectionArgs);
        // if (rowAffected > 0) {
        // Cursor c = query(uri, null, selection, selectionArgs, null);
        // ArrayList<Integer> rowIdList = new ArrayList<Integer>();
        //
        // while (c.moveToNext())
        // rowIdList.add(c.getInt(0));
        // c.close();
        //
        // for (Integer rowId : rowIdList) {
        // Uri newUri = ContentUris.withAppendedId(uri, rowId.intValue());
        // getContext().getContentResolver().notifyChange(newUri, null);
        // }
        // }

        return rowAffected;
    }

    @Override
    public boolean onCreate() {
        mTableUriChecker = TableUriChecker.getInstance();
        mDbHelper = new UtilSQLiteOpenHelper(DB_NAME, getContext());
        return true;
    }

    public static class UtilSQLiteOpenHelper extends SQLiteOpenHelper {
        private static final int VERSION = 3;
        public static final String TB_MATERIAL = "material";
        public static final String TB_MATERIAL_TYPE = "material_type";
        public static final String TB_MATERIAL_HISTORY = "material_history";
        public static final String TB_GROCERY_LIST = "grocery_list";
        public static final String TB_GROCERY_ITEMS = "grocery_items";
        public static final String TB_GROCERY_LIST_HISTORY = "grocery_list_history";

        public UtilSQLiteOpenHelper(String name, Context context) {
            this(name, context, null, VERSION);
        }

        public UtilSQLiteOpenHelper(String name, Context context, CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            /*
            *  Material informations
            *
            *  is_valid_date_setup:
            *  defult is setup(1), otherwise is not-setup(0)
            * */
            final String TB_MATERIAL_SQL_STMT = "create table " + TB_MATERIAL
                    + "( id INTEGER PRIMARY KEY,"
                    + " name TEXT,"
                    + " material_type TEXT,"
                    + " is_as_photo_type INTEGER,"
                    + " material_place TEXT,"
                    + " purchase_date DATE,"
                    + " valid_date DATE,"
                    + " notification_days INTEGER,"
                    + " photo_path TEXT,"
                    + " comment TEXT,"
                    + " barcode TEXT,"
                    + " barcode_format TEXT,"
                    + " is_valid_date_setup INTEGER DEFAULT 1)";
            /* Material type informations */
            final String TB_MATERIAL_TYPE_SQL_STMT = "create table " + TB_MATERIAL_TYPE
                    + "( id INTEGER PRIMARY KEY,"
                    + " name TEXT)";

            /* Material history informations */
            final String TB_MATERIAL_HISTORY_SQL_STMT = "create table " + TB_MATERIAL_HISTORY
                    + "( id INTEGER PRIMARY KEY,"
                    + " name TEXT,"
                    + " barcode TEXT,"
                    + " barcode_format TEXT,"
                    + " material_type TEXT,"
                    + " material_place TEXT,"
                    + " photo_path TEXT,"
                    + " comment TEXT)";

            /*
            * Grocery list informations.
            * The id use the AUTOINCREMENT to keep the increment id in seq table, so that it will
            * be not always started from 0.
            * */
            final String TB_GROCERY_LIST_SQL_STMT = "create table " + TB_GROCERY_LIST
                    + "( id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " grocery_list_name TEXT,"
                    + " alert_nearby INTEGER DEFAULT 0,"
                    + " store_name TEXT,"
                    + " address TEXT,"
                    + " latitude TEXT,"
                    + " longitude TEXT,"
                    + " phone TEXT,"
                    + " service_time TEXT)";

            /* Grocery list informations */
            final String TB_GROCERY_ITEMS_SQL_STMT = "create table " + TB_GROCERY_ITEMS
                    + "( id INTEGER PRIMARY KEY,"
                    + " name TEXT,"
                    + " grocery_type TEXT,"
                    + " grocery_list_id INTEGER,"
                    + " photo_path TEXT,"
                    + " barcode TEXT,"
                    + " barcode_format TEXT,"
                    + " purchase_date DATE,"
                    + " valid_date DATE,"
                    + " size TEXT,"
                    + " size_unit TEXT,"
                    + " qty TEXT,"
                    + " price TEXT,"
                    + " comment TEXT)";

            /* Grocery list history informations */
            final String TB_GROCERY_LIST_HISTORY_SQL_STMT = "create table " + TB_GROCERY_LIST_HISTORY
                    + "( id INTEGER PRIMARY KEY,"
                    + " grocery_list_id INTEGER,"
                    + " grocery_list_name TEXT,"
                    + " alert_nearby INTEGER DEFAULT 0,"
                    + " store_name TEXT,"
                    + " address TEXT,"
                    + " phone TEXT,"
                    + " service_time TEXT,"
                    + " check_out_time DATETIME)";

            db.execSQL(TB_MATERIAL_SQL_STMT);
            db.execSQL(TB_MATERIAL_TYPE_SQL_STMT);
            db.execSQL(TB_MATERIAL_HISTORY_SQL_STMT);
            db.execSQL(TB_GROCERY_LIST_SQL_STMT);
            db.execSQL(TB_GROCERY_ITEMS_SQL_STMT);
            db.execSQL(TB_GROCERY_LIST_HISTORY_SQL_STMT);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (newVersion > oldVersion) {
                db.beginTransaction();
                boolean isSuccessful = false;

                /* FIXME: 
                 * the upgrade only handle neighboring old and new version. e.g.: 1 -> 2.
                 * we need to handle the complex upgrade in the future. e.g.: 1 -> 3.
                 * */
                switch (oldVersion) {
                    case 1: {
                        db.execSQL("ALTER TABLE " + TB_MATERIAL + " ADD COLUMN barcode TEXT NOT NULL DEFAULT ''");
                        db.execSQL("ALTER TABLE " + TB_MATERIAL + " ADD COLUMN barcode_format TEXT NOT NULL DEFAULT ''");
                        db.execSQL("create table " + TB_MATERIAL_HISTORY
                                + "( id INTEGER PRIMARY KEY,"
                                + " name TEXT,"
                                + " barcode TEXT,"
                                + " barcode_format TEXT,"
                                + " material_type TEXT,"
                                + " material_place TEXT,"
                                + " photo_path TEXT,"
                                + " comment TEXT)");
                        oldVersion++;
                        isSuccessful = true;
                        Utility.setIntValueForKey(Utility.DB_UPGRADE_FLAG_1to2, 1);
                    }
                    case 2: {
                        db.execSQL("DROP TABLE IF EXISTS " + TB_GROCERY_LIST);
                        db.execSQL("DROP TABLE IF EXISTS " + TB_GROCERY_ITEMS);
                        db.execSQL("DROP TABLE IF EXISTS " + TB_GROCERY_LIST_HISTORY);
                        db.execSQL("ALTER TABLE " + TB_MATERIAL + " ADD COLUMN is_valid_date_setup INTEGER DEFAULT 1");
                        db.execSQL("create table " + TB_GROCERY_LIST
                                + "( id INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + " grocery_list_name TEXT,"
                                + " alert_nearby INTEGER DEFAULT 0,"
                                + " store_name TEXT,"
                                + " address TEXT,"
                                + " latitude TEXT,"
                                + " longitude TEXT,"
                                + " phone TEXT,"
                                + " service_time TEXT)");

                        db.execSQL("create table " + TB_GROCERY_ITEMS
                                + "( id INTEGER PRIMARY KEY,"
                                + " name TEXT,"
                                + " grocery_type TEXT,"
                                + " grocery_list_id INTEGER,"
                                + " photo_path TEXT,"
                                + " barcode TEXT,"
                                + " barcode_format TEXT,"
                                + " purchase_date DATE,"
                                + " valid_date DATE,"
                                + " size TEXT,"
                                + " size_unit TEXT,"
                                + " qty TEXT,"
                                + " price TEXT,"
                                + " comment TEXT)");

                        db.execSQL("create table " + TB_GROCERY_LIST_HISTORY
                                + "( id INTEGER PRIMARY KEY,"
                                + " grocery_list_id INTEGER,"
                                + " grocery_list_name TEXT,"
                                + " alert_nearby INTEGER DEFAULT 0,"
                                + " store_name TEXT,"
                                + " address TEXT,"
                                + " phone TEXT,"
                                + " service_time TEXT,"
                                + " check_out_time DATETIME)");

                        oldVersion++;
                        isSuccessful = true;
                        Utility.setIntValueForKey(Utility.DB_UPGRADE_FLAG_2to3, 1);
                    }
                }

                if (isSuccessful) {
                    db.setTransactionSuccessful();
                }
                db.endTransaction();
            } else {
                db.execSQL("DROP TABLE IF EXISTS " + TB_MATERIAL);
                db.execSQL("DROP TABLE IF EXISTS " + TB_MATERIAL_TYPE);
                db.execSQL("DROP TABLE IF EXISTS " + TB_MATERIAL_HISTORY);
                db.execSQL("DROP TABLE IF EXISTS " + TB_GROCERY_LIST);
                db.execSQL("DROP TABLE IF EXISTS " + TB_GROCERY_ITEMS);
                db.execSQL("DROP TABLE IF EXISTS " + TB_GROCERY_LIST_HISTORY);
                onCreate(db);
            }
        }
    }
}
