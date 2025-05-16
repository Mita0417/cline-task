package com.kenshu.kintai.servlet;

import com.kenshu.kintai.dao.KintaiDao;
import com.kenshu.kintai.entity.Kintai;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/kintai")
public class KintaiServlet extends HttpServlet {

    private KintaiDao kintaiDao = new KintaiDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 初期表示
        // 現在の年と月を取得してJSPに渡す
        java.time.LocalDate now = java.time.LocalDate.now();
        request.setAttribute("year", String.valueOf(now.getYear()));
        request.setAttribute("month", String.format("%02d", now.getMonthValue()));

        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        String message = "";
        List<Kintai> kintaiList = null;

        if ("search".equals(action)) {
            // 検索アクション
            String year = request.getParameter("year");
            String month = request.getParameter("month");

            // 入力チェック（簡易）
            if (year == null || year.isEmpty() || month == null || month.isEmpty()) {
                message = "年と月を入力してください。";
            } else {
                // 検索処理
                String yearMonth = year + month;
                kintaiList = kintaiDao.findByYearMonth(yearMonth);

                // 検索結果が空の場合、その月の日付リストを生成
                if (kintaiList == null || kintaiList.isEmpty()) {
                    kintaiList = new java.util.ArrayList<>();
                    try {
                        java.time.YearMonth ym = java.time.YearMonth.of(Integer.parseInt(year), Integer.parseInt(month));
                        int daysInMonth = ym.lengthOfMonth();
                        for (int i = 1; i <= daysInMonth; i++) {
                            com.kenshu.kintai.entity.Kintai kintai = new com.kenshu.kintai.entity.Kintai();
                            kintai.setKinmuYmd(yearMonth + String.format("%02d", i));
                            kintaiList.add(kintai);
                        }
                    } catch (java.time.format.DateTimeParseException | NumberFormatException e) {
                        e.printStackTrace();
                        message = "年月形式が不正です。";
                        kintaiList = null; // エラーの場合はリストをクリア
                    }
                }

                message = "検索が完了しました。";
            }
        } else if ("register".equals(action)) {
            // 登録アクション
            String kinmuYmd = request.getParameter("kinmuYmd");
            String workSt = request.getParameter("workSt");
            String workEd = request.getParameter("workEd");
            String workRt = request.getParameter("workRt");

            // 入力チェックと登録処理
            if (kinmuYmd == null || kinmuYmd.isEmpty() || workSt == null || workSt.isEmpty() || workEd == null || workEd.isEmpty() || workRt == null || workRt.isEmpty()) {
                 message = "全ての項目を入力してください。";
            } else {
                Kintai kintai = new Kintai();
                kintai.setKinmuYmd(kinmuYmd);
                kintai.setWorkSt(workSt);
                kintai.setWorkEd(workEd);
                kintai.setWorkRt(workRt);

                if (kintaiDao.insert(kintai)) {
                    message = "登録が完了しました。";
                } else {
                    message = "登録に失敗しました。";
                }
            }

        } else if ("delete".equals(action)) {
            // 削除アクション
            String kinmuYmd = request.getParameter("kinmuYmd");

            // 削除処理（後で実装）
            if (kinmuYmd == null || kinmuYmd.isEmpty()) {
                message = "削除対象の勤務日が指定されていません。";
            } else {
                if (kintaiDao.delete(kinmuYmd)) {
                    message = "削除が完了しました。";
                } else {
                    message = "削除に失敗しました。";
                }
            }
        } else if ("edit".equals(action)) {
            // 編集アクション（後で実装）
            message = "編集機能は未実装です。";
        }

        // 登録・削除後は再度検索を実行して最新のデータを表示
        String year = request.getParameter("year");
        String month = request.getParameter("month");
        if (year != null && !year.isEmpty() && month != null && !month.isEmpty()) {
             kintaiList = kintaiDao.findByYearMonth(year + month);
        }


        request.setAttribute("message", message);
        request.setAttribute("kintaiList", kintaiList); // 検索結果をセット
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}
