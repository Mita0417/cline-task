<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="com.example.kintai.Kintai" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<title>勤怠入力画面</title>
	<style>
		body {
			text-align: center; /* 全体のテキストを中央揃え */
		}
		form, table {
			margin: 0 auto; /* ブロック要素を中央揃え */
		}
		table {
			width: 80%; /* テーブルの幅を設定（必要に応じて調整） */
			border-collapse: collapse; /* テーブルの罫線を結合 */
		}
		th, td {
			padding: 8px; /* セルのパディング */
			border: 1px solid #ddd; /* セルの罫線 */
		}
	</style>
</head>
<body>
	<h1>勤怠表入力画面</h1>
	
	<!-- 年月を入力して勤怠データを検索 -->
	<form action = "/kintai-app/kintaiservlet" method = "post"> <!-- POSTメソッドでKintaiServletに送信 -->
		<input type = "hidden" name = "action" value = "search">
		
		<!-- 年の入力欄 -->
		<input type = "text" name = "year" id="yearInput" maxlength = "4" pattern="[0-9]{4}" required style="width: 60px;"> 年 <!-- 未入力の状態では送信不可 -->
		
		<!-- 月の選択リスト -->
		<select name = "month" required> <!-- 未入力の状態では送信不可 -->
			  <option value = "01">01</option>
			  <option value = "02">02</option>
			  <option value = "03">03</option>
			  <option value = "04">04</option>
			  <option value = "05">05</option>
			  <option value = "06">06</option>
			  <option value = "07">07</option>
			  <option value = "08">08</option>
			  <option value = "09">09</option>
			  <option value = "10">10</option>
			  <option value = "11">11</option>
			  <option value = "12">12</option>
		</select> 月度
		
		<!-- 検索ボタン -->
		<input type = "submit" value = "検索">
	</form>
	
	<!-- メッセージの表示 -->
	<% String message = ( String ) request.getAttribute ( "message" ); //メッセージを取得
		if ( message != null && !message.isEmpty() ) //メッセージが存在する場合
		{
	%>
		<p><%= message %></p> <!-- メッセージを表示 -->
	<%
		}
	%>
	
	<%
		List<Kintai> kintaiList = ( List<Kintai> ) request.getAttribute ( "kintaiList" ); //勤怠データのリストを取得
		boolean showTable = ( kintaiList != null && !kintaiList.isEmpty() ); //テーブルを表示するかどうかのフラグ
		
		// 合計勤務時間と合計残業時間の初期値を設定
		double totalMonthlyWorkTime = 0.0;
		long totalOvertimeMinutes = 0;

		// KintaiServletから渡された合計残業時間を取得
		Object totalOvertimeObj = request.getAttribute("totalOvertimeMinutes");
		if (totalOvertimeObj instanceof Long) {
			totalOvertimeMinutes = (Long) totalOvertimeObj;
		} else if (totalOvertimeObj instanceof Integer) { // Integerで渡される可能性も考慮
			totalOvertimeMinutes = ((Integer) totalOvertimeObj).longValue();
		}
	%>
	<% if (showTable) { %>
	<!-- 勤怠データの登録フォーム -->
	<form action = "/kintai-app/kintaiservlet" method = "post"> <!-- POSTメソッドでKintaiServletに送信 -->
	
		<!-- 年と月のデータを引き継ぐための隠しフィールド -->
		<input type = "hidden" name = "year" value = "<%= request.getParameter ( "year" ) %>">
		<input type = "hidden" name = "month" value = "<%= request.getParameter ( "month" ) %>">
		
		<!-- 登録ボタン -->
		<button type = "submit" name = "action" value = "register">登録</button>
		
		<!-- 削除ボタン -->
		<input type = "hidden" name = "action" value = "delete">
		<input type = "hidden" name = "year" value = "<%= request.getParameter ( "year" ) %>">
		<input type = "hidden" name = "month" value = "<%= request.getParameter ( "month" ) %>">
		<button type = "submit" onclick = "return confirmDelete()" style="margin-left: 10px;">削除</button>
		
		<table border = "1" style="margin-top: 20px;">
			<tr>
				<th>年月日</th>
				<th>曜日</th>
				<th>出勤時刻</th>
				<th>退勤時刻</th>
				<th>休憩時間(分)</th>
				<th>勤務時間</th>
				<th>残業時間</th>
			</tr>
			
			<%
				//年と月を取得
				String yearStr = request.getParameter ( "year" );
				String monthStr = request.getParameter ( "month" );
				
				//年と月が存在し、かつテーブルを表示する場合
				if ( yearStr != null && monthStr != null && showTable )
				{
					//各日の勤怠データを表示
					for ( Kintai kintai : kintaiList )
					{
						String date = kintai.getDisplayDate();     //表示用の日付を取得
						String weekDayName = kintai.getWeekDay();  //曜日を取得
						String startTimeStr = kintai.getWorkSt();  //出勤時刻を取得
						String endTimeStr = kintai.getWorkEd();    //退勤時刻を取得
						String breakTimeStr = kintai.getWorkRt();  //休憩時間を取得
						String workTimeStr = kintai.getWorkTime(); //勤務時間を取得
						String overtimeFormatted = kintai.getOvertimeFormatted(); //残業時間を取得
						
						double dailyWorkTime = 0.0; //日の勤務時間を初期化
						
						//勤務時間が存在する場合
						if ( workTimeStr != null && !workTimeStr.isEmpty() )
						{
							String[] timeParts = workTimeStr.split ( ":" ); //勤務時間を分割
							dailyWorkTime = Integer.parseInt ( timeParts[0] ) + Integer.parseInt ( timeParts[1] ) / 60.0; //日の勤務時間を計算
						}
						
						totalMonthlyWorkTime += dailyWorkTime; //月の合計勤務時間に加算
			%>
			<tr>
				<!-- 日付 -->
				<td><%= kintai.getDisplayDate() %></td>
				<!-- 曜日 -->
				<td><%= weekDayName %></td>
				<!-- 出勤時刻 -->
				<td><input type = "text" name = "work_st_<%= kintai.getKinmuYmd() %>" value = "<%= startTimeStr %>" maxlength="4" pattern="([01][0-9]|2[0-3])[0-5][0-9]" oninput="validateTimeInput(this)"/></td>
				<!-- 退勤時刻 -->
				<td><input type = "text" name = "work_ed_<%= kintai.getKinmuYmd() %>" value = "<%= endTimeStr %>" maxlength="4" pattern="([01][0-9]|2[0-3])[0-5][0-9]" oninput="validateTimeInput(this)"/></td>
				<!-- 休憩時間 (分) -->
				<td><input type = "text" name = "work_rt_<%= kintai.getKinmuYmd() %>" value = "<%= breakTimeStr %>" maxlength="3" pattern="[0-9]{1,3}"/></td>
				<!-- 勤務時間 -->
				<td><%= String.format ( "%.2f", dailyWorkTime ) %>時間</td> <!-- 1日の勤務時間を計算し、小数点以下2桁で表示 -->
				<!-- 残業時間 -->
				<td><%= String.format ( "%.2f", kintai.getOvertimeMinutes() / 60.0 ) %>時間</td>
			</tr>
			<%
					} // forループの閉じタグ
				} // if (yearStr != null && monthStr != null && showTable) の閉じタグ
			%>
			<%
				// 合計勤務時間と合計残業時間の表示
				// showTableがtrue (kintaiListが空でない) かつ、どちらかの合計が0より大きい場合のみ表示
				if (showTable && (totalMonthlyWorkTime > 0 || totalOvertimeMinutes > 0)) {
			%>
			<tr>
				<td colspan = "5" align = "right">合計時間:</td>
				<!-- 月の合計勤務時間 -->
				<td><%= String.format ( "%.2f", totalMonthlyWorkTime ) %>時間</td>
				<!-- 月の合計残業時間 -->
				<td><%= String.format ( "%.2f", totalOvertimeMinutes / 60.0 ) %>時間</td>
			</tr>
			<%
				}
			%>
		</table>
	</form>
	<% } %>
	
	<script>
		// 年入力フィールドの文字数制限
		document.addEventListener('DOMContentLoaded', function() {
			const yearInput = document.getElementById('yearInput');
			yearInput.addEventListener('input', function() {
				if (this.value.length > 4) {
					this.value = this.value.slice(0, 4);
				}
			});
		});

		// 時刻入力フィールドのバリデーション
		function validateTimeInput(inputElement) {
			let value = inputElement.value;
			// 数字以外を削除
			value = value.replace(/[^0-9]/g, '');

			if (value.length > 0) {
				const firstChar = parseInt(value.charAt(0));
				if (firstChar < 0 || firstChar > 2) { // 1桁目は0, 1, 2のみ
					value = ''; // 無効な場合はクリア
				}
			}
			if (value.length > 2) {
				const thirdChar = parseInt(value.charAt(2));
				if (thirdChar < 0 || thirdChar > 5) { // 3桁目は0～5のみ
					value = value.slice(0, 2) + value.charAt(3); // 3桁目を削除
				}
			}
			inputElement.value = value;
		}

		function confirmDelete() //削除確認ダイアログを表示する関数
		{
			//確認ダイアログを表示
			return confirm("削除してよろしいですか？") //ユーザーに確認
		}
	</script>
	
</body>
</html>
