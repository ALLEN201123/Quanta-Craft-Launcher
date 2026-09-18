/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.TargetApi
 *  android.content.ContentUris
 *  android.content.Context
 *  android.database.Cursor
 *  android.net.Uri
 *  android.os.Environment
 *  android.provider.DocumentsContract
 *  android.provider.MediaStore$Audio$Media
 *  android.provider.MediaStore$Files
 *  android.provider.MediaStore$Images$Media
 *  android.provider.MediaStore$Video$Media
 */
package com.qcl.launcher.utils.file;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

public class UriUtils {
    @TargetApi(value=19)
    public static String getRealPathFromUri_AboveApi19(Context context, Uri uri) {
        if (DocumentsContract.isDocumentUri((Context)context, (Uri)uri)) {
            if (UriUtils.isExternalStorageDocument(uri)) {
                String docId = DocumentsContract.getDocumentId((Uri)uri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else {
                if (UriUtils.isDownloadsDocument(uri)) {
                    String id2 = DocumentsContract.getDocumentId((Uri)uri);
                    Uri contentUri = ContentUris.withAppendedId((Uri)Uri.parse((String)"content://downloads/public_downloads"), (long)Long.valueOf(id2));
                    return UriUtils.getDataColumn(context, contentUri, null, null);
                }
                if (UriUtils.isMediaDocument(uri)) {
                    String docId = DocumentsContract.getDocumentId((Uri)uri);
                    String[] split = docId.split(":");
                    String type = split[0];
                    Uri contentUri = "image".equals(type) ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI : ("video".equals(type) ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : ("audio".equals(type) ? MediaStore.Audio.Media.EXTERNAL_CONTENT_URI : MediaStore.Files.getContentUri((String)"external")));
                    String selection = "_id=?";
                    String[] selectionArgs = new String[]{split[1]};
                    return UriUtils.getDataColumn(context, contentUri, "_id=?", selectionArgs);
                }
            }
        } else {
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                return UriUtils.getDataColumn(context, uri, null, null);
            }
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = "_data";
        String[] projection = new String[]{column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(column);
                String string2 = cursor.getString(column_index);
                return string2;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}

