package com.example.camerajava;

import android.media.Image;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;

public class WebsocketThread extends Thread{
    private MyWebSocket myWebSocket;
    private Image image;
    private String imageName;

    public WebsocketThread(MyWebSocket myWebSocket, Image image, String imageName) {
        this.myWebSocket = myWebSocket;
        this.image = image;
        this.imageName = imageName;
    }

    //多线程，在run方法中执行消息发送
    @Override
    public void run() {
        Log.d("WebsocketThread", "多线程执行run方法发送消息ing...");
        super.run();
        try {
            myWebSocket.sendImage("jumpcount",image,imageName);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
