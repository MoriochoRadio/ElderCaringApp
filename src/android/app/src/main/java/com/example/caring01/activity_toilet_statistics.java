package com.example.caring01;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class activity_toilet_statistics extends AppCompatActivity {

    private TextView tabDaily, tabWeekly, tabMonthly, tabYearly, weekTextView;
    private RecyclerView toiletView;
    private ToiletUsageAdapter usageAdapter;
    private List<ToiletUsage> usageList;
    private DatabaseReference databaseReference;
    private TextView usageCountTextView;

    private Date currentDate;
    private Calendar currentWeek; // 현재 주를 저장할 캘린더 객체
    private SimpleDateFormat dateFormat;

    private Button prevButton, nextButton; // 이전, 다음 버튼
    private String currentView = "daily"; // 현재 보는 단위 (daily, weekly, monthly, yearly)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toilet_statistics);

        // 뷰 초기화
        toiletView = findViewById(R.id.ToiletrecyclerView);
        weekTextView = findViewById(R.id.weekTextView);

        tabDaily = findViewById(R.id.tab_daily);
        tabWeekly = findViewById(R.id.tab_weekly);
        tabMonthly = findViewById(R.id.tab_monthly);
        tabYearly = findViewById(R.id.tab_yearly);

        usageList = new ArrayList<>();
        usageAdapter = new ToiletUsageAdapter(usageList);
        toiletView.setAdapter(usageAdapter);
        toiletView.setLayoutManager(new LinearLayoutManager(this));

        // Firebase 초기화
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("USER/StatsData/toiletUsage");
        currentDate = new Date();

        // 날짜 포맷
        dateFormat = new SimpleDateFormat("MM월 dd일", Locale.getDefault());

        // 현재 주를 기준으로 설정
        currentWeek = Calendar.getInstance();
        showCurrentWeekRange();

        // 버튼 설정
        prevButton = findViewById(R.id.prevWeekButton);
        nextButton = findViewById(R.id.nextWeekButton);

        prevButton.setOnClickListener(v -> moveTime(-1)); // 이전 시간으로 이동
        nextButton.setOnClickListener(v -> moveTime(1)); // 다음 시간으로 이동

        // 탭 버튼 클릭 리스너 설정
        tabDaily.setOnClickListener(v -> {
            currentView = "daily";
            loadDailyData();
            showCurrentDay();// 일별 데이터 로드
        });

        tabWeekly.setOnClickListener(v -> {
            currentView = "weekly";
            loadWeeklyData();
            showCurrentWeekRange();// 주별 데이터 로드
        });

        tabMonthly.setOnClickListener(v -> {
            currentView = "monthly";
            loadMonthlyData();
            showCurrentMonth();// 월별 데이터 로드
        });

        tabYearly.setOnClickListener(v -> {
            currentView = "yearly";
            loadYearlyData(); // 연별 데이터 로드
            showCurrentYear();
        });

        // 기본적으로 일별 데이터를 로드
        loadDailyData();
        showCurrentDay();
    }

    // 시간 범위 이동 (일, 주, 월, 년에 따라)
    private void moveTime(int direction) {
        switch (currentView) {
            case "daily":
                currentWeek.add(Calendar.DAY_OF_YEAR, direction);
                showCurrentDay();
                loadDailyData();
                break;
            case "weekly":
                currentWeek.add(Calendar.WEEK_OF_YEAR, direction);
                showCurrentWeekRange();
                loadWeeklyData();
                break;
            case "monthly":
                currentWeek.add(Calendar.MONTH, direction);
                showCurrentMonth();
                loadMonthlyData();
                break;
            case "yearly":
                currentWeek.add(Calendar.YEAR, direction);
                showCurrentYear();
                loadYearlyData();
                break;
        }
    }


    // 현재 일만 표시
    private void showCurrentDay() {
        String formattedDay = dateFormat.format(currentWeek.getTime());
        weekTextView.setText(formattedDay);
    }

    // 현재 주차 표시
    private void showCurrentWeekRange() {
        Calendar startOfWeek = (Calendar) currentWeek.clone();
        Calendar endOfWeek = (Calendar) currentWeek.clone();

        // 주차 계산
        int dayOfMonth = startOfWeek.get(Calendar.DAY_OF_MONTH);
        int weekOfMonth = (dayOfMonth - 1) / 7 + 1;  // 1-7: week1, 8-14: week2 ...

        // 해당 주차의 범위 계산
        int startDay = (weekOfMonth - 1) * 7 + 1;
        int endDay = Math.min(weekOfMonth * 7, startOfWeek.getActualMaximum(Calendar.DAY_OF_MONTH));

        startOfWeek.set(Calendar.DAY_OF_MONTH, startDay);
        endOfWeek.set(Calendar.DAY_OF_MONTH, endDay);

        // 주차 범위를 텍스트로 표시
        String weekRange = dateFormat.format(startOfWeek.getTime()) + " ~ " + dateFormat.format(endOfWeek.getTime());
        weekTextView.setText(weekRange);
    }



    // 현재 월 표시
    private void showCurrentMonth() {
        Calendar startOfMonth = (Calendar) currentWeek.clone();
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1);

        Calendar endOfMonth = (Calendar) currentWeek.clone();
        endOfMonth.set(Calendar.DAY_OF_MONTH, currentWeek.getActualMaximum(Calendar.DAY_OF_MONTH));

        String monthRange = new SimpleDateFormat("MM월", Locale.getDefault()).format(startOfMonth.getTime()) + " ~ " +
                new SimpleDateFormat("MM월 dd일", Locale.getDefault()).format(endOfMonth.getTime());
        weekTextView.setText(monthRange);
    }

    // 현재 연도 표시
    private void showCurrentYear() {
        int year = currentWeek.get(Calendar.YEAR);
        weekTextView.setText(year + "년");
    }

    private void usageCountTextView(){

    }

    // 일별 데이터 로드
    private void loadDailyData() {
        String currentYear = String.valueOf(currentWeek.get(Calendar.YEAR));
        String currentMonth = String.format(Locale.getDefault(), "%02d", currentWeek.get(Calendar.MONTH) + 1);
        String todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentWeek.getTime());

        // fetchToiletData를 사용하여 특정 날짜의 데이터를 가져옵니다
        fetchToiletData(currentWeek.getTime());
    }

    private void fetchToiletData(Date date) {
        SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat sdfMonth = new SimpleDateFormat("M", Locale.getDefault());
        SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String year = sdfYear.format(date);
        String month = sdfMonth.format(date);
        String formattedDate = sdfDay.format(date);

        // 월별 주차 계산 (1~7일, 8~14일 등)
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int weekOfMonth = (dayOfMonth - 1) / 7 + 1;  // 1~7일: week1, 8~14일: week2, 15~21일: week3, ...

        String week = "week" + weekOfMonth;

        // Firebase 경로 생성
        DatabaseReference path = databaseReference.child(year)
                .child(month)
                .child(week)
                .child(formattedDate);

        Log.d("ToiletActivity", "Fetching data from path: " + path.toString());

        path.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double dailyAverage = snapshot.child("Daily average").getValue(Double.class);
                    Long dailyCount = snapshot.child("Daily count").getValue(Long.class);

                    Log.d("ToiletActivity", "Daily Average: " + dailyAverage + ", Daily Count: " + dailyCount);

                    // 사용 기록을 리스트에 추가
                    List<ToiletUsage> toiletUsageList = new ArrayList<>();
                    for (DataSnapshot usageSnapshot : snapshot.getChildren()) {
                        if (!usageSnapshot.getKey().equals("Daily average") && !usageSnapshot.getKey().equals("Daily count")) {
                            String count = usageSnapshot.getKey();
                            Integer duration = usageSnapshot.child("duration").getValue(Integer.class);
                            String startTime = usageSnapshot.child("startTime").getValue(String.class);
                            String endTime = usageSnapshot.child("endTime").getValue(String.class);

                            // ToiletUsage 객체 생성하여 리스트에 추가
                            toiletUsageList.add(new ToiletUsage(count, startTime, endTime,duration));
                        }
                    }

                    // RecyclerView에 어댑터 설정
                    usageAdapter = new ToiletUsageAdapter(toiletUsageList);
                    toiletView.setAdapter(usageAdapter);
                } else {
                    Log.d("ToiletActivity", "No data found for " + formattedDate);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("ToiletActivity", "Failed to read value.", error.toException());
            }
        });
    }

    private void fetchToiletWeeklyData(Date date) {
        SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat sdfMonth = new SimpleDateFormat("M", Locale.getDefault());
        SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String year = sdfYear.format(date);
        String month = sdfMonth.format(date);

        // 월별 주차 계산 (1~7일, 8~14일 등)
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int weekOfMonth = (dayOfMonth - 1) / 7 + 1;  // 주차 계산

        String week = "week" + weekOfMonth;

        // Firebase 경로 생성
        DatabaseReference path = databaseReference.child(year)
                .child(month)
                .child(week);

        Log.d("ToiletActivity", "Fetching data from path: " + path.toString());

        path.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<ToiletUsage> toiletUsageList = new ArrayList<>();

                    for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                        String date = dateSnapshot.getKey();  // 주별 날짜 키 가져오기
                        Double dailyAverage = dateSnapshot.child("Daily average").getValue(Double.class);
                        Long dailyCount = dateSnapshot.child("Daily count").getValue(Long.class);

                        if (dailyCount != null && dailyAverage != null) {
                            // 날짜의 일(day) 부분 추출
                            String day = date.split("-")[2];  // "2024-11-15"에서 "15" 추출

                            // ToiletUsage 객체 생성 후 리스트에 추가
                            toiletUsageList.add(new ToiletUsage(day + "일", String.valueOf(dailyCount), dailyAverage));
                            Log.d("ToiletActivity", "Added: Date=" + date + ", Count=" + dailyCount + ", Avg=" + dailyAverage);

                        }
                    }

                    // RecyclerView에 어댑터 설정
                    usageAdapter = new ToiletUsageAdapter(toiletUsageList);
                    toiletView.setAdapter(usageAdapter);

                } else {
                    Log.d("ToiletActivity", "No data found for week " + week);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("ToiletActivity", "Failed to read value.", error.toException());
            }
        });
    }

    private void fetchToiletMonthlyData(String year, String month) {
        // Firebase 경로 설정
        DatabaseReference path = databaseReference.child(year).child(month);

        Log.d("ToiletActivity", "Fetching data from path: " + path.toString());

        path.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<ToiletUsage> toiletUsageList = new ArrayList<>();

                    for (DataSnapshot weekSnapshot : snapshot.getChildren()) {
                        String week = weekSnapshot.getKey();  // "week1", "week2", ...
                        Double weeklyAverage = weekSnapshot.child("weeklyAvg").getValue(Double.class);
                        Integer weeklyCount = weekSnapshot.child("weeklyCount").getValue(int.class);

                        if (week != null && weeklyAverage != null && weeklyCount != null) {
                            // ToiletUsage 객체 생성 후 리스트에 추가
                            toiletUsageList.add(new ToiletUsage(week, weeklyCount, weeklyAverage));
                            Log.d("ToiletActivity", "Added: Week=" + week + ", Count=" + weeklyCount + ", Avg=" + weeklyAverage);
                        }
                    }

                    // RecyclerView에 어댑터 설정
                    usageAdapter = new ToiletUsageAdapter(toiletUsageList);
                    toiletView.setAdapter(usageAdapter);

                } else {
                    Log.d("ToiletActivity", "No data found for month " + month);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("ToiletActivity", "Failed to read value.", error.toException());
            }
        });
    }

    private void fetchToiletYearlyData(String year) {
        // Firebase 경로 설정
        DatabaseReference path = databaseReference.child(year);
        Log.d("ToiletActivity", "Fetching data from path: " + path.toString());

        path.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<ToiletUsage> toiletUsageList = new ArrayList<>();

                    for (int i = 1; i <= 12; i++) {
                        // 월을 두 자리로 포맷팅
                        String monthKey = String.format(Locale.getDefault(), "%02d", i);

                        DataSnapshot monthSnapshot = snapshot.child(monthKey);
                        String month = monthSnapshot.getKey();
                        Double monthlyAvg = monthSnapshot.child("monthlyAvg").getValue(Double.class);
                        Integer monthlyCount = monthSnapshot.child("monthlyCount").getValue(Integer.class);

                        if (monthlyAvg != null && monthlyCount != null) {
                            // ToiletUsage 객체 생성 후 리스트에 추가
                            toiletUsageList.add(new ToiletUsage(monthlyCount, monthlyAvg, month));
                            Log.d("ToiletActivity", "Added: month=" + month + ", Count=" + monthlyCount + ", Avg=" + monthlyAvg);
                        }
                    }

                    // RecyclerView에 어댑터 설정
                    usageAdapter = new ToiletUsageAdapter(toiletUsageList);
                    toiletView.setAdapter(usageAdapter);

                } else {
                    Log.d("ToiletActivity", "No data found for year " + year);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("ToiletActivity", "Failed to read value.", error.toException());
            }
        });
    }

    // 주별 데이터 로드
    private void loadWeeklyData() {

        String currentYear = String.valueOf(currentWeek.get(Calendar.YEAR));
        String currentMonth = String.format(Locale.getDefault(), "%02d", currentWeek.get(Calendar.MONTH) + 1);
        String weekKey = "week" + currentWeek.get(Calendar.WEEK_OF_YEAR);

        String firebasePath = String.format("USER/StatsData/toiletUsage/%s/%s/%s", currentYear, currentMonth, weekKey);
        loadData(firebasePath);  // 주별 경로로 데이터 로드
        fetchToiletWeeklyData(currentWeek.getTime());
    }

    // 월별 데이터 로드
    private void loadMonthlyData() {
        // 현재 연도와 월 가져오기
        String currentYear = String.valueOf(currentWeek.get(Calendar.YEAR));
        String currentMonth = String.format(Locale.getDefault(), "%02d", currentWeek.get(Calendar.MONTH) + 1);


        // fetchToiletMonthlyData에 연도와 월 전달
        fetchToiletMonthlyData(currentYear, currentMonth);
    }

    // 연별 데이터 로드
    private void loadYearlyData() {
        String currentYear = String.valueOf(currentWeek.get(Calendar.YEAR));

        String firebasePath = String.format("USER/StatsData/toiletUsage/%s", currentYear);
        Log.d("ToiletActivity", "Fetching data from path: " + firebasePath);
        fetchToiletYearlyData(currentYear);  // 연별 경로로 데이터 로드
    }


    // 데이터 로드
    private void loadData(String firebasePath) {
        DatabaseReference toiletUsageRef = FirebaseDatabase.getInstance().getReference(firebasePath);

        toiletUsageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usageList.clear();  // 기존 리스트 비우기
                if (snapshot.exists()) {
                    for (DataSnapshot usageSnapshot : snapshot.getChildren()) {
                        // 여기서 데이터 읽어오기
                        String count = usageSnapshot.getKey();
                        Integer duration = usageSnapshot.child("duration").getValue(Integer.class);
                        String startTime = usageSnapshot.child("startTime").getValue(String.class);
                        String endTime = usageSnapshot.child("endTime").getValue(String.class);

                        if (duration != null && startTime != null && endTime != null) {
                            int durationInMinutes = duration / 60;  // 초 -> 분 변환
                            usageList.add(new ToiletUsage(count, startTime, endTime,durationInMinutes));
                        }
                    }
                    usageAdapter.notifyDataSetChanged(); // RecyclerView 갱신
                } else {
                    Log.d("ToiletActivity", "No data found");
                    //usageCountTextView.setText("데이터 없음");
                    usageAdapter.notifyDataSetChanged(); // RecyclerView 갱신
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("ToiletActivity", "Failed to read value.", error.toException());
            }
        });
    }

}