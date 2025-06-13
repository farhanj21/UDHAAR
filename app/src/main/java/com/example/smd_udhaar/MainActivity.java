package com.example.smd_udhaar;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    TextView tvLogo;
    ImageView udhaar_logo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        init();
        loadAnimations();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent= new Intent(MainActivity.this, login.class);
                startActivity(intent);
                finish();
            }
        }, 5000);
    }
    public void init(){
        tvLogo=findViewById(R.id.tvLogo);
        udhaar_logo=findViewById(R.id.logoIcon);
    }

    public void loadAnimations(){
        Animation animation_text= AnimationUtils.loadAnimation(MainActivity.this,R.anim.logo);
        tvLogo.startAnimation(animation_text);

        Animation animation_icon=AnimationUtils.loadAnimation(MainActivity.this,R.anim.logo_icon);
        udhaar_logo.startAnimation(animation_icon);
    }
}