package com.watayouxiang.gradlerouter;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.watayouxiang.router.annotations.Destination;

@Destination(url = "router://page-home", description = "应用主页")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}