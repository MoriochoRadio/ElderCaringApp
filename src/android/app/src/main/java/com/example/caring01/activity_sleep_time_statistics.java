package com.example.caring01;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class activity_sleep_time_statistics extends AppCompatActivity {

    private GraphView sleepTimeGraph;
    private TextView dateTextView;
    private Button prevButton, nextButton;
    private Calendar currentStartDate; // 현재 표시 중인 주의 시작 날짜
    private final int maxWeeks = 5; // 기록 가능한 최대 주 수 (예시로 설정)
    private DatabaseReference databaseReference;
    private Date currentDate;
    private String currentTab = "daily"; // 기본값을 "daily"로 설정


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_time_statistics);

        sleepTimeGraph = findViewById(R.id.sleepTimeGraph);
        dateTextView = findViewById(R.id.dateTextView);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);

        findViewById(R.id.tab_daily).setOnClickListener(v -> {
            currentTab = "daily";
            updateDateTextView();
            displaySleepDataForCurrentWeek(currentDate);

            // sleepstats를 보이도록 설정
            findViewById(R.id.sleepstats).setVisibility(View.INVISIBLE);
        });

        findViewById(R.id.tab_weekly).setOnClickListener(v -> {
            currentTab = "weekly";
            updateDateTextView();
            fetchWeeklyData(currentDate);

            // sleepstats를 보이도록 설정
            findViewById(R.id.sleepstats).setVisibility(View.VISIBLE);
            findViewById(R.id.daystats).setVisibility(View.INVISIBLE);
        });

        findViewById(R.id.tab_monthly).setOnClickListener(v -> {
            currentTab = "monthly";
            updateDateTextView();
            fetchMonthlyData(currentDate);
            // sleepstats를 보이도록 설정
            findViewById(R.id.sleepstats).setVisibility(View.VISIBLE);
            findViewById(R.id.daystats).setVisibility(View.INVISIBLE);
        });

        findViewById(R.id.tab_yearly).setOnClickListener(v -> {
            currentTab = "yearly";
            updateDateTextView();
            fetchYearlyData(currentDate);
            // sleepstats를 보이도록 설정
            findViewById(R.id.sleepstats).setVisibility(View.INVISIBLE);
            findViewById(R.id.daystats).setVisibility(View.INVISIBLE);
        });

        // Firebase Database Reference 초기화
        databaseReference = FirebaseDatabase.getInstance().getReference();

        currentDate = new Date();
        // 현재 날짜 기준으로 시작
        currentStartDate = Calendar.getInstance();
        setToStartOfWeek(currentStartDate);
        updateWeekTextView();
        displaySleepDataForCurrentWeek(currentDate);

        findViewById(R.id.prevButton).setOnClickListener(v -> {
            updateDate(-1);
            fetchDataForCurrentTab();
        });

        findViewById(R.id.nextButton).setOnClickListener(v -> {
            updateDate(1);
            fetchDataForCurrentTab();
        });
    }

    private void fetchDataForCurrentTab() {
        switch (currentTab) {
            case "daily":
                displaySleepDataForCurrentWeek(currentDate);
                break;
            case "weekly":
                fetchWeeklyData(currentDate);
                break;
            case "monthly":
                fetchMonthlyData(currentDate);
                break;
            case "yearly":
                fetchYearlyData(currentDate);
                break;
        }
    }

    //오늘 데이터 fetch 함수임
    private void displaySleepDataForCurrentWeek(Date date) {
        sleepTimeGraph.removeAllSeries(); // 기존 데이터를 제거하여 초기화

        // 년도, 월, 날짜 포맷 설정
        SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat sdfMonth = new SimpleDateFormat("M", Locale.getDefault());
        SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String year = sdfYear.format(date);
        String month = sdfMonth.format(date);
        String formattedDate = sdfDay.format(date);

        // 주차 계산
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int weekOfMonth = (dayOfMonth - 1) / 7 + 1;

        String week = "week" + weekOfMonth;

        // Firebase 경로 생성
        DatabaseReference path = databaseReference.child("USER")
                .child("StatsData")
                .child("sleepData")
                .child(year)
                .child(month)
                .child(week)
                .child(formattedDate);

        // 데이터 요청
        path.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d("SleepTimeActivity", "Data found: " + dataSnapshot.getValue());

                    // 수면 데이터 업데이트
                    updateChartWithData(dataSnapshot);
                } else {
                    Log.d("SleepTimeActivity", "No data found.");
                    clearChart(); // 데이터가 없을 경우 그래프 초기화
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("SleepTimeActivity", "데이터 로드 실패: " + databaseError.getMessage());
            }
        });
    }

    //오늘 데이터 차트에 표시하는 함수
    private void updateChartWithData(DataSnapshot dataSnapshot) {
        BarGraphSeries<DataPoint> sleepTimeSeries = new BarGraphSeries<>();

        try {
            // Daily sleep duration
            Double dailySleepDuration = dataSnapshot.child("Daily sleep duration").getValue(Double.class);
            if (dailySleepDuration != null) {
                // 막대 그래프에 데이터 추가
                sleepTimeSeries.appendData(new DataPoint(1, dailySleepDuration), true, 1);
            }

            // start_time
            String startTime = dataSnapshot.child("start_time").getValue(String.class);
            if (startTime != null) {
                TextView startSleepView = findViewById(R.id.startsleep);
                startSleepView.setText("취침 시간: " + startTime);
            }

            // end_time
            String endTime = dataSnapshot.child("end_time").getValue(String.class);
            if (endTime != null) {
                TextView endSleepView = findViewById(R.id.endsleep);
                endSleepView.setText("기상 시간: " + endTime);
            }

            // Daily restless count
            Integer restlessCount = dataSnapshot.child("Daily restless count").getValue(Integer.class);
            if (restlessCount != null) {
                TextView tossTurnView = findViewById(R.id.tossturn);
                tossTurnView.setText("뒤척임 횟수: " + restlessCount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 그래프에 시리즈 추가
        sleepTimeGraph.addSeries(sleepTimeSeries);

        // BarGraphSeries 스타일 설정
        sleepTimeSeries.setSpacing(50); // 막대 간격 조정
        sleepTimeSeries.setDrawValuesOnTop(true);  // 각 막대 위에 값 표시
        sleepTimeSeries.setValuesOnTopColor(Color.BLACK);  // 값 색상

        // X축과 Y축 설정
        sleepTimeGraph.getGridLabelRenderer().setHorizontalAxisTitle("일");
        sleepTimeGraph.getGridLabelRenderer().setVerticalAxisTitle("수면 시간");

        // X축 범위 고정
        sleepTimeGraph.getViewport().setXAxisBoundsManual(true);
        sleepTimeGraph.getViewport().setMinX(0);
        sleepTimeGraph.getViewport().setMaxX(2); // 하나의 막대기를 보여줄 범위 설정

        // Y축 범위 설정
        sleepTimeGraph.getViewport().setYAxisBoundsManual(true);
        sleepTimeGraph.getViewport().setMinY(0);
        sleepTimeGraph.getViewport().setMaxY(12); // 최대 수면시간 12시간
    }


    private void fetchWeeklyData(Date date) {
        // 년도와 월 포맷 설정
        SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat sdfMonth = new SimpleDateFormat("M", Locale.getDefault());
        String year = sdfYear.format(date);
        String month = sdfMonth.format(date);

        // 주차 계산
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH);
        String week = "week" + weekOfMonth;

        // Firebase 경로 생성
        DatabaseReference path = databaseReference.child("USER")
                .child("StatsData")
                .child("sleepData")
                .child(year)
                .child(month)
                .child(week);

        // 데이터 요청
        path.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    updateWeeklyChartWithData(dataSnapshot);

                    // 평균, 최대, 최소 값 업데이트
                    Integer weeklyAvg = dataSnapshot.child("Weekly average").getValue(Integer.class);
                    Integer weeklyMax = dataSnapshot.child("Weekly max").getValue(Integer.class);
                    Integer weeklyMin = dataSnapshot.child("Weekly min").getValue(Integer.class);

                    if (weeklyAvg != null) {
                        TextView avgSleepView = findViewById(R.id.avgSleep);
                        avgSleepView.setText(weeklyAvg + "시간");
                    }

                    if (weeklyMax != null) {
                        TextView maxSleepView = findViewById(R.id.maxSleep);
                        maxSleepView.setText(weeklyMax + "시간");
                    }

                    if (weeklyMin != null) {
                        TextView minSleepView = findViewById(R.id.minSleep);
                        minSleepView.setText(weeklyMin + "시간");
                    }
                } else {
                    clearChart();
                    clearStats();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("HeartRateActivity", "주간 데이터 로드 실패: " + databaseError.getMessage());
            }
        });
    }

    private void updateWeeklyChartWithData(DataSnapshot dataSnapshot) {
        // 새로운 BarGraphSeries 생성
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>();
        int dayIndex = 1;  // X축 인덱스 1부터 시작

        // 7일 데이터 순차적으로 처리
        for (DataSnapshot daySnapshot : dataSnapshot.getChildren()) {
            // Daily average 값 가져오기
            Integer dailyAverage = daySnapshot.child("Daily sleep duration").getValue(Integer.class);
            if (dailyAverage != null) {
                Log.d("sleepTimeActivity", "Day " + dayIndex + " - Daily average: " + dailyAverage);

                // X축 인덱스를 기준으로 데이터 포인트 추가
                series.appendData(new DataPoint(dayIndex, dailyAverage), true, 7);

                // 인덱스 증가
                dayIndex++;
            }
        }

        // 그래프에 시리즈 추가
        sleepTimeGraph.removeAllSeries();
        sleepTimeGraph.addSeries(series);

        // BarGraphSeries 스타일 설정
        series.setSpacing(20); // 막대 간격 조정
        series.setDrawValuesOnTop(true);  // 각 막대 위에 값 표시
        series.setValuesOnTopColor(Color.BLACK);  // 값 색상

        // X축과 Y축 레이블 설정
        sleepTimeGraph.getGridLabelRenderer().setHorizontalAxisTitle("일");
        sleepTimeGraph.getGridLabelRenderer().setVerticalAxisTitle("일별 수면 시간");

        // X축을 7일로 설정
        sleepTimeGraph.getViewport().setXAxisBoundsManual(true);
        sleepTimeGraph.getViewport().setMinX(1);
        sleepTimeGraph.getViewport().setMaxX(7);  // 7일로 설정

        // X축에 여백 추가 (양쪽 막대가 잘리지 않도록 설정)
        sleepTimeGraph.getViewport().setXAxisBoundsManual(true);
        sleepTimeGraph.getViewport().setMinX(series.getLowestValueX() - 1); // X축의 시작을 약간 이전으로 설정
        sleepTimeGraph.getViewport().setMaxX(series.getHighestValueX() + 1); // X축의 끝을 약간 이후로 설정


        // Y축 범위 설정 (최대값, 최소값)
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        int total = 0;
        int count = 0;

        // 통계 계산
        for (DataSnapshot daySnapshot : dataSnapshot.getChildren()) {
            Integer dailyAverage = daySnapshot.child("Daily average").getValue(Integer.class);
            if (dailyAverage != null) {
                max = Math.max(max, dailyAverage);
                min = Math.min(min, dailyAverage);
                total += dailyAverage;
                count++;
            }
        }

        if (count > 0) {
            sleepTimeGraph.getViewport().setYAxisBoundsManual(true);
            sleepTimeGraph.getViewport().setMinY(min - 10);
            sleepTimeGraph.getViewport().setMaxY(max + 10);
            // 통계 업데이트

        } else {

        }

        // 확대/축소 및 스크롤 비활성화
        sleepTimeGraph.getViewport().setScrollable(false);
        sleepTimeGraph.getViewport().setScalable(false);
    }

    private void fetchMonthlyData(Date date) {
        // 년도와 월 포맷 설정
        SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat sdfMonth = new SimpleDateFormat("M", Locale.getDefault());
        String year = sdfYear.format(date);
        String month = sdfMonth.format(date);

        // Firebase 경로 생성
        DatabaseReference path = databaseReference.child("USER")
                .child("StatsData")
                .child("sleepData")
                .child(year)
                .child(month);

        // 데이터 요청
        path.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    updateMonthlyBarChartWithData(dataSnapshot);

                    // 평균, 최대, 최소 값 업데이트
                    Integer monthlyAvg = dataSnapshot.child("Monthly average").getValue(Integer.class);
                    Integer monthlyMax = dataSnapshot.child("Monthly max").getValue(Integer.class);
                    Integer monthlyMin = dataSnapshot.child("Monthly min").getValue(Integer.class);

                    if (monthlyAvg != null) {
                        TextView avgSleepView = findViewById(R.id.avgSleep);
                        avgSleepView.setText(monthlyAvg + "시간");
                    }

                    if (monthlyMax != null) {
                        TextView maxSleepView = findViewById(R.id.maxSleep);
                        maxSleepView.setText(monthlyMax + "시간");
                    }

                    if (monthlyMin != null) {
                        TextView minSleepView = findViewById(R.id.minSleep);
                        minSleepView.setText(monthlyMin + "시간");
                    }
                } else {
                    clearChart();
                    clearStats();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("sleepTimeActivity", "월간 데이터 로드 실패: " + databaseError.getMessage());
            }
        });
    }

    private void updateMonthlyBarChartWithData(DataSnapshot dataSnapshot) {
        // 새로운 BarGraphSeries 생성
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>();
        int weekIndex = 1;

        // 데이터 추가
        for (DataSnapshot daySnapshot : dataSnapshot.getChildren()) {
            // Weekly average 값 가져오기
            Integer weeklyAverage = daySnapshot.child("Weekly average").getValue(Integer.class);
            if (weeklyAverage != null) {
                Log.d("sleepTimeActivity", "week " + weekIndex + " - Weekly average: " + weeklyAverage);

                // X축 인덱스를 기준으로 데이터 포인트 추가
                series.appendData(new DataPoint(weekIndex, weeklyAverage), true, 5);

                // 인덱스 증가
                weekIndex++;
            }
        }

        // 그래프에 시리즈 추가
        sleepTimeGraph.removeAllSeries();
        sleepTimeGraph.addSeries(series);

        // BarGraphSeries 스타일 설정
        series.setSpacing(20); // 막대 간격 조정
        series.setDrawValuesOnTop(true);  // 각 막대 위에 값 표시
        series.setValuesOnTopColor(Color.BLACK);  // 값 색상

        // X축과 Y축 레이블 설정
        sleepTimeGraph.getGridLabelRenderer().setHorizontalAxisTitle("주");
        sleepTimeGraph.getGridLabelRenderer().setVerticalAxisTitle("주 평균 (시간)");

        // X축 범위 설정 (1 ~ 4)
        sleepTimeGraph.getViewport().setXAxisBoundsManual(true);
        sleepTimeGraph.getViewport().setMinX(1);
        sleepTimeGraph.getViewport().setMaxX(5);  // 5주로 설정

        // X축에 여백 추가 (양쪽 막대가 잘리지 않도록 설정)
        sleepTimeGraph.getViewport().setXAxisBoundsManual(true);
        sleepTimeGraph.getViewport().setMinX(series.getLowestValueX() - 1); // X축의 시작을 약간 이전으로 설정
        sleepTimeGraph.getViewport().setMaxX(series.getHighestValueX() + 1); // X축의 끝을 약간 이후로 설정

        // X축 레이블 수동 설정 (1, 2, 3, 4)
        sleepTimeGraph.getGridLabelRenderer().setNumHorizontalLabels(4);  // 레이블 갯수 설정
        sleepTimeGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    return String.valueOf((int) value);  // 1, 2, 3, 4로 표시
                } else {
                    return super.formatLabel(value, isValueX);
                }
            }
        });

        // Y축 범위 설정 (최대값, 최소값)
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        int total = 0;
        int count = 0;

        // 통계 계산
        for (DataSnapshot daySnapshot : dataSnapshot.getChildren()) {
            Integer weeklyAverage = daySnapshot.child("Weekly average").getValue(Integer.class);
            if (weeklyAverage != null) {
                max = Math.max(max, weeklyAverage);
                min = Math.min(min, weeklyAverage);
                total += weeklyAverage;
                count++;
            }
        }

        if (count > 0) {
            sleepTimeGraph.getViewport().setYAxisBoundsManual(true);
            sleepTimeGraph.getViewport().setMinY(min - 10);
            sleepTimeGraph.getViewport().setMaxY(max + 10);
            // 통계 업데이트
        } else {
        }

        // 확대/축소 및 스크롤 비활성화
        sleepTimeGraph.getViewport().setScrollable(false);
        sleepTimeGraph.getViewport().setScalable(false);
    }




    // 연간 데이터 가져오기
    private void fetchYearlyData(Date date) {
        // 년도 포맷 설정
        SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
        String year = sdfYear.format(date);

        // Firebase 경로 생성
        DatabaseReference path = databaseReference.child("USER")
                .child("StatsData")
                .child("heartRate")
                .child(year);

        // 데이터 요청
        path.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    updateYearlyBarChartWithData(dataSnapshot);

                    // 평균, 최대, 최소 값 업데이트
                    Integer yearlyAvg = dataSnapshot.child("Yearly average").getValue(Integer.class);
                    Integer yearlyMax = dataSnapshot.child("Yearly max").getValue(Integer.class);
                    Integer yearlyMin = dataSnapshot.child("Yearly min").getValue(Integer.class);

                    if (yearlyAvg != null) {
                        TextView avgSleepView = findViewById(R.id.avgSleep);
                        avgSleepView.setText(yearlyAvg + "시간");
                    }

                    if (yearlyMax != null) {
                        TextView maxSleepView = findViewById(R.id.maxSleep);
                        maxSleepView.setText(yearlyMax + "시간");
                    }

                    if (yearlyMin != null) {
                        TextView minSleepView = findViewById(R.id.minSleep);
                        minSleepView.setText(yearlyMin + "시간");
                    }
                } else {
                    clearChart();
                    clearStats();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("sleepTimeActivity", "연간 데이터 로드 실패: " + databaseError.getMessage());
            }
        });
    }


    private void updateYearlyBarChartWithData(DataSnapshot dataSnapshot) {
        // 새로운 BarGraphSeries 생성
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>();
        int monthIndex = 1;

        // 월별 데이터만 가져오기 (1부터 12까지 확인, 해당 월에 데이터가 있는 경우만 X축에 추가)
        List<Integer> monthsWithData = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            DataSnapshot monthSnapshot = dataSnapshot.child(String.valueOf(i)); // 1, 2, ..., 12월
            if (monthSnapshot.exists()) {
                Integer monthlyAverage = monthSnapshot.child("Monthly average").getValue(Integer.class);
                if (monthlyAverage != null) {
                    // 데이터가 존재하면 해당 월을 X축에 추가
                    series.appendData(new DataPoint(i, monthlyAverage), true, 12);
                    monthsWithData.add(i); // 데이터가 있는 월을 저장
                }
            }
        }

        // 그래프에 시리즈 추가
        sleepTimeGraph.removeAllSeries();
        sleepTimeGraph.addSeries(series);

        // BarGraphSeries 스타일 설정
        series.setSpacing(20); // 막대 간격 조정
        series.setDrawValuesOnTop(true);  // 각 막대 위에 값 표시
        series.setValuesOnTopColor(Color.BLACK);  // 값 색상

        // X축과 Y축 레이블 설정
        sleepTimeGraph.getGridLabelRenderer().setHorizontalAxisTitle("월");
        sleepTimeGraph.getGridLabelRenderer().setVerticalAxisTitle("월 평균 (시간)");

        // X축 범위 설정: 실제로 데이터가 있는 월만 X축에 표시
        sleepTimeGraph.getViewport().setXAxisBoundsManual(true);
        sleepTimeGraph.getViewport().setMinX(monthsWithData.get(0));
        sleepTimeGraph.getViewport().setMaxX(monthsWithData.get(monthsWithData.size() - 1));

        // X축에 여백 추가 (양쪽 막대가 잘리지 않도록 설정)
        sleepTimeGraph.getViewport().setXAxisBoundsManual(true);
        sleepTimeGraph.getViewport().setMinX(series.getLowestValueX() - 1); // X축의 시작을 약간 이전으로 설정
        sleepTimeGraph.getViewport().setMaxX(series.getHighestValueX() + 1); // X축의 끝을 약간 이후로 설정

        // Y축 범위 설정 (최대값, 최소값)
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        int total = 0;
        int count = 0;

        // 통계 계산
        for (int i = 1; i <= 12; i++) {
            DataSnapshot monthSnapshot = dataSnapshot.child(String.valueOf(i)); // 1, 2, ..., 12월
            if (monthSnapshot.exists()) {
                Integer monthlyAverage = monthSnapshot.child("Monthly average").getValue(Integer.class);
                if (monthlyAverage != null) {
                    max = Math.max(max, monthlyAverage);
                    min = Math.min(min, monthlyAverage);
                    total += monthlyAverage;
                    count++;
                }
            }
        }

        if (count > 0) {
            sleepTimeGraph.getViewport().setYAxisBoundsManual(true);
            sleepTimeGraph.getViewport().setMinY(min - 10);
            sleepTimeGraph.getViewport().setMaxY(max + 10);
            // 통계 업데이트

        } else {
        }

        // 확대/축소 및 스크롤 비활성화
        sleepTimeGraph.getViewport().setScrollable(false);
        sleepTimeGraph.getViewport().setScalable(false);
    }

    // 주 텍스트 업데이트
    private void updateWeekTextView() {
        int month = currentStartDate.get(Calendar.MONTH) + 1;
        int startDay = currentStartDate.get(Calendar.DAY_OF_MONTH);
        Calendar endDate = (Calendar) currentStartDate.clone();
        endDate.add(Calendar.DAY_OF_MONTH, 6);
        int endDay = endDate.get(Calendar.DAY_OF_MONTH);

        dateTextView.setText(String.format("%d월 %d일 - %d일", month, startDay, endDay));
    }

    // 특정 날짜로 주의 시작 설정
    private void setToStartOfWeek(Calendar date) {
        date.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
    }

    // 데이터가 있는지 여부 확인 (가상의 조건)
    private boolean isDataAvailable(Calendar date) {
        // 예를 들어 최대 4주까지만 데이터가 있다고 가정
        Calendar currentDate = Calendar.getInstance();
        int weeksDifference = (int) ((currentDate.getTimeInMillis() - date.getTimeInMillis()) / (1000 * 60 * 60 * 24 * 7));
        return weeksDifference <= maxWeeks && weeksDifference >= 0;
    }

    // 날짜에서 일 인덱스 가져오기 (예: "2024-11-21" -> 1)
    private int getDayIndexFromDate(String date) {
        try {
            String[] parts = date.split("-");
            int day = Integer.parseInt(parts[2]);
            return day; // 1일, 2일, ...7일
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }

    private void updateDateTextView() {
        SimpleDateFormat sdf;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        switch (currentTab) {
            case "daily":
                sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                dateTextView.setText(sdf.format(currentDate));
                break;
            case "weekly":
                int weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH);
                sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                dateTextView.setText(sdf.format(currentDate) + " - Week " + weekOfMonth);
                break;
            case "monthly":
                sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                dateTextView.setText(sdf.format(currentDate));
                break;
            case "yearly":
                sdf = new SimpleDateFormat("yyyy", Locale.getDefault());
                dateTextView.setText(sdf.format(currentDate));
                break;
        }
    }

    private void updateDate(int delta) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        switch (currentTab) {
            case "daily":
                calendar.add(Calendar.DATE, delta);
                break;
            case "weekly":
                calendar.add(Calendar.WEEK_OF_YEAR, delta);
                break;
            case "monthly":
                calendar.add(Calendar.MONTH, delta);
                break;
            case "yearly":
                calendar.add(Calendar.YEAR, delta);
                break;
        }

        currentDate = calendar.getTime();
        updateDateTextView();
    }
    // 주 번호 계산 (예: week1, week2...)
    private int getWeekNumber(Calendar date) {
        int weekOfYear = date.get(Calendar.WEEK_OF_YEAR);
        return weekOfYear;
    }

    private void clearChart() {
        sleepTimeGraph.removeAllSeries();

    }
    private void clearStats() {
        TextView avgSleepView = findViewById(R.id.avgSleep);
        TextView maxSleepView = findViewById(R.id.maxSleep);
        TextView minSleepView = findViewById(R.id.minSleep);

        avgSleepView.setText("평균: --");
        maxSleepView.setText("최대: --");
        minSleepView.setText("최소: --");
    }

}
