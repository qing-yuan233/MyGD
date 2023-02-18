package com.example.camerajava;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.media.Image;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

public class MyTool {
    //YUC图片转Bitmap格式
    //参考https://blog.csdn.net/qq_40243750/article/details/115740646
    public static Bitmap toBitmap(Image image) throws IOException {

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
        return bitmapImage;
    }

//    //将bitmap存储到本地
//    public int saveImageToGallery(Bitmap bmp) {
//        //生成路径
//        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
//        String dirName = "erweima16";
//        File appDir = new File(root , dirName);
//        if (!appDir.exists()) {
//            appDir.mkdirs();
//        }
//
//        //文件名为时间
//        long timeStamp = System.currentTimeMillis();
//        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        String sd = sdf.format(new Date(timeStamp));
//        String fileName = sd + ".jpg";
//
//        //获取文件
//        File file = new File(appDir, fileName);
//        FileOutputStream fos = null;
//        try {
//            fos = new FileOutputStream(file);
//            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
//            fos.flush();
//            //通知系统相册刷新
//            ImageActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
//                    Uri.fromFile(new File(file.getPath()))));
//            return 2;
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (fos != null) {
//                    fos.close();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        return -1;
//    }
public static void saveBitmap(String name, Bitmap bm, Context mContext) {

    Log.d("Save Bitmap", "Ready to save picture");

    //指定我们想要存储文件的地址

    String TargetPath = mContext.getFilesDir() + "/images/";

    Log.d("Save Bitmap", "Save Path=" + TargetPath);

    //判断指定文件夹的路径是否存在

    if (!MyTool.fileIsExist(TargetPath)) {

        Log.d("Save Bitmap", "TargetPath isn't exist");

    } else {

        //如果指定文件夹创建成功，那么我们则需要进行图片存储操作

        File saveFile = new File(TargetPath, name);

        try {

            FileOutputStream saveImgOut = new FileOutputStream(saveFile);

            // compress - 压缩的意思

            bm.compress(Bitmap.CompressFormat.JPEG, 80, saveImgOut);

            //存储完成后需要清除相关的进程

            saveImgOut.flush();

            saveImgOut.close();

            Log.d("Save Bitmap", "The picture is save to your phone!");

        } catch (IOException ex) {

            ex.printStackTrace();

        }

    }

}
    static boolean fileIsExist(String fileName)

    {

        //传入指定的路径，然后判断路径是否存在

        File file=new File(fileName);

        if (file.exists())

            return true;

        else{

            //file.mkdirs() 创建文件夹的意思

            return file.mkdirs();

        }

    }


}
