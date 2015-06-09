package com.material.management.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BitmapUtility {
    private static BitmapUtility sInstance = null;
    private static Context sContext = null;

    private MemoryCache mMemBitmapCache;
    private HashMap<String, BitmapWorkerTask> mUrlTaskMap;
    private ExecutorService mBitmapSdCacheExecutor;

    private BitmapUtility() {
        mMemBitmapCache = new MemoryCache();
        mUrlTaskMap = new HashMap<String, BitmapWorkerTask>();
        /* Use fixed thread 5, because the most device is 4 cores */
        mBitmapSdCacheExecutor = Executors.newFixedThreadPool(5);
    }

    public static void init(Context context) {
        sContext = context;
    }

    public static BitmapUtility getInstance() {
        if (sInstance == null && sContext != null) {
            sInstance = new BitmapUtility();
        }
        return sInstance;
    }

    public void applyBitmapFromUrl(String url, ImageView imgView, int scaledWidth, int scaledHeight, boolean isKeepOrigin, BitmapCallBack bmpCallBack, BitmapFactory.Options customOptions) {
        if (url == null || url.isEmpty()) {
            return;
        }

        Bitmap bmp = mMemBitmapCache.get(url);

        if (bmp != null && imgView != null) {
            imgView.setImageBitmap(bmp);
            if (bmpCallBack != null) {
                bmpCallBack.postDecodeBmpCallBack(imgView);
            }
        } else {
            BitmapWorkerTask task = new BitmapWorkerTask(url, imgView, scaledWidth, scaledHeight, isKeepOrigin, bmpCallBack, customOptions);

            cancelPotentialWork(url);
            mUrlTaskMap.put(url, task);
            if (Build.VERSION.SDK_INT >= 11) {
                task.executeOnExecutor(Executors.newCachedThreadPool());
            } else {
                task.execute();
            }
        }
    }

    public void applyBitmapFromUri(Uri uri, ImageView imgView, int scaledWidth, int scaledHeight, boolean isKeepOrigin, BitmapCallBack bmpCallBack, BitmapFactory.Options customOptions) {
        try {
            URI netURI = new URI(uri.toString());

            applyBitmapFromUrl(netURI.toURL().toString(), imgView, scaledWidth, scaledHeight, isKeepOrigin, bmpCallBack, customOptions);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /* It decode the bitmap retrieved from url and apply it to the imageview
    *
    * url: the imgView want to apply, it's got from server and as a cache key
    * imgView: the ImageView that you want to set up bitmap asynchronously.
    * isKeepOrigin: whether or not scale the width and height to original.
    *
    * */
    public void applyBitmapFromUrl(String url, ImageView imgView, boolean isKeepOrigin) {
        applyBitmapFromUrl(url, imgView, -1, -1, isKeepOrigin, null, null);
    }

    /* It decode the bitmap retrieved from url and apply it to the imageview
    *
    * url: the imgView want to apply, it's got from server and as a cache key
    * imgView: the ImageView that you want to set up bitmap asynchronously.
    *
    * */
    public void applyBitmapFromUrl(String url, ImageView imgView) {
        applyBitmapFromUrl(url, imgView, -1, -1, false, null, null);
    }

    /* It decode the bitmap retrieved from url and apply it to the imageview
    *
    * url: the imgView want to apply, it's got from server and as a cache key
    * imgView: the ImageView that you want to set up bitmap asynchronously.
    * scaledWidth: the bitmap's width that you want to apply. .
    * scaledHeight: the bitmap's height that you want to apply.
    * */
    public void applyBitmapFromUrl(String url, ImageView imgView, int scaledWidth, int scaledHeight) {
        applyBitmapFromUrl(url, imgView, scaledWidth, scaledHeight, false, null, null);
    }

    /* It decode the bitmap retrieved from url and apply it to the imageview
    *
    * url: the imgView want to apply, it's got from server and as a cache key
    * imgView: the ImageView that you want to set up bitmap asynchronously.
    *
    * */
    public void applyBitmapFromUri(Uri uri, ImageView imgView) {
        applyBitmapFromUri(uri, imgView, -1, -1, false, null, null);
    }

    /* It decode the bitmap retrieved from url and apply it to the imageview
    *
    * url: the imgView want to apply, it's got from server and as a cache key
    * imgView: the ImageView that you want to set up bitmap asynchronously.
    * scaledWidth: the bitmap's width that you want to apply. .
    * scaledHeight: the bitmap's height that you want to apply.
    * */
    public void applyBitmapFromUri(Uri uri, ImageView imgView, int scaledWidth, int scaledHeight) {
        applyBitmapFromUri(uri, imgView, scaledWidth, scaledHeight, false, null, null);
    }

    private void cancelPotentialWork(String url) {
        BitmapWorkerTask task = mUrlTaskMap.get(url);

        if (task != null && !task.isCancelled()) {
            task.cancel(true);
            mUrlTaskMap.remove(url);
        }
    }

    public void releaseBitmaps(Bitmap... bitmaps) {
        if (bitmaps != null) {
            for (Bitmap bitmap : bitmaps) {
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
            System.gc();
        }
    }

    public void clearMemCache() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Iterator<BitmapWorkerTask> iteTask = mUrlTaskMap.values().iterator();

                while (iteTask.hasNext()) {
                    BitmapWorkerTask task = iteTask.next();

                    if (task != null && !task.isCancelled()) {
                        task.cancel(true);
                    }
                }

                mMemBitmapCache.clear();
                mUrlTaskMap.clear();
            }
        }).start();
    }

    public void writeBitmapToTarget(final String url, final Bitmap bmp, final File targetFile) {
        mBitmapSdCacheExecutor.execute(new Runnable() {
            @Override
            public void run() {
                FileOutputStream fos = null;
                InputStream in = null;

                try {
                    fos = new FileOutputStream(targetFile);
                        /* Default write bitmap as .jpg */
                    Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;

                    //
                    if (url.endsWith("png")) {
                        compressFormat = Bitmap.CompressFormat.PNG;
                    }
                    bmp.compress(compressFormat, 100, fos);
                    fos.flush();
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                        if (in != null) {
                            in.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /* TODO: Need to be refactor and review, I afraid the quality of bitmap to become more low. */
    public Bitmap readBitmapFromTarget(File cachedFile, BitmapFactory.Options options) {
        return BitmapFactory.decodeFile(cachedFile.getAbsolutePath(), options);
    }

    public static abstract class BitmapCallBack {
        public abstract <T extends ImageView> void postDecodeBmpCallBack(T imgView);
    }

    private class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
        private BitmapFactory.Options mThumbnailOptions;
        private DisplayMetrics mMetrics;
        private WeakReference<ImageView> mImgViewRef = null;
        private String mUrl;
        private int mImgScaledWidth;
        private int mImgScaledHeight;
        private boolean mIsKeepOrigin;
        private BitmapCallBack mBmpCallBack = null;
        private Object mOrigTag = null;

        /* Default settings */ {
            mMetrics = Utility.getDisplayMetrics();
            /* We use the 1/4 width and height as a default scaling factor if user doesn't specify. */
            mImgScaledWidth = mMetrics.widthPixels / 4;
            mImgScaledHeight = mMetrics.heightPixels / 4;
            mThumbnailOptions = new BitmapFactory.Options();
            mThumbnailOptions.inPreferredConfig = Bitmap.Config.RGB_565;
            mThumbnailOptions.inPurgeable = true;
            mThumbnailOptions.inInputShareable = true;
            mThumbnailOptions.inScaled = false;
            mThumbnailOptions.inDensity = mMetrics.densityDpi;
            /* Re-sample the bitmap to 1/4 total pixel count. (1/2 * width) * (1/2 * height) */
            mThumbnailOptions.inSampleSize = 2;
        }

        public BitmapWorkerTask(String url, ImageView imageView, int scaledWidth, int scaledHeight, boolean isKeepOrigin, BitmapCallBack bmpCallBack, BitmapFactory.Options customOptions) {
            mImgViewRef = new WeakReference<ImageView>(imageView);
            mOrigTag = imageView.getTag();
            mUrl = url;
            mImgScaledWidth = (scaledWidth > 0) ? scaledWidth : mImgScaledWidth;
            mImgScaledHeight = (scaledHeight > 0) ? scaledHeight : mImgScaledHeight;
            mIsKeepOrigin = isKeepOrigin;
            mBmpCallBack = bmpCallBack;
            /* If no custom bitmap options, then use the*/
            mThumbnailOptions = (customOptions != null) ? customOptions : mThumbnailOptions;

            imageView.setTag(url);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Integer... params) {
            InputStream in = null;
            Bitmap bmp = null;
            Bitmap tmpBmp = null;

            try {
                bmp = mMemBitmapCache.get(mUrl);

                if (bmp != null) {
                    return bmp;
                } else {
                    mThumbnailOptions.inJustDecodeBounds = true;
                    in = new URL(mUrl).openStream();

                    BitmapFactory.decodeStream(in, null, mThumbnailOptions);
                    /* According to mIsKeepOrig, we decide to change scale width and height to be original or not */
                    mImgScaledWidth = (mIsKeepOrigin) ? mThumbnailOptions.outWidth : mImgScaledWidth;
                    mImgScaledHeight = (mIsKeepOrigin) ? mThumbnailOptions.outHeight : mImgScaledHeight;

                    mThumbnailOptions.inSampleSize = calculateInSampleSize(mThumbnailOptions, mImgScaledWidth, mImgScaledHeight);
                    mThumbnailOptions.inJustDecodeBounds = false;
                    in.close();

                    try {
                        in = new URL(mUrl).openStream();
                        tmpBmp = BitmapFactory.decodeStream(in, null, mThumbnailOptions);
                    } catch (OutOfMemoryError e) {
                        /* Workaround for OOM. */
                        e.printStackTrace();
                        System.gc();
                    }

                    if (tmpBmp != null) {
                        if (!mIsKeepOrigin) {
                            try {
                                bmp = Bitmap.createScaledBitmap(tmpBmp, mImgScaledWidth, mImgScaledHeight, false);
                            } catch (OutOfMemoryError e) {
                                /* Workaround for OOM. */
                                e.printStackTrace();
                                System.gc();
                            }
                        } else {
                            bmp = tmpBmp;
                        }

                        if (tmpBmp != null && tmpBmp != bmp) {
                            releaseBitmaps(tmpBmp);
                        }
                        mMemBitmapCache.put(mUrl, bmp);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            return bmp;
        }

        /*
        /* May be same image url decoded in different activity,
        /* but the previous one decode after than the later one.
        /* So the later task be canceled and ImageView can't be apply
        /* bitmap.
         */
        @Override
        protected void onCancelled() {
            Bitmap bmp = mMemBitmapCache.get(mUrl);

            if (mImgViewRef != null && bmp != null && mUrlTaskMap.containsKey(mUrl)) {
                final ImageView imageView = mImgViewRef.get();

                mUrlTaskMap.remove(mUrl);
                if (imageView != null) {
                    String url = (String) imageView.getTag();

                    if (url != null && url.equals(mUrl)) {
                        imageView.setImageBitmap(bmp);
//                      imageView.setBackgoundColor(mResource.getColor(R.color.transparent));
                        /* Restore original tag */
                        imageView.setTag(mOrigTag);
                        /* Do other things after applying the bitmap to imageview */
                        if (mBmpCallBack != null) {
                            mBmpCallBack.postDecodeBmpCallBack(imageView);
                        }
                    }
                }
            }
            super.onCancelled();
        }

        /* Once complete, see if ImageView is still around and set bitmap. */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (mImgViewRef != null && bitmap != null && mUrlTaskMap.containsKey(mUrl)) {
                final ImageView imageView = mImgViewRef.get();

                mUrlTaskMap.remove(mUrl);
                if (imageView != null) {
                    String url = (String) imageView.getTag();

                    if (url != null && url.equals(mUrl)) {
                        imageView.setImageBitmap(bitmap);
//                        imageView.setBackgroundColor(mResource.getColor(R.color.transparent));
                        /* Restore original tag */
                        imageView.setTag(mOrigTag);
                        /* Do other things after applying the bitmap to imageview */
                        if (mBmpCallBack != null) {
                            mBmpCallBack.postDecodeBmpCallBack(imageView);
                        }
                    }
                }
            }
        }

        private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {

                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) > reqHeight
                        && (halfWidth / inSampleSize) > reqWidth) {
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }
    }
}
