package com.kenshu.kintai.entity;

public class Kintai {

    private String kinmuYmd;
    private String workSt;
    private String workEd;
    private String workRt;

    public String getKinmuYmd() {
        return kinmuYmd;
    }

    public void setKinmuYmd(String kinmuYmd) {
        this.kinmuYmd = kinmuYmd;
    }

    public String getWorkSt() {
        return workSt;
    }

    public void setWorkSt(String workSt) {
        this.workSt = workSt;
    }

    public String getWorkEd() {
        return workEd;
    }

    public void setWorkEd(String workEd) {
        this.workEd = workEd;
    }

    public String getWorkRt() {
        return workRt;
    }

    public void setWorkRt(String workRt) {
        this.workRt = workRt;
    }

    // yyyyMMdd 形式の勤務日を yyyy/MM/dd 形式に変換して返す
    public String getDisplayDate() {
        if (kinmuYmd == null || kinmuYmd.length() != 8) {
            return kinmuYmd; // 不正な形式の場合はそのまま返すか、エラーを示す値を返す
        }
        return kinmuYmd.substring(0, 4) + "/" + kinmuYmd.substring(4, 6) + "/" + kinmuYmd.substring(6, 8);
    }

    // yyyyMMdd 形式の勤務日から曜日を計算して返す
    public String getWeekDay() {
        if (kinmuYmd == null || kinmuYmd.length() != 8) {
            return ""; // 不正な形式の場合は空文字列を返す
        }
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(kinmuYmd, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
            switch (dayOfWeek) {
                case MONDAY: return "月";
                case TUESDAY: return "火";
                case WEDNESDAY: return "水";
                case THURSDAY: return "木";
                case FRIDAY: return "金";
                case SATURDAY: return "土";
                case SUNDAY: return "日";
                default: return "";
            }
        } catch (java.time.format.DateTimeParseException e) {
            e.printStackTrace();
            return ""; // パースエラーの場合は空文字列を返す
        }
    }

    // 出勤時刻、退勤時刻、休憩時間から勤務時間を計算して返す (HH:mm 形式)
    public String getWorkTime() {
        if (workSt == null || workSt.length() != 4 || workEd == null || workEd.length() != 4 || workRt == null || workRt.length() != 3) {
            return ""; // 不正な形式の場合は空文字列を返す
        }
        try {
            int startHour = Integer.parseInt(workSt.substring(0, 2));
            int startMinute = Integer.parseInt(workSt.substring(2, 4));
            int endHour = Integer.parseInt(workEd.substring(0, 2));
            int endMinute = Integer.parseInt(workEd.substring(2, 4));
            int breakMinutes = Integer.parseInt(workRt);

            // 出勤時刻と退勤時刻を分に変換
            int startTimeInMinutes = startHour * 60 + startMinute;
            int endTimeInMinutes = endHour * 60 + endMinute;

            // 退勤時刻が出勤時刻より前の場合は翌日とみなす
            if (endTimeInMinutes < startTimeInMinutes) {
                endTimeInMinutes += 24 * 60;
            }

            // 勤務時間を計算 (分)
            int workTimeInMinutes = endTimeInMinutes - startTimeInMinutes - breakMinutes;

            // 勤務時間が負の場合は0とする
            if (workTimeInMinutes < 0) {
                workTimeInMinutes = 0;
            }

            // 勤務時間を HH:mm 形式に変換
            int workHours = workTimeInMinutes / 60;
            int workMinutes = workTimeInMinutes % 60;

            return String.format("%02d:%02d", workHours, workMinutes);

        } catch (NumberFormatException e) {
            e.printStackTrace();
            return ""; // 数値変換エラーの場合は空文字列を返す
        }
    }
}
