package vn.vnpt.kntc.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "GQ_HOSO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GqHoSo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "TN_MA_HO_SO", length = 100)
    private String tnMaHoSo;

    @Column(name = "GQ_MA_HO_SO", length = 100)
    private String gqMaHoSo;

    @Column(name = "TN_HOSO_ID")
    private Integer tnHoSoId;

    @Column(name = "NGUOI_PHAN_CONG", length = 100)
    private String nguoiPhanCong;

    @Column(name = "NGUOI_PHAN_CONG_ID")
    private Integer nguoiPhanCongId;

    @Column(name = "NGAY_PHAN_CONG")
    private LocalDateTime ngayPhanCong;

    @Column(name = "CO_QUAN_PHAN_CONG_ID")
    private Integer coQuanPhanCongId;

    @Column(name = "CO_QUAN_PHAN_CONG_TEN", length = 255)
    private String coQuanPhanCongTen;

    @Column(name = "NOI_DUNG_PHAN_CONG", length = 4000)
    private String noiDungPhanCong;

    @Column(name = "STATUS")
    private Integer status;

    @Column(name = "LOAI_PHAN_CONG")
    private Integer loaiPhanCong;

    @Column(name = "CO_QUAN_DUOC_PC_ID")
    private Integer coQuanDuocPcId;

    @Column(name = "CO_QUAN_DUOC_PC_TEN")
    private String coQuanDuocPcTen;

    @Column(name = "CHUC_VU_NGUOI_PC_ID")
    private Integer chucVuNguoiPcId;

    @Column(name = "CHUC_VU_NGUOI_PC_TEN")
    private String chucVuNguoiPcTen;

    @Column(name = "CQ_NGUOI_PC_ID")
    private Integer cqNguoiPcId;

    @Column(name = "CQ_NGUOI_PC_TEN")
    private String cqNguoiPcTen;

    @Column(name = "CB_DUOC_PC_ID")
    private Integer cbDuocPcId;

    @Column(name = "CB_DUOC_PC_TEN")
    private String cbDuocPcTen;

    @Column(name = "NOI_DUNG_DUOC_PHAN_CONG")
    private String noiDungDuocPhanCong;

    @Column(name = "LOAI_TRINH_KY")
    private Integer loaiTrinhKy;

    @Column(name = "THOI_GIAN_TRINH_KY")
    private LocalDateTime thoiGianTrinhKy;

    @Column(name = "CURRENT_USER_ID")
    private Integer currentUserId;

    @Column(name = "NGAY_PHE_DUYET_KTR_DK")
    private LocalDateTime ngayPheDuyetKtrDk;

    @Column(name = "NGUOI_PHE_DUYET_KTR_DK")
    private String nguoiPheDuyetKtrDk;

    @Column(name = "NGAY_PHE_DUYET_GQKNTC")
    private LocalDateTime ngayPheDuyetGqkntc;

    @Column(name = "LOAI_DON_ID")
    private Integer loaiDonId;

    @Column(name = "THANH_LAP_TO_XAC_MINH")
    private Integer thanhLapToXacMinh;
}
