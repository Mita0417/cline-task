package com.kenshu.kintai.dao;

import com.kenshu.kintai.entity.Kintai;
import com.kenshu.kintai.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class KintaiDao {

    public Kintai findByKinmuYmd(String kinmuYmd) {
        String sql = "SELECT kinmu_ymd, work_st, work_ed, work_rt FROM tbl_kintai WHERE kinmu_ymd = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, kinmuYmd);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Kintai kintai = new Kintai();
                    kintai.setKinmuYmd(rs.getString("kinmu_ymd"));
                    kintai.setWorkSt(rs.getString("work_st"));
                    kintai.setWorkEd(rs.getString("work_ed"));
                    kintai.setWorkRt(rs.getString("work_rt"));
                    return kintai;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // エラーハンドリングを適切に行う
        }
        return null;
    }

    public List<Kintai> findByYearMonth(String yearMonth) {
        List<Kintai> kintaiList = new ArrayList<>();
        String sql = "SELECT kinmu_ymd, work_st, work_ed, work_rt FROM tbl_kintai WHERE kinmu_ymd LIKE ? ORDER BY kinmu_ymd";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, yearMonth + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Kintai kintai = new Kintai();
                    kintai.setKinmuYmd(rs.getString("kinmu_ymd"));
                    kintai.setWorkSt(rs.getString("work_st"));
                    kintai.setWorkEd(rs.getString("work_ed"));
                    kintai.setWorkRt(rs.getString("work_rt"));
                    kintaiList.add(kintai);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // エラーハンドリングを適切に行う
        }
        return kintaiList;
    }

    public boolean insert(Kintai kintai) {
        String sql = "INSERT INTO tbl_kintai (kinmu_ymd, work_st, work_ed, work_rt) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, kintai.getKinmuYmd());
            pstmt.setString(2, kintai.getWorkSt());
            pstmt.setString(3, kintai.getWorkEd());
            pstmt.setString(4, kintai.getWorkRt());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            // エラーハンドリングを適切に行う
            return false;
        }
    }

    public boolean update(Kintai kintai) {
        String sql = "UPDATE tbl_kintai SET work_st = ?, work_ed = ?, work_rt = ? WHERE kinmu_ymd = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, kintai.getWorkSt());
            pstmt.setString(2, kintai.getWorkEd());
            pstmt.setString(3, kintai.getWorkRt());
            pstmt.setString(4, kintai.getKinmuYmd());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            // エラーハンドリングを適切に行う
            return false;
        }
    }

    public boolean delete(String kinmuYmd) {
        String sql = "DELETE FROM tbl_kintai WHERE kinmu_ymd = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, kinmuYmd);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            // エラーハンドリングを適切に行う
            return false;
        }
    }
}
