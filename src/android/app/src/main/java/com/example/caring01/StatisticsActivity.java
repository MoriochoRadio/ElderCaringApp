package com.example.caring01;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {

    // Firebase 데이터베이스 참조
    private DatabaseReference databaseReference;

    // 현재 날짜와 주차를 계산하기 위한 캘린더 객체
    private Calendar calendar;

    // UI 요소
    private TextView txtHeartRateStatistics, txtSleepStatistics, txtMovementStatistics, txtToiletStatistics;
    private TextView sdheartRate, sdsleepData, sdsteps, sdtoilet;
    private TextView weekTextView;
    private Button prevWeekButton, nextWeekButton;



    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Firebase Database 초기화
        databaseReference = FirebaseDatabase.getInstance().getReference("USER/StatsData");

        // 캘린더 초기화 (현재 날짜 기준)
        calendar = Calendar.getInstance();


        // UI 요소 초기화
        txtHeartRateStatistics = findViewById(R.id.txtHeartRateStatistics);
        txtSleepStatistics = findViewById(R.id.txtSleepStatistics);
        txtMovementStatistics = findViewById(R.id.txtMovementStatistics);
        txtToiletStatistics = findViewById(R.id.txtToiletStatistics);

        sdheartRate = findViewById(R.id.sdheartRate);
        sdsleepData = findViewById(R.id.sdsleepData);
        sdsteps = findViewById(R.id.sdsteps);
        sdtoilet = findViewById(R.id.sdtoilet);

        weekTextView = findViewById(R.id.weekTextView);
        prevWeekButton = findViewById(R.id.prevWeekButton);
        nextWeekButton = findViewById(R.id.nextWeekButton);

        // 초기 주 표시
        updateWeekTextView();

        // 이전/다음 주 버튼 클릭 리스너
        prevWeekButton.setOnClickListener(v -> {
            calendar.add(Calendar.WEEK_OF_YEAR, -1); // 이전 주로 이동
            updateWeekTextView();
            updateStatisticsFromFirebase();
        });

        nextWeekButton.setOnClickListener(v -> {
            calendar.add(Calendar.WEEK_OF_YEAR, 1); // 다음 주로 이동
            updateWeekTextView();
            updateStatisticsFromFirebase();
        });

        // 초기 Firebase 데이터 로드
        updateStatisticsFromFirebase();
    }

    // 현재 주를 텍스트뷰에 표시하는 메서드
    private void updateWeekTextView() {
        Calendar startOfWeek = (Calendar) calendar.clone();
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        Calendar endOfWeek = (Calendar) calendar.clone();
        endOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM월 dd일", Locale.getDefault());
        String weekRange = dateFormat.format(startOfWeek.getTime()) + " ~ " + dateFormat.format(endOfWeek.getTime());
        weekTextView.setText(weekRange);
    }

    // Firebase에서 데이터를 가져오는 메서드
    private void updateStatisticsFromFirebase() {
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH는 0부터 시작
        int currentWeek = calendar.get(Calendar.WEEK_OF_MONTH);

        // 심박수 데이터 가져오기
        String heartRatePath = "heartRate/" + currentYear + "/" + currentMonth + "/week" + currentWeek + "/Weekly average";
        fetchHeartRateData(heartRatePath);

        // 수면 데이터 가져오기
        String sleepPath = "sleepData/" + currentYear + "/" + currentMonth + "/week" + currentWeek + "/Weekly average";
        fetchSleepData(sleepPath);

        // 움직임 데이터 가져오기
        String movementPath = "steps/" + currentYear + "/" + currentMonth + "/week" + currentWeek + "/Weekly steps";
        fetchMovementData(movementPath);

        // 화장실 데이터 가져오기
        String toiletPath = "toiletUsage/" + currentYear + "/" + currentMonth + "/week" + currentWeek + "/weeklyCount";
        fetchToiletData(toiletPath);
    }

    private void fetchHeartRateData(String currentPath) {
        Calendar prevWeek = (Calendar) calendar.clone();
        prevWeek.add(Calendar.WEEK_OF_YEAR, -1);
        int prevYear = prevWeek.get(Calendar.YEAR);
        int prevMonth = prevWeek.get(Calendar.MONTH) + 1; // Calendar.MONTH는 0부터 시작
        int prevWeekNum = prevWeek.get(Calendar.WEEK_OF_MONTH);
        String prevPath = "heartRate/" + prevYear + "/" + prevMonth + "/week" + prevWeekNum + "/Weekly average";

        // 현재 주 데이터 가져오기
        databaseReference.child(currentPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot currentSnapshot) {
                Integer currentHeartRate = currentSnapshot.exists() ? currentSnapshot.getValue(Integer.class) : null;

                // 이전 주 데이터 가져오기
                databaseReference.child(prevPath).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot prevSnapshot) {
                        Integer prevHeartRate = prevSnapshot.exists() ? prevSnapshot.getValue(Integer.class) : null;

                        // 변화량 계산
                        String changeText = "";
                        if (currentHeartRate != null && prevHeartRate != null) {
                            int change = currentHeartRate - prevHeartRate;
                            changeText = "\n(" + (change >= 0 ? "+" : "") + change + " BPM 변화)";
                        }

                        // 상태 텍스트 가져오기
                        String status = getHeartRateStatusText(currentHeartRate);

                        // UI 업데이트
                        updateHeartRateUI(currentHeartRate, changeText, status);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("StatisticsActivity", "Firebase Error: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StatisticsActivity", "Firebase Error: " + error.getMessage());
            }
        });
    }



    private void fetchSleepData(String currentPath) {
        Calendar prevWeek = (Calendar) calendar.clone();
        prevWeek.add(Calendar.WEEK_OF_YEAR, -1);
        int prevYear = prevWeek.get(Calendar.YEAR);
        int prevMonth = prevWeek.get(Calendar.MONTH) + 1;
        int prevWeekNum = prevWeek.get(Calendar.WEEK_OF_MONTH);
        String prevPath = "sleepTime/" + prevYear + "/" + prevMonth + "/week" + prevWeekNum + "/Weekly average";

        databaseReference.child(currentPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot currentSnapshot) {
                Double currentSleepTime = currentSnapshot.exists() ? currentSnapshot.getValue(Double.class) : null;
                Log.d("SleepData", "Current Sleep Time: " + currentSleepTime);

                databaseReference.child(prevPath).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot prevSnapshot) {
                        Double prevSleepTime = prevSnapshot.exists() ? prevSnapshot.getValue(Double.class) : null;
                        Log.d("SleepData", "Previous Sleep Time: " + prevSleepTime);

                        String changeText = "";
                        if (currentSleepTime != null && prevSleepTime != null) {
                            double change = currentSleepTime - prevSleepTime;
                            if (change != 0) {
                                changeText = "\n(" + (change >= 0 ? "+" : "") + String.format("%.1f", change) + "시간 변화)";
                            } else {
                                changeText = " (변화 없음)";
                            }
                        }


                        String status = getSleepStatusText(currentSleepTime);

                        updateSleepUI(currentSleepTime, changeText, status);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("SleepData", "Firebase Error: " + error.getMessage());
                    }
                });
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StatisticsActivity", "Firebase Error: " + error.getMessage());
            }
        });
    }



    private void fetchMovementData(String currentPath) {
        Calendar prevWeek = (Calendar) calendar.clone();
        prevWeek.add(Calendar.WEEK_OF_YEAR, -1);
        int prevYear = prevWeek.get(Calendar.YEAR);
        int prevMonth = prevWeek.get(Calendar.MONTH) + 1;
        int prevWeekNum = prevWeek.get(Calendar.WEEK_OF_MONTH);
        String prevPath = "movement/" + prevYear + "/" + prevMonth + "/week" + prevWeekNum + "/Weekly steps";

        databaseReference.child(currentPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot currentSnapshot) {
                Integer currentMovement = currentSnapshot.exists() ? currentSnapshot.getValue(Integer.class) : null;

                databaseReference.child(prevPath).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot prevSnapshot) {
                        Integer prevMovement = prevSnapshot.exists() ? prevSnapshot.getValue(Integer.class) : null;

                        String changeText = "";
                        if (currentMovement != null && prevMovement != null) {
                            int change = currentMovement - prevMovement;
                            changeText = " (" + (change >= 0 ? "+" : "") + change + "회 변화)";
                        }

                        String status = getMovementStatusText(currentMovement);

                        updateMovementUI(currentMovement, changeText, status);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("StatisticsActivity", "Firebase Error: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StatisticsActivity", "Firebase Error: " + error.getMessage());
            }
        });
    }


    private void fetchToiletData(String currentPath) {
        Calendar prevWeek = (Calendar) calendar.clone();
        prevWeek.add(Calendar.WEEK_OF_YEAR, -1);
        int prevYear = prevWeek.get(Calendar.YEAR);
        int prevMonth = prevWeek.get(Calendar.MONTH) + 1;
        int prevWeekNum = prevWeek.get(Calendar.WEEK_OF_MONTH);
        String prevPath = "restroom/" + prevYear + "/" + prevMonth + "/week" + prevWeekNum + "/Weekly average";

        databaseReference.child(currentPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot currentSnapshot) {
                Integer currentRestroom = currentSnapshot.exists() ? currentSnapshot.getValue(Integer.class) : null;

                databaseReference.child(prevPath).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot prevSnapshot) {
                        Integer prevRestroom = prevSnapshot.exists() ? prevSnapshot.getValue(Integer.class) : null;

                        String changeText = "";
                        if (currentRestroom != null && prevRestroom != null) {
                            int change = currentRestroom - prevRestroom;
                            changeText = " (" + (change >= 0 ? "+" : "") + change + "회 변화)";
                        }

                        String status = getToiletStatusText(currentRestroom/7);

                        updateToiletUI(currentRestroom, changeText, status);
                        Log.d("ToiletData", "Current Path: " + currentPath);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("StatisticsActivity", "Firebase Error: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StatisticsActivity", "Firebase Error: " + error.getMessage());

            }
        });
    }


    // 상태 판단 로직 (각각 텍스트와 색상을 반환)
    private String getHeartRateStatusText(Integer value) {
        if (value == null) return "데이터 없음";
        if (value >= 60 && value <= 100) return "양호(적정)";
        if (value < 60) return "주의 : 심박수가 낮습니다.";
        return "주의 : 심박수가 높습니다.";
    }

    private int getHeartRateStatusColor(Integer value) {
        if (value == null) return ContextCompat.getColor(this, android.R.color.darker_gray);
        if (value >= 60 && value <= 100)
            return ContextCompat.getColor(this, android.R.color.holo_green_dark);
        if (value < 60)
            return ContextCompat.getColor(this, android.R.color.holo_red_dark);
        return ContextCompat.getColor(this, android.R.color.holo_red_dark);
    }

    private String getSleepStatusText(Double value) {
        if (value == null) return "데이터 없음";
        if (value >= 7 && value <= 8) return "양호(적정)";
        if (value < 7) return "보통 : 수면 시간이 부족합니다.";
        return "보통 : 수면 시간이 깁니다.";
    }

    private int getSleepStatusColor(Double value) {
        if (value == null) return ContextCompat.getColor(this, android.R.color.darker_gray);
        if (value >= 7 && value <= 8)
            return ContextCompat.getColor(this, android.R.color.holo_green_dark);
        if (value < 7)
            return ContextCompat.getColor(this, android.R.color.holo_orange_light);
        return ContextCompat.getColor(this, android.R.color.holo_orange_light);
    }

    private String getMovementStatusText(Integer value) {
        if (value == null) return "데이터 없음";
        if (value >= 5000) return "양호(적정)";
        if (value >= 3000) return "보통 : 활동량이 다소 적습니다.";
        return "주의 : 활동량이 매우 적습니다.";
    }

    private int getMovementStatusColor(Integer value) {
        if (value == null) return ContextCompat.getColor(this, android.R.color.darker_gray);
        if (value >= 5000)
            return ContextCompat.getColor(this, android.R.color.holo_green_dark);
        if (value >= 3000)
            return ContextCompat.getColor(this, android.R.color.holo_orange_light);
        return ContextCompat.getColor(this, android.R.color.holo_red_dark);
    }

    private String getToiletStatusText(Integer value) {
        if (value == null) return "데이터 없음";
        if (value >= 4 && value <= 6) return "양호(적정)";
        if (value < 4) return "주의 : 배뇨 횟수가 적습니다.";
        return "주의 : 배뇨 횟수가 많습니다.";
    }


    private int getToiletStatusColor(Integer value) {
        if (value == null) return ContextCompat.getColor(this, android.R.color.darker_gray);
        if (value >= 4 && value <= 6)
            return ContextCompat.getColor(this, android.R.color.holo_green_dark);
        if (value < 3)
            return ContextCompat.getColor(this, android.R.color.holo_red_dark);
        return ContextCompat.getColor(this, android.R.color.holo_red_dark);
    }

    private void updateHeartRateUI(Integer value, String changeText, String status) {
        if (value != null) {
            // 텍스트뷰: 심박수와 변화량 표시
            txtHeartRateStatistics.setText("이번주 평균 심박수는 " + value + " BPM입니다." + changeText);
            txtHeartRateStatistics.setTextColor(ContextCompat.getColor(this, android.R.color.black));

            // 상태 텍스트뷰: 상태만 표시
            sdheartRate.setText(status);
            sdheartRate.setTextColor(getHeartRateStatusColor(value));
        } else {
            // 데이터가 없을 때
            txtHeartRateStatistics.setText("심박수 데이터를 가져올 수 없습니다.");
            txtHeartRateStatistics.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));

            sdheartRate.setText("데이터 없음");
            sdheartRate.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }
    }



    private void updateSleepUI(Double value, String changeText, String status) {
        if (value != null) {
            txtSleepStatistics.setText("이번주 평균 수면 시간은 " + String.format("%.1f", value) + "시간입니다." + changeText);
            txtSleepStatistics.setTextColor(ContextCompat.getColor(this, android.R.color.black));

            sdsleepData.setText(status);
            sdsleepData.setTextColor(getSleepStatusColor(value));
        } else {
            txtSleepStatistics.setText("수면 시간 데이터를 가져올 수 없습니다.");
            txtSleepStatistics.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));

            sdsleepData.setText("데이터 없음");
            sdsleepData.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }
    }



    private void updateMovementUI(Integer value, String changeText, String status) {
        if (value != null) {
            txtMovementStatistics.setText("이번주 평균 움직임 횟수는 " + value + "회입니다." + changeText);
            txtMovementStatistics.setTextColor(ContextCompat.getColor(this, android.R.color.black));

            sdsteps.setText(status);
            sdsteps.setTextColor(getMovementStatusColor(value));
        } else {
            txtMovementStatistics.setText("움직임 데이터를 가져올 수 없습니다.");
            txtMovementStatistics.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));

            sdsteps.setText("데이터 없음");
            sdsteps.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }
    }


    private void updateToiletUI(Integer value, String changeText, String status) {
        int avg = value / 7;
        if (value != null) {
            txtToiletStatistics.setText("이번주 평균 화장실 사용 횟수는 " + avg + "회입니다." + changeText);
            txtToiletStatistics.setTextColor(ContextCompat.getColor(this, android.R.color.black));

            sdtoilet.setText(status);
            // 올바른 색상 적용
            sdtoilet.setTextColor(getToiletStatusColor(avg));
        } else {
            txtToiletStatistics.setText("화장실 데이터를 가져올 수 없습니다.");
            txtToiletStatistics.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));

            sdtoilet.setText("데이터 없음");
            sdtoilet.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }
    }




}
