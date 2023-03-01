package com.example.camerajava;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Base64;
import android.util.Log;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MyWebSocket extends WebSocketListener {

    private static final int NORMAL_CLOSURE_STATUS = 1000;

    private WebSocket webSocket;
    private OkHttpClient client;
    private String serverUrl;

    public MyWebSocket(String serverUrl) {
        this.client = new OkHttpClient();
        this.serverUrl = serverUrl;
    }

    public void connect() {
        Log.d("MyWebSocket", "connect...");
        Request request = new Request.Builder().url(serverUrl).build();
        webSocket = client.newWebSocket(request, this);
    }

    public void close() {
        webSocket.close(1000,"断开连接...");
    }

    //发送图片，参数直接传入image
    public void sendImage(String apiName,Image image, String imageName) throws IOException, JSONException {
        //jpeg图像的字节流数组
        byte[] bytes = convertYUVToJPEG(image);
        //String base64Image = Base64.getEncoder().encodeToString(bytes);
        //转为base64编码，并使用字符串存储
        String base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP);
        JSONObject data = new JSONObject();
        data.put("image", base64Image);
        data.put("imageName",imageName);
        String message = data.toString();
        webSocket.send(message);
    }

    //YUV转jpeg字节数组
    private byte[] convertYUVToJPEG(Image image)  {
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

    //以下都是回调函数
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        Log.d("websocket","连接已打开！");
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        //System.out.println("接收到服务器发送的消息：" + text);
        Log.d("websocket","接收到服务器发送的消息：" + text);
//        int count = Integer.parseInt(text);
//        Log.d("updateUI", "当前个数： "+count);
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        //System.out.println("接收到服务器发送的字节流：" + bytes);
        Log.d("websocket","接收到服务器发送的字节流：" + bytes);

    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        //System.out.println("连接即将关闭，code=" + code + ", reason=" + reason);
        Log.d("websocket","连接即将关闭，code=" + code + ", reason=" + reason);
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        Log.d("websocket","连接失败，Throwable=" + t + ", response=" + response);
        t.printStackTrace();
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        //System.out.println("连接已关闭，code=" + code + ", reason=" + reason);
        Log.d("websocket","连接已关闭，code=" + code + ", reason=" + reason);
    }
}

