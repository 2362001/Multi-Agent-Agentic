package vn.vnpt.kntc.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Đơn thư tiếp nhận — bảng TN_HOSO
 */
@Entity
@Table(name = "TN_HOSO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TnHoSo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MA_HO_SO", length = 100)
    private String maHoSo;

    @Column(name = "TEN_NGUOI_KY", length = 255)
    private String tenNguoiKy;

    @Column(name = "NOI_DUNG", length = 4000)
    private String noiDung;

    @Column(name = "NGAY_TIEP_NHAN")
    private LocalDateTime ngayTiepNhan;

    /**
     * 0 = Mới/Chờ phân công
     * 1 = Đã phân công
     * 2 = Đang xử lý
     * 3 = Hoàn thành
     */
    @Column(name = "STATUS")
    private Integer status;

    @Column(name = "NGUOI_TIEP_NHAN_ID")
    private Integer nguoiTiepNhanId;

    @Column(name = "NGUOI_TIEP_NHAN_TEN", length = 255)
    private String nguoiTiepNhanTen;

    @Column(name = "LOAI_DON_ID")
    private Integer loaiDonId;

    @Column(name = "LOAI_DON_TEN", length = 255)
    private String loaiDonTen;

    @Column(name = "DIA_CHI_NGUOI_KY", length = 500)
    private String diaChiNguoiKy;

    @Column(name = "GQ_HOSO_ID")
    private Integer gqHoSoId;

    @Column(name = "NGAY_TAO")
    private LocalDateTime ngayTao;

    @Column(name = "NGAY_CAP_NHAT")
    private LocalDateTime ngayCapNhat;
}
