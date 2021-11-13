package com.watayouxiang.gradlerouter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.watayouxiang.router.annotations.Destination

@Destination(url = "router://page-user", description = "用户信息")
class UserActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
    }
}