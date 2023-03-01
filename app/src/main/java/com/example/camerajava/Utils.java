package com.example.camerajava;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;



public  class Utils {

    //YUV转JPEG
    static public byte[] YuvToJpeg(Image image) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Rect rect = image.getCropRect();
        int width = rect.width();
        int height = rect.height();
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        byte[] yuvBytes = new byte[ySize + uSize + vSize];
        yBuffer.get(yuvBytes, 0, ySize);
        uBuffer.get(yuvBytes, ySize, uSize);
        vBuffer.get(yuvBytes, ySize + uSize, vSize);

        YuvImage yuvImage = new YuvImage(yuvBytes, ImageFormat.NV21, width, height, null);
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        return out.toByteArray();
    }

    //RGB_8888图像转JPEG
    static public byte[] RGB8888ToJpeg(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        YuvImage yuvImage = new YuvImage(bytes, ImageFormat.FLEX_RGBA_8888 ,image.getWidth(), image.getHeight(), null);
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, outputStream);

        return outputStream.toByteArray();

    }

    static public byte[] Yuv_420_888_ToJpeg(Image image) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Rect rect = image.getCropRect();
        int width = rect.width();
        int height = rect.height();
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        // Convert YUV_420_888 to NV21 format
        byte[] nv21 = new byte[width * height * 3 / 2];
        int stride = planes[1].getRowStride();
        int pixelStride = planes[1].getPixelStride();
        byte[] u = new byte[width / 2];
        byte[] v = new byte[width / 2];
        int pos = 0;
        if (pixelStride == 1 && stride == width / 2) {
            uBuffer.get(u);
            vBuffer.get(v);
            for (int i = 0; i < width / 2; i++) {
                nv21[pos++] = v[i];
                nv21[pos++] = u[i];
            }
        } else {
            int yPos = 0;
            for (int i = 0; i < height; i++) {
                yBuffer.position(yPos);
                int yRowStride = planes[0].getRowStride();
                if (yRowStride == width) {
                    yBuffer.get(nv21, pos, width);
                    pos += width;
                } else {
                    for (int j = 0; j < width; j++) {
                        nv21[pos++] = yBuffer.get();
                    }
                }
                if (i % 2 == 0 && i < height / 2) {
                    int upos = planes[2].getBuffer().remaining() / stride * (i / 2);
                    int vpos = planes[1].getBuffer().remaining() / stride * (i / 2);
                    planes[2].getBuffer().position(upos);
                    planes[1].getBuffer().position(vpos);
                    planes[2].getBuffer().get(u, 0, width / 2);
                    planes[1].getBuffer().get(v, 0, width / 2);
                    if (pixelStride == 2 && stride == width) {
                        for (int j = 0; j < width / 2; j++) {
                            nv21[pos++] = v[j];
                            nv21[pos++] = u[j];
                        }
                    } else {
                        for (int j = 0; j < width / 2; j++) {
                            nv21[pos++] = u[j];
                            nv21[pos++] = v[j];
                        }
                    }
                }
                yPos += yRowStride;
            }
        }

        // Convert NV21 to JPEG
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        return out.toByteArray();
    }



    //Planar格式（P）的处理
    private static ByteBuffer getuvBufferWithoutPaddingP(ByteBuffer uBuffer,ByteBuffer vBuffer, int width, int height, int rowStride, int pixelStride){
        int pos = 0;
        byte []byteArray = new byte[height*width/2];
        for (int row=0; row<height/2; row++) {
            for (int col=0; col<width/2; col++) {
                int vuPos = col*pixelStride + row*rowStride;
                byteArray[pos++] = vBuffer.get(vuPos);
                byteArray[pos++] = uBuffer.get(vuPos);
            }
        }
        ByteBuffer bufferWithoutPaddings=ByteBuffer.allocate(byteArray.length);
        // 数组放到buffer中
        bufferWithoutPaddings.put(byteArray);
        //重置 limit 和postion 值否则 buffer 读取数据不对
        bufferWithoutPaddings.flip();
        return bufferWithoutPaddings;
    }
    //Semi-Planar格式（SP）的处理和y通道的数据
    private static ByteBuffer getBufferWithoutPadding(ByteBuffer buffer, int width, int rowStride, int times,boolean isVbuffer){
        if(width == rowStride) return buffer;  //没有buffer,不用处理。
        int bufferPos = buffer.position();
        int cap = buffer.capacity();
        byte []byteArray = new byte[times*width];
        int pos = 0;
        //对于y平面，要逐行赋值的次数就是height次。对于uv交替的平面，赋值的次数是height/2次
        for (int i=0;i<times;i++) {
            buffer.position(bufferPos);
            //part 1.1 对于u,v通道,会缺失最后一个像u值或者v值，因此需要特殊处理，否则会crash
            if(isVbuffer && i==times-1){
                width = width -1;
            }
            buffer.get(byteArray, pos, width);
            bufferPos+= rowStride;
            pos = pos+width;
        }

        //nv21数组转成buffer并返回
        ByteBuffer bufferWithoutPaddings=ByteBuffer.allocate(byteArray.length);
        // 数组放到buffer中
        bufferWithoutPaddings.put(byteArray);
        //重置 limit 和postion 值否则 buffer 读取数据不对
        bufferWithoutPaddings.flip();
        return bufferWithoutPaddings;
    }

    static public byte[] YUV_420_888toNV21toJpeg(Image image) {
        int width =  image.getWidth();
        int height = image.getHeight();
        ByteBuffer yBuffer = getBufferWithoutPadding(image.getPlanes()[0].getBuffer(), image.getWidth(), image.getPlanes()[0].getRowStride(),image.getHeight(),false);
        ByteBuffer vBuffer;
        //part1 获得真正的消除padding的ybuffer和ubuffer。需要对P格式和SP格式做不同的处理。如果是P格式的话只能逐像素去做，性能会降低。
        if(image.getPlanes()[2].getPixelStride()==1){ //如果为true，说明是P格式。
            vBuffer = getuvBufferWithoutPaddingP(image.getPlanes()[1].getBuffer(), image.getPlanes()[2].getBuffer(),
                    width,height,image.getPlanes()[1].getRowStride(),image.getPlanes()[1].getPixelStride());
        }else{
            vBuffer = getBufferWithoutPadding(image.getPlanes()[2].getBuffer(), image.getWidth(), image.getPlanes()[2].getRowStride(),image.getHeight()/2,true);
        }

        //part2 将y数据和uv的交替数据（除去最后一个v值）赋值给nv21
        int ySize = yBuffer.remaining();
        int vSize = vBuffer.remaining();
        byte[] nv21;
        int byteSize = width*height*3/2;
        nv21 = new byte[byteSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);

        //part3 最后一个像素值的u值是缺失的，因此需要从u平面取一下。
        ByteBuffer uPlane = image.getPlanes()[1].getBuffer();
        byte lastValue = uPlane.get(uPlane.capacity() - 1);
        nv21[byteSize - 1] = lastValue;

        //return nv21;

        //nv21转jpeg
        //其中nv21Data为NV21格式的图像数据，width和height为图像的宽和高，quality为JPEG压缩质量，取值范围为0到100。函数返回值为转换后的JPEG格式的字节数组。
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 80, out);
        return out.toByteArray();

    }









}
