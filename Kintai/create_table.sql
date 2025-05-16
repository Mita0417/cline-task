USE kenshu_db;

CREATE TABLE tbl_kintai (
    kinmu_ymd CHAR(8) NOT NULL COMMENT '勤務日',
    work_st CHAR(4) COMMENT '出勤時刻',
    work_ed CHAR(4) COMMENT '退勤時刻',
    work_rt CHAR(3) COMMENT '休憩時間',
    PRIMARY KEY (kinmu_ymd)
) COMMENT '勤怠';
