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

    @Column(name = "DOI_TUONG_GUI", length = 255)
    private String tenNguoiKy;

    @Column(name = "TOM_TAT_NOI_DUNG", length = 4000)
    private String noiDung;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    /**
     * 0 = Mới/Chờ phân công
     * 1 = Đã phân công
     * 2 = Đang xử lý
     * 3 = Hoàn thành
     */
    @Column(name = "HO_SO_STATUS")
    private Integer status;

    @Column(name = "CURRENT_USER_ID")
    private Integer userId;

    @Column(name = "NGUOI_TIEP_DAN_NAME", length = 255)
    private String nguoiTiepNhanTen;

    @Column(name = "LOAI_HO_SO")
    private Integer loaiDonId;

    @Column(name = "LOAI_DON_TEN", length = 255)
    private String loaiDonTen;

    @Column(name = "DIA_BAN_NOI_DUNG_CT", length = 500)
    private String diaChiNguoiKy;

    @Column(name = "LAST_MODIFIED_DATE")
    private LocalDateTime ngayCapNhat;
}
