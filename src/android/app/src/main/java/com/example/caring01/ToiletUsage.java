package com.example.caring01;

public class ToiletUsage {
    private String count;
    private Integer duration;
    private String startTime;
    private String endTime;
    private double dailyavg;  // 주 탭 하루 평균
    private String dailycount;  // 주 탭
    private String day; // 주 탭 split 날짜
    private Integer weeklycount;
    private String week; // 월 탭 날짜
    private double weeklyavg;
    private Integer monthlycount;
    private double monthlyavg;
    private String month;



    // 기본 생성자
    public ToiletUsage() {}

    // 일 탭 생성자
    public ToiletUsage(String count ,String startTime, String endTime, Integer duration) {
        this.count = count;
        this.duration = duration;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // 주 탭 생성자
    public ToiletUsage(String day, String dailycount, double dailyavg){
        this.day = day;
        this.dailycount = dailycount;
        this.dailyavg = dailyavg;
    }

    // 월 탭 생성자
    public ToiletUsage(String week, Integer weeklycount, double weeklyavg) {
        this.week = week; // "week1", "week2" 등
        this.weeklycount = weeklycount;
        this.weeklyavg = weeklyavg; // 주 평균
    }

    // 년 탭 생성자
    public ToiletUsage(Integer monthlycount, double monthlyavg, String month) {
        this.month = month;
        this.monthlycount = monthlycount;
        this.monthlyavg = monthlyavg; // 월 평균
    }


    public String getDay(){
        return day;
    }

    public void setDay(String day){
        this.day = day;
    }

    public String getCount(){
        return count;
    }

    public void setCount(String count){
        this.count = count;
    }
    // Getter and Setter
    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public double getDailyAvg(){
        return dailyavg;
    }

    public void setDailyAvg(double dailyavg){
        this.dailyavg = dailyavg;
    }

    public String getDailyCount(){
        return dailycount;
    }

    public void setDailyCount(String dailycount){
        this.dailycount = dailycount;
    }

    public Integer getWeeklyCount() { return weeklycount; }

    public void setWeeklyCount(Integer weeklycount) { this.weeklycount = weeklycount; }

    public String getWeek() { return week; }

    public void setWeek(String week) { this.week = week; }

    public double getWeeklyAvg() { return weeklyavg; }

    public void setWeeklyCount(double weeklyavg) { this.weeklyavg = weeklyavg; }

    public Integer getMonthlycount() { return monthlycount; }

    public void setMonthlycount(Integer mothlycount) { this.monthlycount = monthlycount; }

    public double getMonthlyavg() { return monthlyavg; }

    public void setMonthlyavg(double monthlyavg) { this.monthlyavg = monthlyavg; }

    public String getMonth() { return month; }

    public void setMonth(String month) { this.month = month; }


}
