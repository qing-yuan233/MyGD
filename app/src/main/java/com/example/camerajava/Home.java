package com.example.camerajava;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

//首页，从此处打开摄像头
public class Home extends AppCompatActivity implements View.OnClickListener{

    private Button openCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        openCamera = findViewById(R.id.openCamera);
        openCamera.setOnClickListener(this);
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