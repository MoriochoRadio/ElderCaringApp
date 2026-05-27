package com.example.caring01;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView timeTextView;
    private Handler handler;
    private Runnable runnable;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 요소 참조
        timeTextView = findViewById(R.id.timeTextView);
        ImageView userNV = findViewById(R.id.userNV);
        ImageView statisticNV = findViewById(R.id.statisticNV);


        // 사용자 정보 화면으로 이동
        userNV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, UserActivity.class);
                startActivity(intent);
            }
        });

        // 통계 화면으로 이동
        statisticNV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
                startActivity(intent);
            }
        });

        // 심박수 레이아웃 클릭 리스너 설정
        LinearLayout heartRateLayout = findViewById(R.id.heartRateLayout);
        heartRateLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, activity_heart_rate_statistics.class);
                startActivity(intent);
            }
        });

        // 수면 시간 레이아웃 클릭 리스너 설정
        LinearLayout sleepLayout = findViewById(R.id.SleepLayout);
        sleepLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, activity_sleep_time_statistics.class);
                startActivity(intent);
            }
        });

        // 움직임 레이아웃 클릭 리스너 설정
        LinearLayout walkLayout = findViewById(R.id.WalkLayout);
        walkLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, activity_movement_statistics.class);
                startActivity(intent);
            }
        });

        // 화장실 사용 레이아웃 클릭 리스너 설정
        LinearLayout toiletLayout = findViewById(R.id.ToiletLayout);
        toiletLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, activity_toilet_statistics.class);
                startActivity(intent);
            }
        });

        // 타임 업데이트 핸들러 설정
        handler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                updateCurrentTime();
                handler.postDelayed(this, 1000); // 1초마다 갱신
            }
        };
        handler.post(runnable); // Runnable 시작
    }



    // 현재 시간 업데이트 메서드
    private void updateCurrentTime() {
        String currentTime = new SimpleDateFormat("MM. dd (E) HH:mm", Locale.getDefault()).format(new Date());
        timeTextView.setText(currentTime);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable); // 핸들러 제거
    }
}

