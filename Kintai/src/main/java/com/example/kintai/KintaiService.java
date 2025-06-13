package com.example.kintai;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.kintai.DBConnection;
import com.example.kintai.Kintai;

public class KintaiService
{
	//Loggerを使用
	private static final Logger logger = Logger.getLogger ( KintaiService.class.getName() );

	//1ヶ月の勤務時間の合計を計算するメソッド
	public static long calculateTotalMonthlyWorkTime ( List<Kintai> kintaiList )
	{
		long totalMinutes = 0; //総勤務時間を分単位で保持
		
		//各日の勤務時間をループ処理で合計する
		for ( Kintai kintai : kintaiList )
		{
			String workSt = kintai.getWorkSt(); //出勤時刻を取得
			String workEd = kintai.getWorkEd(); //退勤時刻を取得
			String workRt = kintai.getWorkRt(); //休憩時間を取得
		
			long restDurationMinutes = 0; //休憩時間がnullまたは空の場合はデフォルトで0分を設定
			if ( workRt != null && !workRt.isEmpty() ) //休憩時間がnullまたは空でない場合
			{
				restDurationMinutes = Integer.parseInt ( workRt ); //休憩時間を分単位に変換
			}
			
			if ( workSt != null && workEd != null && !workSt.isEmpty() && !workEd.isEmpty() ) //出勤時刻と退勤時刻がnullまたは空でない場合
			{
				try
				{
					SimpleDateFormat sdf = new SimpleDateFormat ( "HHmm" ); //HHmmフォーマットを使用
					long startTime = sdf.parse ( workSt ).getTime(); //出勤時刻を時間に変換
					long endTime = sdf.parse ( workEd ).getTime(); //退勤時刻を時間に変換
					
					//勤務時間(休憩時間を含まない)を計算
					long workDurationMinutes = ( endTime - startTime ) / (1000 * 60);
					
					//休憩時間を引く
					workDurationMinutes -= restDurationMinutes;
					
					//勤務時間に加算
					totalMinutes += workDurationMinutes;
				}
				catch ( Exception e ) //例外処理
				{
					//エラーログを記録
					logger.log ( Level.SEVERE, "勤務時間の計算中にエラーが発生しました: ", e );
				}
			}
		}
		//合計を返す
		return totalMinutes;
	}
	
	//勤怠データのリストを取得するメソッド
	public static List<Kintai> getKintaiList ( String year, String month )
	{
		List<Kintai> kintaiList = new ArrayList<>(); //勤怠データのリストを初期化
		
		//SQLクエリを定義
		String sql =  "SELECT kinmu_ymd, work_st, work_ed, work_rt, overtime_minutes FROM tbl_kintai "
		           + "WHERE DATE_FORMAT(kinmu_ymd, '%Y') = ? AND DATE_FORMAT(kinmu_ymd, '%m') = ?";
		
		try ( Connection connection = DBConnection.getConnection();
			PreparedStatement statement = connection.prepareStatement ( sql ) )
		{
			//年と月をパラメータとして設定
			statement.setString ( 1, year );
			statement.setString ( 2, month );
			
			ResultSet resultSet = statement.executeQuery(); //クエリを実行
			
			//結果をループ処理
			while ( resultSet.next() )
			{
				Kintai kintai = new Kintai(); //勤怠オブジェクトを作成
				
				kintai.setKinmuYmd ( resultSet.getString ( "kinmu_ymd" ) ); //勤務日を設定
				kintai.setWorkSt ( resultSet.getString ( "work_st" ) );    //出勤時刻を設定
				kintai.setWorkEd ( resultSet.getString ( "work_ed" ) );     //退勤時刻を設定
				kintai.setWorkRt ( resultSet.getString ( "work_rt" ) );    //休憩時間を設定
				kintai.setOvertimeMinutes ( resultSet.getInt ( "overtime_minutes" ) ); //残業時間を設定
				
				//曜日を計算して設定
				kintai.setWeekDay ( kintai.calculateWeekDay ( kintai.getKinmuYmd() ) );
				kintaiList.add ( kintai ); //リストに追加
				logger.log(Level.INFO, "KintaiService: getKintaiList - 勤怠データ取得: " + kintai.getKinmuYmd());
			}
			
			//検索結果がない場合
			if ( kintaiList.isEmpty() )
			{
				logger.log(Level.INFO, "KintaiService: getKintaiList - 勤怠データが見つかりませんでした。空のデータを生成します。");
				createKintaiListForMonth ( year, month ); //年月に対応する日付リストを生成する (データベースに挿入)
				// データベースに挿入後、再度データを取得し直す
				kintaiList = getKintaiList ( year, month );
				logger.log(Level.INFO, "KintaiService: getKintaiList - 空のデータ生成後、再取得した勤怠データ数: " + kintaiList.size());
			}
		}
		catch ( SQLException e ) //例外処理
		{
			logger.log ( Level.SEVERE, "勤怠データの取得中にエラーが発生しました: ", e ); //エラーログを記録
			}
		//勤怠リストを返す
		return kintaiList;
		}
	
	//指定された年と月に対応する勤怠データリストを生成するメソッド
	private static List<Kintai> createKintaiListForMonth ( String year, String month )
	{
		List<Kintai> kintaiList = new ArrayList<>(); //勤怠データのリストを初期化
		
		int yearInt = Integer.parseInt ( year ); //年を整数に変換
		int monthInt = Integer.parseInt ( month ) - 1; //月を整数に変換 (0-indexed)
		
		//該当月の日数を計算
		Calendar calendar = new GregorianCalendar ( yearInt, monthInt, 1 ); //カレンダーを初期化
		int daysInMonth = calendar.getActualMaximum ( Calendar.DAY_OF_MONTH ); //該当月の日数を取得
		
		SimpleDateFormat sdf = new SimpleDateFormat ( "yyyyMMdd" ); //日付フォーマットを設定
		for ( int day = 1; day <= daysInMonth; day++ ) //各日付の勤怠データを作成
		{
			Kintai kintai = new Kintai(); //勤怠オブジェクトを作成
			calendar.set ( Calendar.DAY_OF_MONTH, day ); //カレンダーの日付を設定
			String formattedDate = sdf.format ( calendar.getTime() ); //日付をフォーマット
			kintai.setKinmuYmd ( formattedDate ); //勤務日を設定
			kintai.setWorkSt ( "" ); //デフォルト値を使用
			kintai.setWorkEd ( "" ); //デフォルト値を使用
			kintai.setWorkRt (	"" ); //デフォルト値を使用
			
			kintai.setWeekDay ( kintai.calculateWeekDay ( formattedDate ) ); //曜日を計算して設定
			
			kintaiList.add ( kintai ); //リストに追加
			
			insertEmptyKintaiData ( kintai ); //生成した空のデータをデータベースに挿入
		}
		return kintaiList;
	}
	
	//生成した空の勤怠データをデータベースに挿入するメソッド
	private static void insertEmptyKintaiData ( Kintai kintai )
	{
		String insertSql = "INSERT INTO tbl_kintai (kinmu_ymd, work_st, work_ed, work_rt, overtime_minutes) VALUES (?, '', '', '', ?)"; //SQL挿入クエリを定義
		
		try ( Connection connection = DBConnection.getConnection(); //データベース接続を取得
				PreparedStatement statement = connection.prepareStatement ( insertSql ) ) //プリペアドステートメントを作成
		{
			statement.setString(1, kintai.getKinmuYmd() ); //勤務日をパラメータとして設定
			statement.setInt(2, kintai.getOvertimeMinutes()); //残業時間をパラメータとして設定
			int rowsAffected = statement.executeUpdate(); //SQLクエリを実行してデータを挿入
			if (rowsAffected > 0) {
				logger.log(Level.INFO, "KintaiService: insertEmptyKintaiData - 空の勤怠データ挿入成功: " + kintai.getKinmuYmd());
			} else {
				logger.log(Level.WARNING, "KintaiService: insertEmptyKintaiData - 空の勤怠データ挿入失敗 (影響を受けた行なし): " + kintai.getKinmuYmd());
			}
		}
		catch ( SQLException e ) //例外処理
		{
			logger.log ( Level.SEVERE, "空の勤怠データを挿入中にエラーが発生しました: " + kintai.getKinmuYmd(), e ); //エラーログを記録
		}
	}
	
	//1ヶ月の残業時間の合計を計算するメソッド
	public static long calculateTotalMonthlyOvertime ( List<Kintai> kintaiList )
	{
		long totalOvertimeMinutes = 0; //総残業時間を分単位で保持
		
		//各日の残業時間をループ処理で合計する
		for ( Kintai kintai : kintaiList )
		{
			totalOvertimeMinutes += kintai.getOvertimeMinutes();
		}
		//合計を返す
		return totalOvertimeMinutes;
	}
	
	//月の日数を取得するメソッド
	public static int getDaysInMonth ( String year, String month )
	{
		Calendar calendar = new GregorianCalendar ( Integer.parseInt ( year ) , Integer.parseInt ( month ) - 1,1 ); //カレンダーを初期化
		return calendar.getActualMaximum ( Calendar.DAY_OF_MONTH ); //該当月の日数を返す
	}
}
