package com.example.caring01;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class activity_movement_statistics extends AppCompatActivity {

    private GraphView walkGraph;
    private TextView dateTextView, txtWalk;
    private Button prevButton, nextButton;
    private Calendar currentStartDate; // 현재 표시 중인 주의 시작 날짜
    private final int maxWeeks = 5; // 기록 가능한 최대 주 수 (예시로 설정)
    private DatabaseReference databaseReference;
    private Date currentDate;
    private String currentTab = "daily"; // 기본값을 "daily"로 설정


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movement_statistics);

        walkGraph = findViewById(R.id.WalkGraph);
        txtWalk = findViewById(R.id.txtWalk);
        dateTextView = findViewById(R.id.dateTextView);

        prevButton = findViewById(R.id.prevWeekButton);
        nextButton = findViewById(R.id.nextWeekButton);

        databaseReference = FirebaseDatabase.getInstance().getReference("USER/StatsData/steps");
        currentStartDate = Calendar.getInstance();

        // 현재 날짜 초기화
        currentDate = new Date();
        updateWeekTextView();


        findViewById(R.id.tab_daily).setOnClickListener(v -> {
            currentTab = "daily";
            updateWeekTextView();
            fetchMovementData(currentDate);
        });

        findViewById(R.id.tab_weekly).setOnClickListener(v -> {
            currentTab = "weekly";
            updateWeekTextView();
            fetchWeeklyData(currentDate);
        });

        findViewById(R.id.tab_monthly).setOnClickListener(v -> {
            currentTab = "monthly";
            updateWeekTextView();
            fetchMonthlyData(currentDate);
        });

        findViewById(R.id.tab_yearly).setOnClickListener(v -> {
            currentTab = "yearly";
            updateWeekTextView();
            fetchYearlyData(currentDate);
        });

        // Firebase Database Reference 초기화
        databaseReference = FirebaseDatabase.getInstance().getReference();

        currentDate = new Date();
        // 현재 날짜 기준으로 시작
        currentStartDate = Calendar.getInstance();
        setToStartOfWeek(currentStartDate);
        updateWeekTextView();
        fetchWeeklyData(currentDate);

        findViewById(R.id.prevWeekButton).setOnClickListener(v -> {
            updateDate(-1);
            fetchDataForCurrentTab();
        });

        findViewById(R.id.nextWeekButton).setOnClickListener(v -> {
            updateDate(1);
            fetchDataForCurrentTab();
        });
    }


    private void fetchDataForCurrentTab() {
        switch (currentTab) {
            case "daily":
                fetchMovementData(currentDate);
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

    private DatabaseReference getFirebasePathForDate(String year, String month, String week, String day) {
        DatabaseReference path = databaseReference.child("USER").child("StatsData").child("steps").child(year);
        if (month != null) path = path.child(month);
        if (week != null) path = path.child(week);
        if (day != null) path = path.child(day);
        return path;
    }

    private void fetchMovementData(Date date) {
        // 날짜 포맷 설정
        SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat sdfMonth = new SimpleDateFormat("M", Locale.getDefault());
        SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String year = sdfYear.format(date);
        String month = sdfMonth.format(date);
        String formattedDate = sdfDay.format(date);

        // 주차 계산(1~7일, 8~14일 등)
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int weekOfMonth = (dayOfMonth - 1) / 7 + 1;  // 주차 계산

        String week = "week" + weekOfMonth;

        // Firebase 경로 생성
        DatabaseReference path = databaseReference.child("USER")
                .child("StatsData")
                .child("steps")
                .child(year)
                .child(month)
                .child(week)
                .child(formattedDate);

        // 데이터 요청
        path.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d("MovementActivity", "Data found: " + dataSnapshot.getValue());
                    updateDayChartWithData(dataSnapshot);
                } else {
                    Log.d("MovementActivity", "No data found.");
                    clearChart();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MovementActivity", "데이터 로드 실패: " + databaseError.getMessage());
            }
        });
    }

    private void fetchWeeklyData(Date date) {
        SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat sdfMonth = new SimpleDateFormat("M", Locale.getDefault());
        String year = sdfYear.format(date);
        String month = sdfMonth.format(date);
        String week = "week" + getWeekOfMonth(date);

        DatabaseReference path = getFirebasePathForDate(year, month, week, null);
        path.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    updateWeeklyChartWithData(dataSnapshot);
                } else {
                    Log.d("MovementActivity", "No weekly data found.");
                    clearChart();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MovementActivity", "Failed to load weekly data: " + databaseError.getMessage());
            }
        });
    }


    private void updateWeeklyChartWithData(DataSnapshot dataSnapshot) {
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>();
        int dayIndex = 0;
        int totalSteps = 0;
        for (DataSnapshot daySnapshot : dataSnapshot.getChildren()) {
            Integer steps = daySnapshot.child("Daily steps").getValue(Integer.class);
            if (steps != null) {
                series.appendData(new DataPoint(dayIndex, steps), true, 7);
                totalSteps += steps;  // 총 걸음 수 계산
                dayIndex++;
            }
        }
        walkGraph.removeAllSeries();
        walkGraph.addSeries(series);

        // 걸음 수 텍스트뷰 업데이트
        txtWalk.setText("" + totalSteps);

        // BarGraphSeries 스타일 설정
        series.setSpacing(50);  // 막대 간격 조정
        series.setDrawValuesOnTop(true);  // 각 막대 위에 값 표시
        series.setValuesOnTopColor(Color.BLACK);  // 값 색상

        // X축 및 Y축 설정
        walkGraph.getGridLabelRenderer().setHorizontalAxisTitle("일");
        walkGraph.getGridLabelRenderer().setVerticalAxisTitle("일 걸음수");

        // X축을 0부터 6까지 설정 (일주일 동안의 데이터)
        walkGraph.getViewport().setXAxisBoundsManual(true);
        walkGraph.getViewport().setMinX(0);
        walkGraph.getViewport().setMaxX(6);

        // X축에 여백 추가 (양쪽 막대가 잘리지 않도록 설정)
        walkGraph.getViewport().setXAxisBoundsManual(true);
        walkGraph.getViewport().setMinX(series.getLowestValueX() - 0.5); // X축의 시작을 약간 이전으로 설정
        walkGraph.getViewport().setMaxX(series.getHighestValueX() + 0.5); // X축의 끝을 약간 이후로 설정

        // Y축 범위 설정 (자동으로 설정하거나 데이터를 바탕으로 설정 가능)
        walkGraph.getViewport().setYAxisBoundsManual(true);
        walkGraph.getViewport().setMinY(0);
        walkGraph.getViewport().setMaxY(series.getHighestValueY() + 500); // 여유를 두어 최대값 설정

        // 확대/축소 및 스크롤 비활성화
        walkGraph.getViewport().setScrollable(false);
        walkGraph.getViewport().setScalable(false);
    }


    private void updateDayChartWithData(DataSnapshot dataSnapshot) {
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>();
        int total = 0, count = 0;
        Integer dailySteps = dataSnapshot.child("Daily steps").getValue(Integer.class);

        int maxHourlySteps = Integer.MIN_VALUE;
        int minHourlySteps = Integer.MAX_VALUE;

        // 시간별 데이터 처리
        for (DataSnapshot hourSnapshot : dataSnapshot.getChildren()) {
            try {
                String key = hourSnapshot.getKey();
                if (key == null) {
                    Log.e("MovementActivity", "Hour key is null");
                    continue;
                }

                int hour = Integer.parseInt(key); // Key를 시간으로 변환
                Integer hourlySteps = hourSnapshot.getValue(Integer.class);

                if (hourlySteps == null) {
                    Log.e("MovementActivity", "Hourly steps is null for hour: " + hour);
                    continue;
                }

                // 데이터 포인트 추가 (Hourly steps)
                series.appendData(new DataPoint(hour, hourlySteps), true, 24);

                // 통계 계산
                total += hourlySteps;
                count++;

                // 최대, 최소값 계산
                maxHourlySteps = Math.max(maxHourlySteps, hourlySteps);
                minHourlySteps = Math.min(minHourlySteps, hourlySteps);
            } catch (NumberFormatException e) {
                Log.e("MovementActivity", "Invalid hour format: " + hourSnapshot.getKey(), e);
            } catch (Exception e) {
                Log.e("MovementActivity", "Unexpected error while processing data", e);
            }
        }

        // 그래프에 시리즈 추가
        walkGraph.removeAllSeries();
        walkGraph.addSeries(series);

        // BarGraphSeries 스타일 설정
        series.setSpacing(20);  // 막대 간격 조정
        series.setDrawValuesOnTop(true);  // 각 막대 위에 값 표시
        series.setValuesOnTopColor(Color.BLACK);  // 값 색상

        // Daily steps도 표시
        if (dailySteps != null) {
            txtWalk.setText("" + dailySteps);
        } else {
            txtWalk.setText("No data for today.");
        }

        // X축 및 Y축 설정
        walkGraph.getGridLabelRenderer().setHorizontalAxisTitle("시간");
        walkGraph.getGridLabelRenderer().setVerticalAxisTitle("시간별 걸음수");

        // X축을 0시부터 23시까지 설정
        walkGraph.getViewport().setXAxisBoundsManual(true);
        walkGraph.getViewport().setMinX(0);
        walkGraph.getViewport().setMaxX(23);

        // X축에 여백 추가 (양쪽 막대가 잘리지 않도록 설정)
        walkGraph.getViewport().setXAxisBoundsManual(true);
        walkGraph.getViewport().setMinX(series.getLowestValueX() - 1); // X축의 시작을 약간 이전으로 설정
        walkGraph.getViewport().setMaxX(series.getHighestValueX() + 1); // X축의 끝을 약간 이후로 설정

        // Y축 범위 설정 (최대값, 최소값을 기반으로 설정)
        walkGraph.getViewport().setYAxisBoundsManual(true);
        walkGraph.getViewport().setMinY(0);
        walkGraph.getViewport().setMaxY(maxHourlySteps + 30); // 최대값에 여유를 두어 설정

        // 확대/축소 및 스크롤 비활성화
        walkGraph.getViewport().setScrollable(false);
        walkGraph.getViewport().setScalable(false);
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
                .child("steps")
                .child(year)
                .child(month);

        // 데이터 요청
        path.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    updateMonthlyBarChartWithData(dataSnapshot);
                } else {
                    clearChart();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MovementActivity", "월간 데이터 로드 실패: " + databaseError.getMessage());
            }
        });
    }

    private void updateMonthlyBarChartWithData(DataSnapshot dataSnapshot) {
        // 새로운 BarGraphSeries 생성
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>();
        int weekIndex = 1;
        int totalSteps = 0;
        // 데이터 추가
        for (DataSnapshot daySnapshot : dataSnapshot.getChildren()) {
            // Weekly average 값 가져오기
            Integer weeklyAverage = daySnapshot.child("Weekly steps").getValue(Integer.class);
            if (weeklyAverage != null) {
                Log.d("MovementActivity", "week " + weekIndex + " - Weekly steps: " + weeklyAverage);

                // X축 인덱스를 기준으로 데이터 포인트 추가
                series.appendData(new DataPoint(weekIndex, weeklyAverage), true, 5);

                totalSteps += weeklyAverage;  // 총 걸음 수 계산

                // 인덱스 증가
                weekIndex++;


            }
        }

        // 그래프에 시리즈 추가
        walkGraph.removeAllSeries();
        walkGraph.addSeries(series);

        // 걸음 수 텍스트뷰 업데이트
        txtWalk.setText(" " + totalSteps);

        // BarGraphSeries 스타일 설정
        series.setSpacing(80); // 막대 간격 조정
        series.setDrawValuesOnTop(true);  // 각 막대 위에 값 표시
        series.setValuesOnTopColor(Color.BLACK);  // 값 색상

        // X축과 Y축 레이블 설정
        walkGraph.getGridLabelRenderer().setHorizontalAxisTitle("주");
        walkGraph.getGridLabelRenderer().setVerticalAxisTitle("주 걸음수");

        // X축 범위 설정 (1 ~ 4)
        walkGraph.getViewport().setXAxisBoundsManual(true);
        walkGraph.getViewport().setMinX(1);
        walkGraph.getViewport().setMaxX(5);  // 5주로 설정

        // X축에 여백 추가 (양쪽 막대가 잘리지 않도록 설정)
        walkGraph.getViewport().setXAxisBoundsManual(true);
        walkGraph.getViewport().setMinX(series.getLowestValueX() - 0.5); // X축의 시작을 약간 이전으로 설정
        walkGraph.getViewport().setMaxX(series.getHighestValueX() + 0.5); // X축의 끝을 약간 이후로 설정

        // X축 레이블 수동 설정 (1, 2, 3, 4)
        walkGraph.getGridLabelRenderer().setNumHorizontalLabels(4);  // 레이블 갯수 설정
        walkGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
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
            Integer monthlyTotal = daySnapshot.child("Monthly steps").getValue(Integer.class);
            if (monthlyTotal != null) {
                max = Math.max(max, monthlyTotal);
                min = Math.min(min, monthlyTotal);
                total += monthlyTotal;
                count++;
            }
        }

        walkGraph.getViewport().setYAxisBoundsManual(true);

        if (count > 0) {
            walkGraph.getViewport().setMinY(Math.max(0, min - 10)); // 최소값이 0보다 작을 수 없도록 설정
            walkGraph.getViewport().setMaxY(max + 50); // 최대값에 여유를 더해 설정
        } else {
            // 데이터가 없는 경우 기본 범위를 설정합니다.
            walkGraph.getViewport().setMinY(0);
            walkGraph.getViewport().setMaxY(40000); // 기본 Y축 최대값 설정
        }

        // 확대/축소 및 스크롤 비활성화
        walkGraph.getViewport().setScrollable(false);
        walkGraph.getViewport().setScalable(false);
    }



    // 연간 데이터 가져오기
    // 연간 데이터 가져오기
    private void fetchYearlyData(Date date) {
        DatabaseReference path = databaseReference.child("USER").child("StatsData").child("steps");
        path.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    updateYearlyChartWithData(dataSnapshot);
                } else {
                    Log.d("MovementActivity", "No yearly data found.");
                    updateYearlyChartWithData(null); // 차트의 틀을 표시
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MovementActivity", "Failed to load yearly data: " + databaseError.getMessage());
            }
        });
    }

    private void updateYearlyChartWithData(DataSnapshot dataSnapshot) {
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>();
        int totalSteps = 0;

        // 그래프 초기화
        walkGraph.removeAllSeries();

        // 현재 선택된 연도 가져오기 (dateTextView 사용)
        String yearText = dateTextView.getText().toString();
        int selectedYear;
        try {
            selectedYear = Integer.parseInt(yearText);
        } catch (NumberFormatException e) {
            Log.e("MovementActivity", "Invalid year format in dateTextView: " + yearText, e);
            return;
        }

        // 선택된 연도의 월별 데이터 가져오기
        if (dataSnapshot != null) {
            DataSnapshot yearSnapshot = dataSnapshot.child(String.valueOf(selectedYear));
            if (yearSnapshot.exists()) {
                for (DataSnapshot monthSnapshot : yearSnapshot.getChildren()) {
                    String monthKey = monthSnapshot.getKey();
                    try {
                        int month = Integer.parseInt(monthKey);
                        Integer monthlyTotal = monthSnapshot.child("Monthly steps").getValue(Integer.class);
                        if (monthlyTotal != null) {
                            // 데이터가 존재하면 해당 월을 X축에 추가
                            series.appendData(new DataPoint(month, monthlyTotal), true, 12);
                            totalSteps += monthlyTotal;  // 총 걸음 수 계산
                        }
                    } catch (NumberFormatException e) {
                        Log.e("MovementActivity", "Invalid month format: " + monthKey, e);
                    }
                }
            } else {
                Log.d("MovementActivity", "No data found for the selected year: " + selectedYear);
            }
        }

        // 그래프에 시리즈 추가
        walkGraph.removeAllSeries();
        walkGraph.addSeries(series);

        // 걸음 수 텍스트뷰 업데이트
        txtWalk.setText("" + totalSteps);

        // BarGraphSeries 스타일 설정
        series.setSpacing(80); // 막대 간격 조정
        series.setDrawValuesOnTop(true);  // 각 막대 위에 값 표시
        series.setValuesOnTopColor(Color.BLACK);  // 값 색상

        // X축과 Y축 레이블 설정
        walkGraph.getGridLabelRenderer().setHorizontalAxisTitle("월");
        walkGraph.getGridLabelRenderer().setVerticalAxisTitle("월 걸음수");

        // X축에 여백 추가 (양쪽 막대가 잘리지 않도록 설정)
        if (series.getHighestValueX() > 0) {
            walkGraph.getViewport().setXAxisBoundsManual(true);
            walkGraph.getViewport().setMinX(series.getLowestValueX() - 0.5); // X축의 시작을 약간 이전으로 설정
            walkGraph.getViewport().setMaxX(series.getHighestValueX() + 0.5); // X축의 끝을 약간 이후로 설정
        }

        // Y축 범위 설정 (기본값 설정)
        walkGraph.getViewport().setYAxisBoundsManual(true);
        walkGraph.getViewport().setMinY(0);
        walkGraph.getViewport().setMaxY(600000); // 기본 Y축 최대값 설정 (적절히 변경 가능)

        // X축 레이블을 더 잘 구분하도록 설정
        walkGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    return String.valueOf((int) value) + "월"; // X축 값은 정수로 표시하고 '월' 추가
                } else {
                    return super.formatLabel(value, isValueX);
                }
            }
        });

        // 확대/축소 및 스크롤 비활성화
        walkGraph.getViewport().setScrollable(true);
        walkGraph.getViewport().setScalable(false);
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


    private void clearChart() {
        walkGraph.removeAllSeries();
        TextView noDataText = new TextView(this);
        noDataText.setText("No Data Available");
        noDataText.setTextColor(Color.GRAY);
        noDataText.setTextSize(16);
        noDataText.setGravity(Gravity.CENTER);

    }

    private int getWeekOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.WEEK_OF_MONTH);
    }




    // 특정 날짜로 주의 시작 설정
    private void setToStartOfWeek(Calendar date) {
        date.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
    }
}