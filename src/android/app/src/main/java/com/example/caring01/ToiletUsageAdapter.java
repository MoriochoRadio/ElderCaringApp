package com.example.caring01;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ToiletUsageAdapter extends RecyclerView.Adapter<ToiletUsageAdapter.ToiletUsageViewHolder> {

    private List<ToiletUsage> toiletUsageList;

    public ToiletUsageAdapter(List<ToiletUsage> toiletUsageList) {
        this.toiletUsageList = toiletUsageList;
    }

    @NonNull
    @Override
    public ToiletUsageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usage, parent, false);
        return new ToiletUsageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ToiletUsageViewHolder holder, int position) {
        ToiletUsage toiletUsage = toiletUsageList.get(position);

        // 조건에 따라 UI에 표시할 데이터 처리

        if (toiletUsage != null && toiletUsage.getDuration() != null) {
            // 기존 데이터 표시 (시작, 종료 시간 및 사용 시간)
            int durationInMinutes = toiletUsage.getDuration() / 60;
            int remainingSeconds = toiletUsage.getDuration() % 60;


            holder.countTextView.setText(toiletUsage.getCount() + "회 ");
            holder.startTimeTextView.setText("시작\n" + toiletUsage.getStartTime());
            holder.durationTextView.setText("사용\n" + durationInMinutes + " 분 " + remainingSeconds + " 초");
            holder.endTimeTextView.setText("종료\n" + toiletUsage.getEndTime());
        } else if (toiletUsage != null && toiletUsage.getDay() != null) {
            // 주별 데이터 표시 (Daily count 및 Daily average)
            int dailyAvgInSeconds = (int) toiletUsage.getDailyAvg(); // double을 int로 변환
            int minutes = dailyAvgInSeconds / 60; // 분 계산
            int seconds = dailyAvgInSeconds % 60; // 초 계산

            holder.countTextView.setText(toiletUsage.getDay());
            holder.startTimeTextView.setText("총 사용 횟수\n" + toiletUsage.getDailyCount()+ "회 ");
            holder.durationTextView.setText("평균 사용\n" + minutes + " 분 " + seconds + " 초");


        }else if(toiletUsage != null && toiletUsage.getWeek() != null) {
            holder.countTextView.setText(toiletUsage.getWeek()); // "week1", "week2" 등 표시
            //월
            double weeklyAvg = toiletUsage.getWeeklyAvg();
            int avgMinutes = (int) (weeklyAvg / 60);
            int avgSeconds = (int) (weeklyAvg % 60);

            holder.durationTextView.setText("평균 사용\n" + avgMinutes + " 분 " + avgSeconds + " 초");

            // 사용 횟수도 표시
            holder.startTimeTextView.setText("총 사용 횟수\n" + toiletUsage.getWeeklyCount() + "회");
            holder.endTimeTextView.setText("");

        }else if(toiletUsage != null && toiletUsage.getMonthlycount() != null){
            holder.countTextView.setText(toiletUsage.getMonth());
            double monthlyAvg = toiletUsage.getMonthlyavg();
            int avgMinutes = (int) (monthlyAvg / 60);
            int avgSeconds = (int) (monthlyAvg % 60);

            holder.durationTextView.setText("평균 사용\n" + avgMinutes + " 분 " + avgSeconds + " 초");

            // 사용 횟수도 표시
            holder.startTimeTextView.setText("총 사용 횟수\n" + toiletUsage.getMonthlycount()+ "회 ");
            holder.endTimeTextView.setText("");


        }
        else {
            // 데이터가 없을 경우 기본값 설정
            holder.countTextView.setText("데이터 없음");
            holder.startTimeTextView.setText("");
            holder.durationTextView.setText("");
            holder.endTimeTextView.setText("");
        }

    }

    @Override
    public int getItemCount() {
        return toiletUsageList.size();
    }

    public static class ToiletUsageViewHolder extends RecyclerView.ViewHolder {

        TextView countTextView;
        TextView startTimeTextView;
        TextView endTimeTextView;
        TextView durationTextView;

        public ToiletUsageViewHolder(View itemView) {
            super(itemView);
            countTextView = itemView.findViewById(R.id.countTextView);
            startTimeTextView = itemView.findViewById(R.id.startTimeTextView);
            endTimeTextView = itemView.findViewById(R.id.endTimeTextView);
            durationTextView = itemView.findViewById(R.id.durationTextView);
        }
    }
}
