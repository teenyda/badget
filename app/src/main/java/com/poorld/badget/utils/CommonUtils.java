package com.poorld.badget.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class CommonUtils {
    private static final String TAG = "Badget#CommonUtils";

    public static int dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
    public static int px2dip(Context context, float pxValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f); // 四舍五入取整
    }

    public static int sp2px(Context context, float spValue) {
        //fontScale （DisplayMetrics类中属性scaledDensity）
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    public static Drawable resizeDrawable(Context context, Drawable image, int width, int height) {
        Bitmap b = drawableToBitmap(image);
        int dstWidth = CommonUtils.dip2px(context, width);
        int dstHight = CommonUtils.dip2px(context, height);
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, dstWidth, dstHight, false);
        return new BitmapDrawable(context.getResources(), bitmapResized);
    }

    /**
     * 将Drawable转成Bitmap
     * @param drawable
     * @return
     */
    public static Bitmap drawableToBitmap(Drawable drawable) {

        Bitmap bitmap = Bitmap.createBitmap(

                drawable.getIntrinsicWidth(),

                drawable.getIntrinsicHeight(),

                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888

                        : Bitmap.Config.RGB_565);

        Canvas canvas = new Canvas(bitmap);

        // canvas.setBitmap(bitmap);

        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight());

        drawable.draw(canvas);

        return bitmap;
    }


    public static void openAssignFolder(Context context, int requestCode){
        //调用系统文件管理器打开指定路径目录
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/javascript");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        ((Activity)context).startActivityForResult(intent, requestCode);
    }


    public static File saveFileFromUri(Uri uri, Context context, String fileSavePath) {
        Log.d(TAG, "saveFileFromUri: ");
        Log.d(TAG, "uri: " + uri);
        Log.d(TAG, "fileSavePath: " + fileSavePath);
        if (uri == null) {
            return null;
        }
        switch (uri.getScheme()) {
            case "content":
                return getFileFromContentUri(uri, context, fileSavePath);
            case "file":
                return new File(uri.getPath());
            default:
                return null;
        }
    }

    private static File getFileFromContentUri(Uri contentUri, Context context,String fileSavePath) {
        if (contentUri == null) {
            return null;
        }
        File file = null;
        String filePath = null;
        String fileName;
        String[] filePathColumn = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(contentUri, filePathColumn, null,
                null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            try{
                filePath = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
            }catch(Exception e){
            }
            fileName = cursor.getString(cursor.getColumnIndex(filePathColumn[1]));
            cursor.close();
            if (!TextUtils.isEmpty(filePath)) {
                file = new File(filePath);
            }
            if (TextUtils.isEmpty(filePath)) {//!file.exists() || file.length() <= 0 ||
                filePath = getPathFromInputStreamUri(context, contentUri, fileSavePath, fileName);
            }
            if (!TextUtils.isEmpty(filePath)) {
                file = new File(filePath);
            }
        }
        return file;
    }

    private static String getPathFromInputStreamUri(Context context, Uri uri,String fileSavePath, String fileName) {
        InputStream inputStream = null;
        String filePath = null;

        if (uri.getAuthority() != null) {
            try {
                inputStream = context.getContentResolver().openInputStream(uri);
                File file = createTemporalFileFrom(context, inputStream,fileSavePath, fileName);
                filePath = file.getPath();

            } catch (Exception e) {
                Log.e("teenyda", e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (Exception e) {
                    // L.e(e);
                }
            }
        }

        return filePath;
    }

    private static File createTemporalFileFrom(Context context, InputStream inputStream, String path, String fileName)
            throws IOException {
        File targetFile = null;

        if (inputStream != null) {
            int read;
            byte[] buffer = new byte[8 * 1024];
            //自己定义拷贝文件路径
            targetFile = new File(path);

            if (!targetFile.getParentFile().exists()) {
                //boolean mkdir = targetFile.getParentFile().mkdir();
                //Log.d(TAG, "mkdir: " + mkdir);
                ShellUtils.execCommand(new String[]{
                        "mkdir " + targetFile.getParentFile().getPath(),
                        "touch " + targetFile.getPath(),
                        "chmod 777 " + targetFile.getPath()
                }, true, false);
            } else {
                // herself
                //if (targetFile.exists()) {
                //    //return targetFile;
                //    targetFile.delete();
                //}
            }


            try (OutputStream outputStream = Files.newOutputStream(targetFile.toPath())){
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
            }
        }

        return targetFile;
    }

    public static boolean saveFile(String config, String toFile) {
        Log.d(TAG, "saveConfig " + config);
        Log.d(TAG, "saveConfig to " + toFile);
        try (FileOutputStream fileOutput = new FileOutputStream(toFile);
             ByteArrayOutputStream byteOut = new ByteArrayOutputStream();){
            byteOut.write(config.getBytes());
            // 从内存到写入到具体文件
            fileOutput.write(byteOut.toByteArray());
            fileOutput.flush();
            return true;
        } catch (Exception ex) {
            Log.d(TAG, "saveFile Exception " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    public static String readFile(File file) {
        Log.d(TAG, "readFile: ");
        try (FileInputStream fis = new FileInputStream(file);
             FileReader fr = new FileReader(file);
             BufferedReader br = new BufferedReader(fr)){
            String str = null;

            StringBuffer sb = new StringBuffer();
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }

            return sb.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * @param fromFiles 指定的下载目录
     * @param toFile    应用的包路径
     */
    public static void copyFile(String fromFiles, String toFile) {
        //要复制的文件目录
        File[] currentFiles;
        File root = new File(fromFiles);
        //如同判断SD卡是否存在或者文件是否存在,如果不存在则 return出去
        if (!root.exists()) {
            return;
        }
        //如果存在则获取当前目录下的全部文件 填充数组
        currentFiles = root.listFiles();
        if (currentFiles == null) {
            Log.d("soFile---", "未获取到文件");
            return;
        }
        //目标目录
        File targetDir = new File(toFile);
        //创建目录
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        //遍历要复制该目录下的全部文件
        for (int i = 0; i < currentFiles.length; i++) {
            if (currentFiles[i].isDirectory()) {
                //如果当前项为子目录 进行递归
                copyFile(currentFiles[i].getPath() + "/", toFile + currentFiles[i].getName() + "/");
            } else {
                //如果当前项为文件则进行文件拷贝
                int id = doCopy(currentFiles[i].getPath(), toFile + File.separator + currentFiles[i].getName());

            }
        }
    }

    /**
     * 要复制的目录下的所有非子目录(文件夹)文件拷贝
     *
     * @param fromFiles 指定的下载目录
     * @param toFile    应用的包路径
     * @return int
     */
    private static int doCopy(String fromFiles, String toFile) {
        Log.d(TAG, "复制文件到" + toFile);
        try {
            FileInputStream fileInput = new FileInputStream(fromFiles);
            FileOutputStream fileOutput = new FileOutputStream(toFile);
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024 * 1];
            int len = -1;
            while ((len = fileInput.read(buffer)) != -1) {
                byteOut.write(buffer, 0, len);
            }
            // 从内存到写入到具体文件
            fileOutput.write(byteOut.toByteArray());
            // 关闭文件流
            byteOut.close();
            fileOutput.close();
            fileInput.close();
            return 0;
        } catch (Exception ex) {
            return -1;
        }
    }
}