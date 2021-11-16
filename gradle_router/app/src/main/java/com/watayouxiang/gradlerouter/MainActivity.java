package com.watayouxiang.gradlerouter;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.watayouxiang.gradle.router.runtime.Router;
import com.watayouxiang.router.annotations.Destination;

@Destination(url = "router://page-home", description = "应用主页")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv_text = findViewById(R.id.tv_text);

        // 测试：获取渠道信息
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            String wtChannel = appInfo.metaData.getString("WT_CHANNEL");
            tv_text.setText("wtChannel = " + wtChannel);
        } catch (Exception ignored) {
        }

        // 测试：页面跳转
        tv_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Router.INSTANCE.go(MainActivity.this, "router://watayouxiang/profile?name=imooc&message=hello");
            }
        });
    }
}