package com.example.kintai;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level; // 追加
import java.util.logging.Logger; // 追加

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.example.kintai.DBConnection;
import com.example.kintai.Kintai;

import com.example.kintai.KintaiService;

//サーブレットのURLパターンを設定
@WebServlet("/kintai") // @WebServletアノテーションを追加
public class KintaiServlet extends HttpServlet
{
	private static final Logger logger = Logger.getLogger(KintaiServlet.class.getName());

	//GETリクエストを処理するメソッド
	@Override
	protected void doGet ( HttpServletRequest request, HttpServletResponse response )
	throws ServletException, IOException
	{
		//JSPページへのフォワード
		request.getRequestDispatcher ( "/WEB-INF/jsp/kintaiForm.jsp" ).forward ( request, response );
	}
	//POSTリクエストを処理するメソッド
	protected void doPost ( HttpServletRequest request, HttpServletResponse response )
	throws ServletException, IOException
	{
		request.setCharacterEncoding ( "UTF-8" ); //リクエストの文字エンコーディングを設定
		
		//入力パラメータを取得
		String action = request.getParameter ( "action" );
		String message = ""; //メッセージ用の変数
		
		//アクションが 検索 の場合
		if ( "search".equals ( action ) )
		{
			String year = request.getParameter ( "year" ); //年を取得
			String month = request.getParameter ( "month" ); //月を取得
		
			//入力チェック
			if ( year == null || year.isEmpty() || month == null || month.isEmpty() ) //年または月が未入力の場合
			{
				message = "年月を指定してください。"; //エラーメッセージを設定
			}
			else if ( !year.matches ( "\\d{4}" ) ) //年が4桁の数字でない場合
			{
				message = "年はyyyyで入力してください。"; //エラーメッセージを設定
			}
			else
			{
				//勤怠データのリストを取得（KintaiServiceを利用）
				List<Kintai> kintaiList = KintaiService.getKintaiList ( year, month );
			
				//テーブルを表示するフラグを設定
				request.setAttribute ( "showTable", !kintaiList.isEmpty() ); //検索結果がある場合のみ表示
				
				//勤怠データをリクエストスコープに設定
				request.setAttribute ( "kintaiList", kintaiList );
				
				//合計残業時間を計算し、リクエストスコープに設定
				long totalOvertimeMinutes = KintaiService.calculateTotalMonthlyOvertime(kintaiList);
				request.setAttribute("totalOvertimeMinutes", totalOvertimeMinutes);
				logger.log(Level.INFO, "KintaiServlet: doPost - totalOvertimeMinutes: " + totalOvertimeMinutes);
				
				//検索結果に応じたメッセージを設定
				message = !kintaiList.isEmpty() ? "検索完了" : "該当する勤怠データは見つかりませんでした。";
			}
		}
		//アクションが 登録 の場合
		else if ( "register".equals ( action ) )
		{
			message = registerKintai ( request ); //勤怠データを登録
			
			//登録後に再度データを取得
			String year = request.getParameter ( "year" ); //年を取得
			String month = request.getParameter ( "month" ); //月を取得
			List<Kintai> kintaiList = KintaiService.getKintaiList ( year, month ); //勤怠データのリストを取得
			request.setAttribute ( "kintaiList", kintaiList ); //勤怠データをリクエストスコープに設定
			
			//合計残業時間を計算し、リクエストスコープに設定 (登録後も表示を更新するため)
			long totalOvertimeMinutes = KintaiService.calculateTotalMonthlyOvertime(kintaiList);
			request.setAttribute("totalOvertimeMinutes", totalOvertimeMinutes);
			logger.log(Level.INFO, "KintaiServlet: doPost (register) - totalOvertimeMinutes: " + totalOvertimeMinutes);
		}
		//アクションが 削除 の場合
		else if ( "delete" .equals ( action ) )
		{
			message = deleteKintai ( request ); //勤怠データを削除
		}
		
		//メッセージをリクエストスコープに設定
		request.setAttribute ( "message", message );
		
		//JSPページへのフォワード
		request.getRequestDispatcher ( "/WEB-INF/jsp/kintaiForm.jsp" ).forward ( request, response );
	}
	
	//勤怠データを登録または更新メソッド
	private String registerKintai ( HttpServletRequest request )
	{
		//フォームから勤怠データを取得
		String year = request.getParameter ( "year" ); //年を取得
		String month = request.getParameter ( "month" ); //月を取得
		String message = ""; //メッセージ用の変数
		boolean hasError = false; //エラーフラグを追加
		
		//指定された年月の日数を取得
		int daysInMonth = java.time.YearMonth.of ( Integer.parseInt ( year ), Integer.parseInt ( month ) ).lengthOfMonth();
		
		//各日の勤怠データを登録するループ
		for ( int day = 1; day <= daysInMonth; day++ )
		{
			//データベースに保存するために yyyyMMdd 形式に変換
			String kinmu_ymd = String.format ( "%04d%02d%02d", Integer.parseInt ( year ), Integer.parseInt ( month ), day );
			
			//表示用に yyyy/MM/dd 形式に変換
			String displayDate = String.format ( "%04d/%02d/%02d", Integer.parseInt ( year ), Integer.parseInt ( month ), day );
			
			String work_st = request.getParameter ( "work_st_" + kinmu_ymd ); //出勤時刻を取得
			String work_ed = request.getParameter ( "work_ed_" + kinmu_ymd ); //退勤時刻を取得
			String work_rt = request.getParameter ( "work_rt_" + kinmu_ymd ); //休憩時間を取得
			
			// nullの場合は空文字列に変換
			if (work_st == null) work_st = "";
			if (work_ed == null) work_ed = "";
			if (work_rt == null) work_rt = "";

			boolean isWorkStEmpty = work_st.isEmpty();
			boolean isWorkEdEmpty = work_ed.isEmpty();
			boolean isWorkRtEmpty = work_rt.isEmpty();

			// 1. 出勤時刻と退勤時刻のどちらか一方のみが入力されている場合
			if ( (isWorkStEmpty && !isWorkEdEmpty) || (!isWorkStEmpty && isWorkEdEmpty) ) {
				hasError = true;
				continue; // この日の処理をスキップ
			}

			// 2. 出勤時刻と退勤時刻が両方空で、休憩時間のみが入力されている場合
			if ( isWorkStEmpty && isWorkEdEmpty && !isWorkRtEmpty ) {
				hasError = true;
				continue; // この日の処理をスキップ
			}
			
			// 勤務日が既に登録されているか確認
			boolean isRegistered = checkKintaiExists ( kinmu_ymd );
			
			// Kintaiオブジェクトを作成し、残業時間を計算
			Kintai kintai = new Kintai(kinmu_ymd, work_st, work_ed, work_rt);
			kintai.calculateWorkTime(); // 残業時間を計算
			logger.log(Level.INFO, "KintaiServlet: registerKintai - Calculated overtime for " + kinmu_ymd + ": " + kintai.getOvertimeMinutes() + " minutes");
			
			// データベースに接続し、勤怠データを登録または更新
			try ( Connection connection = DBConnection.getConnection() )
			{
				String sql;
				
				// 既存の勤怠データがあれば更新、なければ挿入
				if ( isRegistered )
				{
					sql = "UPDATE tbl_kintai SET work_st = ?, work_ed = ?, work_rt = ?, overtime_minutes = ? WHERE kinmu_ymd = ?"; //更新SQL文
				}
				else
				{
					sql = "INSERT INTO tbl_kintai (kinmu_ymd, work_st, work_ed, work_rt, overtime_minutes) VALUES (?, ?, ?, ?, ?)"; //挿入SQL文
				}
				
				// 各パラメータをセット
				try ( PreparedStatement statement = connection.prepareStatement ( sql ) )
				{
					if ( isRegistered ) {
						statement.setString ( 1, work_st ); //出勤時刻をセット
						statement.setString ( 2, work_ed ); //退勤時刻をセット
						statement.setString ( 3, work_rt ); //休憩時間をセット
						statement.setInt ( 4, kintai.getOvertimeMinutes() ); //残業時間をセット
						statement.setString ( 5, kinmu_ymd ); //勤務日をセット (WHERE句)
					} else {
						statement.setString ( 1, kinmu_ymd ); //勤務日をセット
						statement.setString ( 2, work_st ); //出勤時刻をセット
						statement.setString ( 3, work_ed ); //退勤時刻をセット
						statement.setString ( 4, work_rt ); //休憩時間をセット
						statement.setInt ( 5, kintai.getOvertimeMinutes() ); //残業時間をセット
					}
					
					logger.log(Level.INFO, "KintaiServlet: registerKintai - Executing SQL: " + sql + " with overtime: " + kintai.getOvertimeMinutes());
					// SQL文を実行
					int rowsAffected = statement.executeUpdate(); //SQL文を実行して影響を受けた行数を取得 
					
					if ( rowsAffected == 0 )
					{
						// 影響を受けた行がない場合
						message = "登録失敗"; //登録失敗メッセージを設定
						hasError = true; //エラーフラグを設定
					}
				}
			}
			catch ( SQLException e )
			{
				// エラーが発生した場合
				logger.log(Level.SEVERE, "KintaiServlet: registerKintai - SQL Error: " + e.getMessage(), e);
				message = "登録失敗"; //登録失敗メッセージを設定
				hasError = true; //エラーフラグを設定
			}
			
			// displayDate をリクエストスコープに設定 (この行は不要かもしれないが、既存ロジックに合わせて残す)
			request.setAttribute ( "displayDate_" + kinmu_ymd, displayDate );
		}
		
		if ( hasError )
		{
			//エラーフラグが立っている場合
			message = "勤怠表の入力に誤りがあります。"; //エラーメッセージを設定
		}
		else if ( message.isEmpty() ) { // エラーがなく、メッセージが設定されていない場合のみ成功メッセージ
			message = "登録成功";
		}
		return message; //登録結果メッセージを返す
	}
	
	//勤怠データが既に登録されているかチェックするメソッド
	private boolean checkKintaiExists ( String kinmu_ymd )
	{
		try ( Connection connection = DBConnection.getConnection() ) //データベース接続を取得
		{
			String sql = "SELECT COUNT(*) FROM tbl_kintai WHERE kinmu_ymd = ?"; //SQL文を準備
			try ( PreparedStatement statement = connection.prepareStatement ( sql ) ) //プリペアドステートメントを作成
			{
				statement.setString ( 1, kinmu_ymd ); //勤務日をパラメータとして設定
			
				ResultSet resultSet = statement.executeQuery(); //SQL文を実行
				if ( resultSet.next() ) //結果セットにデータがある場合
				{
					return resultSet.getInt ( 1 ) > 0;  //既に登録されている場合はtrueを返す
				}
			}
		}
		catch ( SQLException e ) //例外処理
		{
			//エラーが発生した場合、例外をログに出力
			e.printStackTrace(); //スタックトレースを出力
		}
		return false; //エラーが発生した場合はfalseを返す
	}
	
	//勤怠データを削除するメソッド
	private String deleteKintai ( HttpServletRequest request )
	{
		String year = request.getParameter ( "year" ); //年を取得
		String month = request.getParameter ( "month" ); //月を取得
		String message = "";
		
		// 入力チェック
		if ( year == null || year.isEmpty() || month == null || month.isEmpty() )
		{
			return "年月を指定してください。"; // エラーメッセージ
		}
		
		//月が1桁の場合、0埋めする
		if ( month.length() == 1 )
		{
			month = "0" + month;
		}
		
		//年月をyyyyMM形式にする
		String yearMonth = year + month;
		
		//データベースに接続して1ヶ月分の勤怠データを削除
		try ( Connection connection = DBConnection.getConnection() )
		{
			String sql = "DELETE FROM tbl_kintai WHERE LEFT(kinmu_ymd, 6) = ?"; //年月を基に削除するSQL文
			try ( PreparedStatement statement = connection.prepareStatement ( sql ) )
			{
				statement.setString ( 1, yearMonth ); //年月をセット
				
				int rowsAffected = statement.executeUpdate(); //SQL文を実行
				if ( rowsAffected > 0 )
				{
					message = "削除完了"; //削除成功メッセージ
				}
			}
		}
		catch ( SQLException e )
		{
			//エラーが発生した場合
			e.printStackTrace(); //スタックトレースを出力
			message = "削除失敗"; //削除失敗メッセージ
		}
		return message; //メッセージを返す
	}
}
