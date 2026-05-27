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
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity2 extends AppCompatActivity {

    private TextView timeTextView, txtName, txtHeartRate, txtBpm;
    private Handler handler;
    private Runnable runnable;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // UI 요소 참조
        timeTextView = findViewById(R.id.timeTextView);
        txtName = findViewById(R.id.txtName);
        txtHeartRate = findViewById(R.id.txtHeartRate);
        ImageView userNV = findViewById(R.id.userNV);
        ImageView statisticNV = findViewById(R.id.statisticNV);

        // Firebase Database 초기화
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // 사용자 정보 가져오기
        fetchUserData();

        // 사용자 정보 화면으로 이동
        userNV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity2.this, UserActivity.class);
                startActivity(intent);
            }
        });

        // 통계 화면으로 이동
        statisticNV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity2.this, StatisticsActivity.class);
                startActivity(intent);
            }
        });

        // 레이아웃 클릭 리스너 설정
        LinearLayout sleepLayout = findViewById(R.id.SleepLayout);
        sleepLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity2.this, activity_sleep_time_statistics.class);
                startActivity(intent);
            }
        });

        LinearLayout walkLayout = findViewById(R.id.WalkLayout);
        walkLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity2.this, activity_movement_statistics.class);
                startActivity(intent);
            }
        });

        LinearLayout toiletLayout = findViewById(R.id.ToiletLayout);
        toiletLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity2.this, activity_toilet_statistics.class);
                startActivity(intent);
            }
        });

        // 심박수 레이아웃 클릭 리스너 설정
        LinearLayout heartRateLayout = findViewById(R.id.heartRateLayout);
        heartRateLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity2.this, activity_heart_rate_statistics.class);
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

    // Firebase 데이터 가져오기 메서드
    private void fetchUserData() {
        databaseReference.child("USER").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // 사용자 이름 설정
                String userName = dataSnapshot.child("HGD12345!").child("sname").getValue(String.class);
                if(userName != null){
                    txtName.setText(userName + "님");
                } else {
                    txtName.setText("이름 없음");
                }


                // 심박수 설정
                Integer heartRate = dataSnapshot.child("LiveData").child("heartRate").getValue(Integer.class);
                if (heartRate != null) {
                    txtHeartRate.setText(heartRate + ""); // 값 뒤에 "BPM" 추가
                } else {
                    txtHeartRate.setText("0 BPM");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity2.this, "데이터를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
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
