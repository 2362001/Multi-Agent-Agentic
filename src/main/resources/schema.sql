-- ============================================================
-- KNTC Multi-Agent System — Oracle DDL
-- Tạo bảng + dữ liệu mẫu để test
-- ============================================================

-- ── GQ_HOSO ──────────────────────────────────────────────────
CREATE TABLE GQ_HOSO (
    ID                      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    TN_MA_HO_SO             VARCHAR2(100),
    GQ_MA_HO_SO             VARCHAR2(100),
    TN_HOSO_ID              NUMBER,
    NGUOI_PHAN_CONG         VARCHAR2(100),
    NGUOI_PHAN_CONG_ID      NUMBER,
    NGAY_PHAN_CONG          TIMESTAMP,
    CO_QUAN_PHAN_CONG_ID    NUMBER,
    CO_QUAN_PHAN_CONG_TEN   VARCHAR2(255),
    NOI_DUNG_PHAN_CONG      VARCHAR2(4000),
    STATUS                  NUMBER(2)    DEFAULT 0,
    LOAI_PHAN_CONG          NUMBER,
    CO_QUAN_DUOC_PC_ID      NUMBER,
    CO_QUAN_DUOC_PC_TEN     VARCHAR2(255),
    CHUC_VU_NGUOI_PC_ID     NUMBER,
    CHUC_VU_NGUOI_PC_TEN    VARCHAR2(255),
    CQ_NGUOI_PC_ID          NUMBER,
    CQ_NGUOI_PC_TEN         VARCHAR2(255),
    CB_DUOC_PC_ID           NUMBER,
    CB_DUOC_PC_TEN          VARCHAR2(255),
    NOI_DUNG_DUOC_PHAN_CONG VARCHAR2(4000),
    LOAI_TRINH_KY           NUMBER,
    THOI_GIAN_TRINH_KY      TIMESTAMP,
    CURRENT_USER_ID         NUMBER,
    NGAY_PHE_DUYET_KTR_DK   TIMESTAMP,
    NGUOI_PHE_DUYET_KTR_DK  VARCHAR2(255),
    NGAY_PHE_DUYET_GQKNTC   TIMESTAMP,
    LOAI_DON_ID             NUMBER,
    THANH_LAP_TO_XAC_MINH   NUMBER(1)    DEFAULT 0
);

CREATE INDEX IDX_GQ_CB_DUOC_PC    ON GQ_HOSO(CB_DUOC_PC_ID);
CREATE INDEX IDX_GQ_NGAY_PC       ON GQ_HOSO(NGAY_PHAN_CONG);
CREATE INDEX IDX_GQ_DEADLINE      ON GQ_HOSO(NGAY_PHE_DUYET_GQKNTC);
CREATE INDEX IDX_GQ_STATUS        ON GQ_HOSO(STATUS);

-- ── TN_HOSO ──────────────────────────────────────────────────
CREATE TABLE TN_HOSO (
    ID                      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    MA_HO_SO                VARCHAR2(100),
    TEN_NGUOI_KY            VARCHAR2(255),
    NOI_DUNG                VARCHAR2(4000),
    NGAY_TIEP_NHAN          TIMESTAMP,
    STATUS                  NUMBER(2)    DEFAULT 0,
    NGUOI_TIEP_NHAN_ID      NUMBER,
    NGUOI_TIEP_NHAN_TEN     VARCHAR2(255),
    LOAI_DON_ID             NUMBER,
    LOAI_DON_TEN            VARCHAR2(255),
    DIA_CHI_NGUOI_KY        VARCHAR2(500),
    GQ_HOSO_ID              NUMBER,
    NGAY_TAO                TIMESTAMP    DEFAULT SYSTIMESTAMP,
    NGAY_CAP_NHAT           TIMESTAMP    DEFAULT SYSTIMESTAMP
);

CREATE INDEX IDX_TN_NGUOI_TN      ON TN_HOSO(NGUOI_TIEP_NHAN_ID);
CREATE INDEX IDX_TN_NGAY          ON TN_HOSO(NGAY_TIEP_NHAN);

-- ── LICH_HOP ─────────────────────────────────────────────────
CREATE TABLE LICH_HOP (
    ID                      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    TIEU_DE                 VARCHAR2(500),
    NOI_DUNG                VARCHAR2(4000),
    THOI_GIAN_BAT_DAU       TIMESTAMP,
    THOI_GIAN_KET_THUC      TIMESTAMP,
    DIA_DIEM                VARCHAR2(500),
    NGUOI_THAM_DU_ID        NUMBER,
    NGUOI_THAM_DU_TEN       VARCHAR2(255),
    NGUOI_TO_CHUC_ID        NUMBER,
    NGUOI_TO_CHUC_TEN       VARCHAR2(255),
    STATUS                  NUMBER(2)    DEFAULT 0,
    LOAI_LICH               NUMBER(2)    DEFAULT 1,
    NGAY_TAO                TIMESTAMP    DEFAULT SYSTIMESTAMP
);

CREATE INDEX IDX_LICH_NGUOI       ON LICH_HOP(NGUOI_THAM_DU_ID);
CREATE INDEX IDX_LICH_THOI_GIAN   ON LICH_HOP(THOI_GIAN_BAT_DAU);

-- ── THONG_BAO ────────────────────────────────────────────────
CREATE TABLE THONG_BAO (
    ID                      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    TIEU_DE                 VARCHAR2(500),
    NOI_DUNG                VARCHAR2(4000),
    LOAI                    NUMBER(2)    DEFAULT 1,
    NGUOI_NHAN_ID           NUMBER,
    NGUOI_NHAN_TEN          VARCHAR2(255),
    DA_DOC                  NUMBER(1)    DEFAULT 0,
    DO_UU_TIEN              NUMBER(2)    DEFAULT 2,
    NGAY_TAO                TIMESTAMP    DEFAULT SYSTIMESTAMP,
    NGUOI_TAO_ID            NUMBER,
    REF_HOSO_ID             NUMBER,
    REF_HOSO_MA             VARCHAR2(100)
);

CREATE INDEX IDX_TB_NGUOI_NHAN    ON THONG_BAO(NGUOI_NHAN_ID);
CREATE INDEX IDX_TB_DA_DOC        ON THONG_BAO(DA_DOC);

-- ============================================================
-- DỮ LIỆU MẪU (userId = 123)
-- ============================================================

-- GQ_HOSO samples
INSERT INTO GQ_HOSO (TN_MA_HO_SO, GQ_MA_HO_SO, CB_DUOC_PC_ID, CB_DUOC_PC_TEN,
    NGAY_PHAN_CONG, NGAY_PHE_DUYET_GQKNTC, STATUS, NOI_DUNG_PHAN_CONG)
VALUES ('TN-2026-001', 'GQ-2026-001', 123, 'Nguyễn Văn A',
    SYSTIMESTAMP, SYSTIMESTAMP - 2, 1, 'Giải quyết khiếu nại đất đai');

INSERT INTO GQ_HOSO (TN_MA_HO_SO, GQ_MA_HO_SO, CB_DUOC_PC_ID, CB_DUOC_PC_TEN,
    NGAY_PHAN_CONG, NGAY_PHE_DUYET_GQKNTC, STATUS, NOI_DUNG_PHAN_CONG)
VALUES ('TN-2026-002', 'GQ-2026-002', 123, 'Nguyễn Văn A',
    SYSTIMESTAMP, SYSTIMESTAMP, 1, 'Xử lý tố cáo cán bộ xã');

INSERT INTO GQ_HOSO (TN_MA_HO_SO, GQ_MA_HO_SO, CB_DUOC_PC_ID, CB_DUOC_PC_TEN,
    NGAY_PHAN_CONG, NGAY_PHE_DUYET_GQKNTC, STATUS, NOI_DUNG_PHAN_CONG,
    THANH_LAP_TO_XAC_MINH)
VALUES ('TN-2026-003', 'GQ-2026-003', 123, 'Nguyễn Văn A',
    SYSTIMESTAMP, SYSTIMESTAMP + 2, 0, 'Xác minh khiếu nại đền bù', 1);

-- LICH_HOP samples
INSERT INTO LICH_HOP (TIEU_DE, THOI_GIAN_BAT_DAU, THOI_GIAN_KET_THUC,
    DIA_DIEM, NGUOI_THAM_DU_ID, STATUS)
VALUES ('Họp giao ban tuần', TRUNC(SYSDATE) + 8.5/24, TRUNC(SYSDATE) + 9.5/24,
    'Phòng họp tầng 3', 123, 0);

INSERT INTO LICH_HOP (TIEU_DE, THOI_GIAN_BAT_DAU, THOI_GIAN_KET_THUC,
    DIA_DIEM, NGUOI_THAM_DU_ID, STATUS)
VALUES ('Họp tiến độ giải quyết KNTC', TRUNC(SYSDATE) + 14/24, TRUNC(SYSDATE) + 15/24,
    'Phòng họp tầng 2', 123, 0);

-- THONG_BAO samples
INSERT INTO THONG_BAO (TIEU_DE, NOI_DUNG, LOAI, NGUOI_NHAN_ID, DA_DOC, DO_UU_TIEN)
VALUES ('Nhắc hạn nộp báo cáo tháng', 'Hạn nộp báo cáo tháng 3 là ngày mai 06/03/2026',
    2, 123, 0, 3);

INSERT INTO THONG_BAO (TIEU_DE, NOI_DUNG, LOAI, NGUOI_NHAN_ID, DA_DOC, DO_UU_TIEN,
    REF_HOSO_MA)
VALUES ('Cảnh báo: GQ-2026-001 quá hạn', 'Hồ sơ GQ-2026-001 đã quá hạn 2 ngày',
    3, 123, 0, 3, 'GQ-2026-001');

INSERT INTO THONG_BAO (TIEU_DE, NOI_DUNG, LOAI, NGUOI_NHAN_ID, DA_DOC, DO_UU_TIEN)
VALUES ('Thông báo họp giao ban', 'Họp giao ban tuần lúc 8:30 hôm nay',
    1, 123, 0, 2);

COMMIT;
