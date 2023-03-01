package com.example.camerajava;

import static androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888;
import static com.example.camerajava.MainActivity.Configuration.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.camerajava.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

//摄像头开启的页面
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding viewBinding;

    private ImageCapture imageCapture = null;

    private VideoCapture videoCapture = null;
    private Recording recording = null;

    private ExecutorService cameraExecutor;

    private static MyWebSocket myWebSocket;

    //发送照片的频率
    private static int frequent;

    private static int imgNumber;

    //计数结果
    private int jumpCount;

    private static TextView countTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        countTextView = (TextView) findViewById(R.id.countNumber);

        //每5帧发送一次请求
        frequent = 5;
        //摄像头捕获的图片数量计数
        imgNumber = 0;

        // 请求相机权限
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, Configuration.REQUIRED_PERMISSIONS,
                    Configuration.REQUEST_CODE_PERMISSIONS);
        }

        // 设置拍照按钮监听
        viewBinding.imageCaptureButton.setOnClickListener(v -> takePhoto());
        viewBinding.videoCaptureButton.setOnClickListener(v -> captureVideo());
        //通过录像按钮开启实时分析
        //viewBinding.videoCaptureButton.setOnClickListener(v -> turnImageAnalysis());

        cameraExecutor = Executors.newSingleThreadExecutor();

        //实例化websocket
        //myWebSocket = new MyWebSocket("http://192.168.1.102:9999");
        //连接
        //myWebSocket.connect();

        jumpCount=0;



    }

    //录像并保存
    @SuppressLint("CheckResult")
    private void captureVideo() {
        // 确保videoCapture 已经被实例化，否则程序可能崩溃
        if (videoCapture != null) {
            // 禁用UI，直到CameraX 完成请求
            viewBinding.videoCaptureButton.setEnabled(false);

            Recording curRecording = recording;
            if (curRecording != null) {
                // 如果正在录制，需要先停止当前的 recording session（录制会话）
                curRecording.stop();
                recording = null;
                return;
            }

            // 创建一个新的 recording session
            // 首先，创建MediaStore VideoContent对象，用以设置录像通过MediaStore的方式保存
            String name = new SimpleDateFormat(Configuration.FILENAME_FORMAT, Locale.SIMPLIFIED_CHINESE)
                    .format(System.currentTimeMillis());
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");
            }

            MediaStoreOutputOptions mediaStoreOutputOptions = new MediaStoreOutputOptions
                    .Builder(getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                    .setContentValues(contentValues)
                    .build();
            // 申请音频权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        Configuration.REQUEST_CODE_PERMISSIONS);
            }
            Recorder recorder = (Recorder) videoCapture.getOutput();
            recording = recorder.prepareRecording(this, mediaStoreOutputOptions)
                    .withAudioEnabled() // 开启音频录制
                    // 开始新录制
                    .start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
                        if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                            viewBinding.videoCaptureButton.setText(getString(R.string.stop_capture));
                            viewBinding.videoCaptureButton.setEnabled(true);// 启动录制时，切换按钮显示文本
                        } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {//录制完成后，使用Toast通知
                            if (!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) {
                                String msg = "Video capture succeeded: " +
                                        ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults()
                                                .getOutputUri();
                                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                                Log.d(TAG, msg);
                            } else {
                                if (recording != null) {
                                    recording.close();
                                    recording = null;
                                    Log.e(TAG, "Video capture end with error: " +
                                            ((VideoRecordEvent.Finalize) videoRecordEvent).getError());
                                }
                            }
                            viewBinding.videoCaptureButton.setText(getString(R.string.start_capture));
                            viewBinding.videoCaptureButton.setEnabled(true);
                        }
                    });
        }
    }

    //拍照
    private void takePhoto() {
        // 确保imageCapture 已经被实例化, 否则程序将可能崩溃
        if (imageCapture != null) {
            // 创建一个 "MediaStore Content" 以保存图片，带时间戳是为了保证文件名唯一
            String name = new SimpleDateFormat(Configuration.FILENAME_FORMAT,
                    Locale.SIMPLIFIED_CHINESE).format(System.currentTimeMillis());
            //contenvalues类类似哈希，存储键值对
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
            }

            // 创建 output option 对象，用以指定照片的输出方式。
            // 在这个对象中指定有关我们希望输出如何的方式。我们希望将输出保存在 MediaStore 中，以便其他应用可以显示它
            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions
                    .Builder(getContentResolver(),
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues)
                    .build();

            // 设置拍照监听，用以在照片拍摄后执行takePicture（拍照）方法
            imageCapture.takePicture(outputFileOptions,
                    ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {// 保存照片时的回调
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            String msg = "照片捕获成功! " + outputFileResults.getSavedUri();
                            Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, msg);
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e(TAG, "Photo capture failed: " + exception.getMessage());// 拍摄或保存失败时
                        }
                    });
        }


    }



    //打开之后有预览画面
    private void startCamera() {
        // 将Camera的生命周期和Activity绑定在一起（设定生命周期所有者），这样就不用手动控制相机的启动和关闭。
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // 将你的相机和当前生命周期的所有者绑定所需的对象
                ProcessCameraProvider processCameraProvider = cameraProviderFuture.get();

                // 创建一个Preview 实例，并设置该实例的 surface 提供者（provider）。
                PreviewView viewFinder = (PreviewView)findViewById(R.id.viewFinder);
                Preview preview = new Preview.Builder()
                        .build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // 创建录像所需实例
                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                // 选择后置摄像头作为默认摄像头
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // 创建拍照所需的实例
                imageCapture = new ImageCapture.Builder().build();

                 //设置预览帧分析(这里设置了捕获图像的格式为rgba8888)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        //.setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, new MyAnalyzer());

                // 重新绑定用例前先解绑
                processCameraProvider.unbindAll();

                // 绑定用例至相机
                processCameraProvider.bindToLifecycle(MainActivity.this, cameraSelector,
                        preview,
                        /*imageCapture,*/
                        imageAnalysis/*,
                        videoCapture*/);

            } catch (Exception e) {
                Log.e(TAG, "用例绑定失败！" + e);
            }
        }, ContextCompat.getMainExecutor(this));

    }


    private static void sendOkhttp(ImageProxy imageProxy,Image image,String imageName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //jpeg图像的字节流数组
                byte[] bytes = Utils.YuvToJpeg(image);
                //String base64Image = Base64.getEncoder().encodeToString(bytes);
                //转为base64编码，并使用字符串存储
                String base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP);
                JSONObject data = new JSONObject();
                try {
                    data.put("image", base64Image);
                    data.put("imageName",imageName);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                String message = data.toString();

                OkHttpClient okHttpClient = new OkHttpClient();

                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(message,mediaType);

                Request request = new Request.Builder()
                        .url("http://192.168.1.102:9999/jumpcount")
                        .post(body)
                        .build();



                //Request request = new Request.Builder().url("http://192.168.1.102:9999").addHeader("Connection", "close").build();
                try {
                    Response response = okHttpClient.newCall(request).execute();
                    String responseDate = response.body().string();
                    Log.d(TAG, "返回结果："+responseDate);


                } catch (IOException e) {
                    //sendOkhttp(image,imageName);
                    throw new RuntimeException(e);
                }
                //imageProxy.close();

            }
        }).start();
    }

    //实时预览分析类
    //这个分析类就是打印每一个预览帧画面的时间戳
    private class MyAnalyzer implements ImageAnalysis.Analyzer{
        @SuppressLint("UnsafeOptInUsageError")
        @Override
        public void analyze(@NonNull ImageProxy image) {
            //或取实时显示图像
            final Image singleImg = image.getImage();

            imgNumber++;
            if (imgNumber%frequent==0) {
                //打印图像信息
                Log.d("size", singleImg.getWidth() + ", " + singleImg.getHeight());
                //设置图像名称和格式
                String name = new SimpleDateFormat(Configuration.FILENAME_FORMAT,
                        Locale.SIMPLIFIED_CHINESE).format(System.currentTimeMillis());


                //使用websocket发送
//                try {
//                    myWebSocket.sendImage("jumpcount",singleImg,name);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                } catch (JSONException e) {
//                    throw new RuntimeException(e);
//                }
                //发送http请求==========================================================
                //sendOkhttp(image,singleImg,name);
                //jpeg图像的字节流数组
                //byte[] bytes = Utils.YuvToJpeg(singleImg);
                //byte[] bytes = Utils.Yuv_420_888_ToJpeg(singleImg);
                byte[] bytes = Utils.YUV_420_888toNV21toJpeg(singleImg);
                //byte[] bytes = Utils.RGB8888ToJpeg(singleImg);
                //String base64Image = Base64.getEncoder().encodeToString(bytes);
                //转为base64编码，并使用字符串存储
                String base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP);
                JSONObject data = new JSONObject();
                try {
                    data.put("image", base64Image);
                    data.put("imageName",name);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                String message = data.toString();

                OkHttpClient okHttpClient = new OkHttpClient();

                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(message,mediaType);

                Request request = new Request.Builder()
                        .url("http://192.168.1.102:9999/jumpcount")
                        .post(body)
                        .build();

                // 发送请求并处理响应
                okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Server response: " + responseBody);
                        //更新UI
                        if (TextUtils.isDigitsOnly(responseBody)) {
                            int num = Integer.parseInt(responseBody);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // 在主线程中更新UI
                                    countTextView.setText("计数结果：" + num);
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e(TAG, "Failed to send image to server", e);
                    }

                });

            }
            image.close();

            //Log.d(Configuration.TAG, "Image's stamp is " + Objects.requireNonNull(image.getImage()).getTimestamp());
            //Log.d("size", singleImg.getWidth() + ", " + singleImg.getHeight());



            //设置图像名称和格式
//            String name = new SimpleDateFormat(Configuration.FILENAME_FORMAT,
//                    Locale.SIMPLIFIED_CHINESE).format(System.currentTimeMillis());

//            ContentValues contentValues = new ContentValues();
//            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
//            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

            //websocket连接发送图片(此时此刻已经连上了)

//            //子线程里发送图像和信息
//            WebsocketThread websocketThread = new WebsocketThread(myWebSocket,singleImg,name);
//            websocketThread.start();

//          myWebSocket.sendImage(singleImg,name);
//          在其回调函数里先打印计数




            //获取图像后做的另一些处理
            //这里列举了一些
//            try {
//                Bitmap bitmapImg = MyTool.toBitmap(singleImg);
//                MyTool tmp = new MyTool();
//                tmp.saveBitmap(name,bitmapImg,MainActivity.this);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }

            //image.close();
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : Configuration.REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //capmyWebSocket.close();
        cameraExecutor.shutdown();
    }

    static class Configuration {
        public static final String TAG = "CameraxBasic";
        public static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
        public static final int REQUEST_CODE_PERMISSIONS = 10;
        public static final int REQUEST_AUDIO_CODE_PERMISSIONS = 12;
        public static final String[] REQUIRED_PERMISSIONS =
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ?
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE} :
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO};
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Configuration.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {// 申请权限通过
                startCamera();
            } else {// 申请权限失败
                Toast.makeText(this, "用户拒绝授予权限！", Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (requestCode == Configuration.REQUEST_AUDIO_CODE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this,
                    "Manifest.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "未授权录制音频权限！", Toast.LENGTH_LONG).show();
            }
        }
    }



}