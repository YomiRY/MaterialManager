package com.material.management.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import com.dropbox.client2.DropboxAPI;
import com.material.management.MaterialManagerApplication;
import com.material.management.Observer;
import com.material.management.R;
import com.material.management.data.BackupRestoreInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import static com.dropbox.client2.DropboxAPI.UploadRequest;

public class FileUtility {
    /* /sdcard */
    private static File sSdRoot = Environment.getExternalStorageDirectory();
    private static Context sContext = Utility.getContext();
    public static final String MATERIAL_PHOTO_PATH = sSdRoot.getPath()
            + File.separator + MaterialManagerApplication.PHOTO_DIR_NAME;
    private static final String TEMP_PHOTO_FILE_NAME = "tmp_captured.jpg";
    public static final File TEMP_PHOTO_FILE;

    static {
        File extPhotoDir = new File(MATERIAL_PHOTO_PATH);
        if (!isPicStorageExist())
            extPhotoDir.mkdir();
        TEMP_PHOTO_FILE = new File(MATERIAL_PHOTO_PATH, TEMP_PHOTO_FILE_NAME);
    }

    public static boolean isPicStorageExist() {
        return new File(MATERIAL_PHOTO_PATH).exists();
    }

    public static String saveMaterialPhoto(String photoName, Bitmap photo) {
        photoName = photoName.trim();
        photoName = (photoName == null || photoName.isEmpty()) ? Integer
                .toString((int) (Math.random() * Integer.MAX_VALUE))
                : photoName;
        photoName += ".jpg";
        File filePhoto = new File(MATERIAL_PHOTO_PATH, photoName);
        FileOutputStream out = null;
        photo = (photo == null) ? BitmapFactory.decodeResource(Utility.getContext().getResources(), R.drawable.ic_no_image_available) : photo;

        try {
            out = new FileOutputStream(filePhoto);
            photo.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return filePhoto.getAbsolutePath();
    }

    public static String exportPhoto(DropboxAPI<?> api, Observer observ) {
        StringBuilder msg = new StringBuilder(sContext.getString(R.string.title_photo_backup));

        msg.append(exportPhotoToDropBox(api, observ));

        return msg.toString();
    }

    public static String importPhoto(DropboxAPI<?> api, Observer observ) {
        StringBuilder msg = new StringBuilder(sContext.getString(R.string.title_photo_restore));

        msg.append(importPhotoFromDropBox(api, observ));

        return msg.toString();
    }

    private static String exportPhotoToDropBox(DropboxAPI<?> api, Observer observ) {
        File photoLocalDir = new File(sSdRoot, MaterialManagerApplication.PHOTO_DIR_NAME);
        FileInputStream fis = null;
        UploadRequest request = null;
        BackupRestoreInfo pi = null;

        try {
            if (!photoLocalDir.isDirectory())
                return sContext.getString(R.string.title_dropbox_backup_fail);

            File[] photoFileAry = photoLocalDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().endsWith(".jpg");
                }
            });

            /* upload the photo files */
            String path;
            int total = photoFileAry.length;            
            pi = new BackupRestoreInfo();
            for(int i = 0 ; i < total ; i++) {
                File photoFile = photoFileAry[i];
                path = MaterialManagerApplication.PHOTO_DROPBOX_PATH + photoFile.getName();
                fis = new FileInputStream(photoFile);
                request = api.putFileOverwriteRequest(path, fis, photoFile.length(), null);

                if (request != null) {
                    request.upload();
                }
                fis.close();

                pi.setMsg(sContext.getString(R.string.title_progress_dropbox_backup_photo_successfully, photoFile.getName(), ((i + 1) * 100) / total));
                pi.setProgress(((i + 1) * 100) / total);
                observ.update(pi);
            }
            return sContext.getString(R.string.title_dropbox_backup_successfully);
        } catch (Exception e) {
            e.printStackTrace();

            return sContext.getString(R.string.title_dropbox_backup_fail);
        } finally {
            if (request != null) {
                request.abort();
            }

            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String importPhotoFromDropBox(DropboxAPI<?> api, Observer observ) {
        DropboxAPI.Entry dirEntry = null;
        FileOutputStream fos = null;

        try {
            dirEntry = api.metadata(MaterialManagerApplication.PHOTO_DROPBOX_PATH, 1000, null, true, null);

            if (!dirEntry.isDir || dirEntry.contents == null) {
                // It's not a directory, or there's nothing in it
                return sContext.getString(R.string.title_dropbox_restore_fail);

            }

            /* Retrieve the photo file */
            List<DropboxAPI.Entry> contList = dirEntry.contents;
            int total = contList.size();
            BackupRestoreInfo pi = new BackupRestoreInfo();
            for(int i = 0 ; i < total ; i++) {
                DropboxAPI.Entry ent = contList.get(i);
                String fileName = ent.fileName();

                if (!ent.isDir && fileName.endsWith(".jpg")) {
                    fos = new FileOutputStream(sSdRoot + "/" + MaterialManagerApplication.PHOTO_DIR_NAME + "/" + ent.fileName());

                    api.getFile(ent.path, null, fos, null);
                    fos.close();

                    pi.setMsg(sContext.getString(R.string.title_progress_dropbox_restore_photo_successfully, fileName, ((i + 1) * 100) / total));
                    pi.setProgress(((i + 1) * 100) / total);
                    observ.update(pi);
                }
            }

            return sContext.getString(R.string.title_dropbox_restore_successfully);
        } catch (Exception e) {
            e.printStackTrace();
            return sContext.getString(R.string.title_dropbox_restore_fail);
        } finally {
            if (fos != null)
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
}
