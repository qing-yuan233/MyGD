package com.example.camerajava;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

//import com.chaquo.python.PyObject;
//import com.chaquo.python.Python;
//import com.chaquo.python.android.AndroidPlatform;

//首页，从此处打开摄像头
public class Home extends AppCompatActivity implements View.OnClickListener{

    private Button openCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        openCamera = findViewById(R.id.openCamera);
        openCamera.setOnClickListener(this);

//        // 测试调用python算法
//        if (!Python.isStarted()){
//            Python.start(new AndroidPlatform(this));
//        }
//        Python python=Python.getInstance();
//        PyObject pyObject=python.getModule("test");
//        pyObject.callAttr("sayHello");
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            //跳转拍摄界面
            case R.id.openCamera:
                Intent intent = new Intent(this,MainActivity.class);
                startActivity(intent);
        }

    }
}