package com.example.kintai;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Kintai
{
	private String kinmuYmd; //勤務日（yyyyMMdd形式）
	private String workSt;   //出勤時刻（hhmm形式）
	private String workEd;   //退勤時刻（hhmm形式）
	private String workRt;   //休憩時間（分）
	private String workTime; //勤務時間（hh:mm形式）
	private String weekDay;  //曜日
	private String displayDate; //表示用の日付（yyyyMMdd形式で保存)
	
	//デフォルト値を設定
	private static final String DEFAULT_WORK_ST = "";
	private static final String DEFAULT_WORK_ED = "";
	private static final String DEFAULT_WORK_RT = "";
	
	//デフォルトコンストラクタ：初期化時にフィールドにデフォルト値を設定
	public Kintai()
	{
		this.workSt = DEFAULT_WORK_ST; //出勤時刻にデフォルト値を設定
		this.workEd = DEFAULT_WORK_ED; //退勤時刻にデフォルト値を設定
		this.workRt = DEFAULT_WORK_RT; //休憩時間にデフォルト値を設定
	}
	
	//勤務日、出勤時刻、退勤時刻、休憩時間を指定して初期化するコンストラクタ
	public Kintai ( String kinmu_Ymd, String work_St, String work_Ed, String work_Rt )
	{
		this.kinmuYmd = kinmu_Ymd != null ? kinmu_Ymd : ""; //勤務日を設定
		this.workSt = work_St != null ? work_St : DEFAULT_WORK_ST; //出勤時刻を設定
		this.workEd = work_Ed != null ? work_Ed : DEFAULT_WORK_ED; //退勤時刻を設定
		this.workRt = work_Rt != null ? work_Rt : DEFAULT_WORK_RT; //休憩時間を設定
		this.weekDay = calculateWeekDay ( kinmu_Ymd ); //曜日を計算
		this.displayDate = formatDisplayDate ( kinmu_Ymd ); //表示用の日付を設定
		calculateWorkTime(); //勤務時間を計算
	}
	
	//勤務時間を計算するメソッド
	public void calculateWorkTime()
	{
		//出勤時刻と退勤時刻が存在する場合のみ計算を行う
		if ( workSt != null && workEd != null )
		{
			try
			{
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern ( "HHmm" ); //時刻を"HH:mm"フォーマットで解析
				LocalTime start = LocalTime.parse ( workSt, formatter ); //出勤時刻を変換
				LocalTime end = LocalTime.parse ( workEd, formatter ); //退勤時刻を変換
				
				//勤務時間 (休憩時間を考慮前) を計算
				Duration workDuration = Duration.between ( start, end );
				
				//休憩時間が設定されている場合のみ減算
				if ( workRt != null && !workRt.isEmpty() )
				{
					int restMinutes = Integer.parseInt ( workRt ); //休憩時間を分に変換
					workDuration = workDuration.minusMinutes ( restMinutes ); //休憩時間を引く
				}
				//勤務時間を "HH:mm" フォーマットで保存
				long hours = workDuration.toHours(); //時間を取得
				long minutes = workDuration.toMinutes() % 60; //分を取得
				this.workTime = String.format ( "%02d:%02d", hours, minutes ); //フォーマットして設定
			}
			catch ( DateTimeParseException e ) //例外処理
			{
				e.printStackTrace(); //例外発生時にスタックトレースを出力
				this.workTime = "00:00"; //例外発生時のデフォルト
			}
		}
		else
		{
			//出勤時刻または退勤時刻が無効な場合は、勤務時間を"00:00"に設定
			this.workTime = "00:00";
		}
	}
	
	//勤務時間を更新するためのメソッド
	public void updateWork_Time()
	{
		calculateWorkTime(); //勤務時間を再計算
	}
	
	//勤務日から曜日を計算するメソッド
	public String calculateWeekDay ( String dateStr )
	{
		try
		{
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern ( "yyyyMMdd" ); //日付フォーマットを設定
			LocalDate date = LocalDate.parse ( dateStr, formatter ); //日付を変換
			DayOfWeek dayOfWeek = date.getDayOfWeek(); //曜日を取得
			String[] weekDays = {"日", "月", "火", "水", "木", "金", "土"}; //曜日の配列
			return weekDays[dayOfWeek.getValue() % 7]; //曜日を返す
		}
		catch ( DateTimeParseException e ) //例外処理
		{
			e.printStackTrace(); //例外発生時にスタックトレースを出力
			return "不明"; //例外発生時のデフォルト
		}
	}
	
	//表示用の日付をフォーマットするメソッド
	private String formatDisplayDate ( String dateStr )
	{
		try
		{
			DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern ( "yyyyMMdd" ); //入力の日付フォーマットを "yyyyMMdd" に設定
			DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern ( "yyyy/MM/dd" ); //出力の日付フォーマットを "yyyy/MM/dd" に設定
			LocalDate date = LocalDate.parse ( dateStr, inputFormatter ); //入力文字列を LocalDate 型に変換
			return date.format ( outputFormatter ); //指定されたフォーマットで日付を出力
		}
		catch ( DateTimeParseException e ) //例外処理
		{
			e.printStackTrace();
			return "不明"; //エラー発生時のデフォルト値 "不明" を返す
		}
	}
	
	//getter と setter メソッド
	public String getKinmuYmd()
	{
		return kinmuYmd;
	}
	
	public void setKinmuYmd ( String kinmuYmd )
	{
		this.kinmuYmd = kinmuYmd; //勤務日を設定
		this.weekDay = calculateWeekDay ( kinmuYmd ); //勤務日の変更に伴い、曜日も更新
		this.displayDate = formatDisplayDate ( kinmuYmd ); //表示用の日付を更新
	}
	
	public String getWorkSt()
	{
		return workSt; //出勤時刻を返す
	}
	
	public void setWorkSt ( String workSt )
	{
		this.workSt = workSt != null ? workSt : DEFAULT_WORK_ST; //出勤時刻を設定
		updateWork_Time(); //出勤時刻、退勤時刻、または休憩時間が変更された場合、勤務時間を再計算
	}
	
	public String getWorkEd()
	{
		return workEd; //退勤時刻を返す
	}
	
	public void setWorkEd ( String workEd )
	{
		this.workEd = workEd != null ? workEd : DEFAULT_WORK_ED; //退勤時刻を設定
		updateWork_Time(); //出勤時刻、退勤時刻、または休憩時間が変更された場合、勤務時間を再計算
	}
	
	public String getWorkRt()
	{
		return workRt; //休憩時間を返す
	}
	
	public void setWorkRt ( String workRt )
	{
		if ( workRt != null && workRt.length() <= 3 ) //休憩時間が3文字以下の場合
		{
			this.workRt = workRt != null ? workRt : DEFAULT_WORK_RT; //休憩時間を設定
			updateWork_Time(); //出勤時刻、退勤時刻、または休憩時間が変更された場合、勤務時間を再計算
		}
		else
		{
			this.workRt = DEFAULT_WORK_RT; //デフォルト値を設定
			updateWork_Time(); //勤務時間を再計算
		}
	}
	
	public String getWorkTime()
	{
		return workTime; //勤務時間を返す
	}
	
	public String getWeekDay()
	{
		return weekDay; //曜日を返す
	}
	
	public void setWeekDay( String weekDay )
	{
		this.weekDay = weekDay; //曜日を設定
	}
	
	public String getDisplayDate()
	{
		return displayDate; //表示用の日付を返す
	}
	
	public void setDisplayDate ( String displayDate )
	{
		this.displayDate = displayDate; //表示用の日付を設定
	}
}
