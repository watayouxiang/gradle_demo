package com.watayouxiang.gradlerouter;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.watayouxiang.gradle.router.runtime.Router;
import com.watayouxiang.router.annotations.Destination;

@Destination(url = "router://page-home", description = "应用主页")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Router.INSTANCE.go(MainActivity.this, "router://watayouxiang/profile?name=imooc&message=hello");
            }
        });
    }
}