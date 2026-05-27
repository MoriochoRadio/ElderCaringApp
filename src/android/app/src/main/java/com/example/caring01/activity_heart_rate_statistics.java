package com.example.caring01;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class activity_heart_rate_statistics extends AppCompatActivity {

    private GraphView heartRateGraph;
    private TextView maxHeartRate, avgHeartRate, minHeartRate, dateTextView;
    private TextView tab_daily, tab_weekly, tab_monthly, tab_yearly;
    private DatabaseReference databaseReference;
    private Date currentDate;
    private String currentTab = "daily"; // 기본값을 "daily"로 설정



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate_statistics);

        databaseReference = FirebaseDatabase.getInstance().getReference();
        heartRateGraph = findViewById(R.id.heartRateGraph);
        maxHeartRate = findViewById(R.id.maxHeartRate);
        avgHeartRate = findViewById(R.id.avgHeartRate);
        minHeartRate = findViewById(R.id.minHeartRate);
        dateTextView = findViewById(R.id.dateTextView);

        currentDate = new Date();
        updateDateTextView();

        // 초기 데이터 로드
        fetchHeartRateData(currentDate);

        findViewById(R.id.prevButton).setOnClickListener(v -> {
            updateDate(-1);
            fetchDataForCurrentTab();
        });

        findViewById(R.id.nextButton).setOnClickListener(v -> {
            updateDate(1);
            fetchDataForCurrentTab();
        });

        findViewById(R.id.tab_daily).setOnClickListener(v -> {
            currentTab = "daily";
            updateDateTextView();
            fetchHeartRateData(currentDate);
        });

        findViewById(R.id.tab_weekly).setOnClickListener(v -> {
            currentTab = "weekly";
            updateDateTextView();
            fetchWeeklyData(currentDate);
        });

        findViewById(R.id.tab_monthly).setOnClickListener(v -> {
            currentTab = "monthly";
            updateDateTextView();
            fetchMonthlyData(currentDate);
        });

        findViewById(R.id.tab_yearly).setOnClickListener(v -> {
            currentTab = "yearly";
            updateDateTextView();
            fetchYearlyData(currentDate);
        });
    }

    private void fetchDataForCurrentTab() {
        switch (currentTab) {
            case "daily":
                fetchHeartRateData(currentDate);
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

    // ... fetchHeartRateData, fetchWeeklyData, fetchMonthlyData, fetchYearlyData 등의 메서드는 기존 코드 그대로 유지


    private void fetchHeartRateData(Date date) {
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
                .child("heartRate")
                .child(year)
                .child(month)
                .child(week)
                .child(formattedDate);
        Log.d("HeartRateActivity", "Data found: " + path.toString());

        // 데이터 요청
        path.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d("HeartRateActivity", "Data found: " + dataSnapshot.getValue());
                    updateChartWithData(dataSnapshot);
                } else {
                    Log.d("HeartRateActivity", "No data found.");
                    clearChart();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("HeartRateActivity", "데이터 로드 실패: " + databaseError.getMessage());
            }
        });
    }

    private void updateChartWithData(DataSnapshot dataSnapshot) {
        // 새로운 BarGraphSeries 생성
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>();
        int max = Integer.MIN_VALUE, min = Integer.MAX_VALUE, total = 0, count = 0;

        for (DataSnapshot hourSnapshot : dataSnapshot.getChildren()) {
            try {
                // 시간 (0~23)
                String key = hourSnapshot.getKey();
                if (key == null) {
                    Log.e("HeartRateActivity", "Hour key is null");
                    continue;
                }

                int hour = Integer.parseInt(key); // Key를 시간으로 변환
                Integer hourlyAverage = hourSnapshot.child("Hourly average").getValue(Integer.class);

                if (hourlyAverage == null) {
                    Log.e("HeartRateActivity", "Hourly average is null for hour: " + hour);
                    continue;
                }

                // 데이터 포인트 추가 (Hourly average만 사용)
                series.appendData(new DataPoint(hour, hourlyAverage), true, 24);

                // 통계 계산
                max = Math.max(max, hourlyAverage);
                min = Math.min(min, hourlyAverage);
                total += hourlyAverage;
                count++;
            } catch (NumberFormatException e) {
                Log.e("HeartRateActivity", "Invalid hour format: " + hourSnapshot.getKey(), e);
            } catch (Exception e) {
                Log.e("HeartRateActivity", "Unexpected error while processing data", e);
            }
        }

        // 그래프에 시리즈 추가
        heartRateGraph.removeAllSeries();
        heartRateGraph.addSeries(series);

        // BarGraphSeries 스타일 설정
        series.setSpacing(20);  // 막대 간격 조정
        series.setDrawValuesOnTop(true);  // 각 막대 위에 값 표시
        series.setValuesOnTopColor(Color.BLACK);  // 값 색상

        // 통계 업데이트
        if (count > 0) {
            maxHeartRate.setText(max + " BPM");
            avgHeartRate.setText((total / count) + " BPM");
            minHeartRate.setText(min + " BPM");
        } else {
            maxHeartRate.setText("-- BPM");
            avgHeartRate.setText("-- BPM");
            minHeartRate.setText("-- BPM");
        }

        // X축 및 Y축 설정
        heartRateGraph.getGridLabelRenderer().setHorizontalAxisTitle("시간");
        heartRateGraph.getGridLabelRenderer().setVerticalAxisTitle("시간별 (BPM)");

        // X축을 0시부터 23시까지 설정
        heartRateGraph.getViewport().setXAxisBoundsManual(true);
        heartRateGraph.getViewport().setMinX(0);
        heartRateGraph.getViewport().setMaxX(23);

        // X축에 여백 추가 (양쪽 막대가 잘리지 않도록 설정)
        heartRateGraph.getViewport().setXAxisBoundsManual(true);
        heartRateGraph.getViewport().setMinX(series.getLowestValueX() - 2); // X축의 시작을 약간 이전으로 설정
        heartRateGraph.getViewport().setMaxX(series.getHighestValueX() + 2); // X축의 끝을 약간 이후로 설정



        // Y축 범위 설정 (최대값, 최소값)
        heartRateGraph.getViewport().setYAxisBoundsManual(true);
        heartRateGraph.getViewport().setMinY(min - 10); // 최소값 아래 여백
        heartRateGraph.getViewport().setMaxY(max + 10); // 최대값 위 여백

        // 확대/축소 및 스크롤 비활성화
        heartRateGraph.getViewport().setScrollable(false);
        heartRateGraph.getViewport().setScalable(false);
    }

    private void fetchWeeklyData(Date date) {
        // 년도와 월 포맷 설정
        SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat sdfMonth = new SimpleDateFormat("M", Locale.getDefault());
        String year = sdfYear.format(date);
        String month = sdfMonth.format(date);

        // 주차 계산(1~7일, 8~14일 등)
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int weekOfMonth = (dayOfMonth - 1) / 7 + 1;  // 주차 계산

        String week = "week" + weekOfMonth;

        // Firebase 경로 생성
        DatabaseReference path = databaseReference.child("USER")
                .child("StatsData")
                .child("heartRate")
                .child(year)
                .child(month)
                .child(week);

        Log.d("HeartRateActivity", "path : " + path.toString());

        // 데이터 요청
        path.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    updateWeeklyChartWithData(dataSnapshot);
                } else {
                    clearChart();
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
            Integer dailyAverage = daySnapshot.child("Daily average").getValue(Integer.class);
            if (dailyAverage != null) {
                Log.d("HeartRateActivity", "Day " + dayIndex + " - Daily average: " + dailyAverage);

                // X축 인덱스를 기준으로 데이터 포인트 추가
                series.appendData(new DataPoint(dayIndex, dailyAverage), true, 7);

                // 인덱스 증가
                dayIndex++;
            }
        }

        // 그래프에 시리즈 추가
        heartRateGraph.removeAllSeries();
        heartRateGraph.addSeries(series);

        // BarGraphSeries 스타일 설정
        series.setSpacing(50); // 막대 간격 조정
        series.setDrawValuesOnTop(true);  // 각 막대 위에 값 표시
        series.setValuesOnTopColor(Color.BLACK);  // 값 색상

        // X축과 Y축 레이블 설정
        heartRateGraph.getGridLabelRenderer().setHorizontalAxisTitle("일");
        heartRateGraph.getGridLabelRenderer().setVerticalAxisTitle("일 평균 (BPM)");





        // X축을 7일로 설정
        heartRateGraph.getViewport().setXAxisBoundsManual(true);
        heartRateGraph.getViewport().setMinX(1);
        heartRateGraph.getViewport().setMaxX(7);  // 7일로 설정

        // X축에 여백 추가 (양쪽 막대가 잘리지 않도록 설정)
        heartRateGraph.getViewport().setXAxisBoundsManual(true);
        heartRateGraph.getViewport().setMinX(series.getLowestValueX() - 2); // X축의 시작을 약간 이전으로 설정
        heartRateGraph.getViewport().setMaxX(series.getHighestValueX() + 2); // X축의 끝을 약간 이후로 설정

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
            heartRateGraph.getViewport().setYAxisBoundsManual(true);
            heartRateGraph.getViewport().setMinY(min - 10);
            heartRateGraph.getViewport().setMaxY(max + 10);
            // 통계 업데이트
            maxHeartRate.setText(max + " BPM");
            avgHeartRate.setText((total / count) + " BPM");
            minHeartRate.setText(min + " BPM");
        } else {
            maxHeartRate.setText("-- BPM");
            avgHeartRate.setText("-- BPM");
            minHeartRate.setText("-- BPM");
        }

        // 확대/축소 및 스크롤 비활성화
        heartRateGraph.getViewport().setScrollable(false);
        heartRateGraph.getViewport().setScalable(false);
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
                .child("heartRate")
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
                Log.e("HeartRateActivity", "월간 데이터 로드 실패: " + databaseError.getMessage());
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
                Log.d("HeartRateActivity", "week " + weekIndex + " - Weekly average: " + weeklyAverage);

                // X축 인덱스를 기준으로 데이터 포인트 추가
                series.appendData(new DataPoint(weekIndex, weeklyAverage), true, 5);

                // 인덱스 증가
                weekIndex++;
            }
        }

        // 그래프에 시리즈 추가
        heartRateGraph.removeAllSeries();
        heartRateGraph.addSeries(series);

        // BarGraphSeries 스타일 설정
        series.setSpacing(20); // 막대 간격 조정
        series.setDrawValuesOnTop(true);  // 각 막대 위에 값 표시
        series.setValuesOnTopColor(Color.BLACK);  // 값 색상

        // X축과 Y축 레이블 설정
        heartRateGraph.getGridLabelRenderer().setHorizontalAxisTitle("주");
        heartRateGraph.getGridLabelRenderer().setVerticalAxisTitle("주 평균 (BPM)");



        // X축 범위 설정 (1 ~ 4)
        heartRateGraph.getViewport().setXAxisBoundsManual(true);
        heartRateGraph.getViewport().setMinX(1);
        heartRateGraph.getViewport().setMaxX(4);  // 4주로 설정

        // X축에 여백 추가 (양쪽 막대가 잘리지 않도록 설정)
        heartRateGraph.getViewport().setXAxisBoundsManual(true);
        heartRateGraph.getViewport().setMinX(series.getLowestValueX() - 1); // X축의 시작을 약간 이전으로 설정
        heartRateGraph.getViewport().setMaxX(series.getHighestValueX() + 1); // X축의 끝을 약간 이후로 설정

        // X축 레이블 수동 설정 (1, 2, 3, 4)
        heartRateGraph.getGridLabelRenderer().setNumHorizontalLabels(4);  // 레이블 갯수 설정
        heartRateGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
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
            heartRateGraph.getViewport().setYAxisBoundsManual(true);
            heartRateGraph.getViewport().setMinY(min - 10);
            heartRateGraph.getViewport().setMaxY(max + 10);
            // 통계 업데이트
            maxHeartRate.setText(max + " BPM");
            avgHeartRate.setText((total / count) + " BPM");
            minHeartRate.setText(min + " BPM");
        } else {
            maxHeartRate.setText("-- BPM");
            avgHeartRate.setText("-- BPM");
            minHeartRate.setText("-- BPM");
        }

        // 확대/축소 및 스크롤 비활성화
        heartRateGraph.getViewport().setScrollable(false);
        heartRateGraph.getViewport().setScalable(false);
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
                } else {
                    clearChart();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("HeartRateActivity", "연간 데이터 로드 실패: " + databaseError.getMessage());
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
        heartRateGraph.removeAllSeries();
        heartRateGraph.addSeries(series);

        // BarGraphSeries 스타일 설정
        series.setSpacing(20); // 막대 간격 조정
        series.setDrawValuesOnTop(true);  // 각 막대 위에 값 표시
        series.setValuesOnTopColor(Color.BLACK);  // 값 색상

        // X축과 Y축 레이블 설정
        heartRateGraph.getGridLabelRenderer().setHorizontalAxisTitle("월");
        heartRateGraph.getGridLabelRenderer().setVerticalAxisTitle("월 평균 (BPM)");



        // X축 범위 설정: 실제로 데이터가 있는 월만 X축에 표시
        heartRateGraph.getViewport().setXAxisBoundsManual(true);
        heartRateGraph.getViewport().setMinX(monthsWithData.get(0));
        heartRateGraph.getViewport().setMaxX(monthsWithData.get(monthsWithData.size() - 1));

        // X축에 여백 추가 (양쪽 막대가 잘리지 않도록 설정)
        heartRateGraph.getViewport().setXAxisBoundsManual(true);
        heartRateGraph.getViewport().setMinX(series.getLowestValueX() - 1); // X축의 시작을 약간 이전으로 설정
        heartRateGraph.getViewport().setMaxX(series.getHighestValueX() + 1); // X축의 끝을 약간 이후로 설정

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
            heartRateGraph.getViewport().setYAxisBoundsManual(true);
            heartRateGraph.getViewport().setMinY(min - 10);
            heartRateGraph.getViewport().setMaxY(max + 10);
            // 통계 업데이트
            maxHeartRate.setText(max + " BPM");
            avgHeartRate.setText((total / count) + " BPM");
            minHeartRate.setText(min + " BPM");
        } else {
            maxHeartRate.setText("-- BPM");
            avgHeartRate.setText("-- BPM");
            minHeartRate.setText("-- BPM");
        }

        // 확대/축소 및 스크롤 비활성화
        heartRateGraph.getViewport().setScrollable(false);
        heartRateGraph.getViewport().setScalable(false);
    }
    private void clearChart() {
        heartRateGraph.removeAllSeries();
        maxHeartRate.setText("-- BPM");
        avgHeartRate.setText("-- BPM");
        minHeartRate.setText("-- BPM");
    }
}
