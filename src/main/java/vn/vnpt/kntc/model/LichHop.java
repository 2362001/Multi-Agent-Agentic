package vn.vnpt.kntc.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lịch họp / Lịch làm việc — bảng LICH_HOP
 */
@Entity
@Table(name = "LICH_HOP")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichHop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "TIEU_DE", length = 500)
    private String tieuDe;

    @Column(name = "NOI_DUNG", length = 4000)
    private String noiDung;

    @Column(name = "THOI_GIAN_BAT_DAU")
    private LocalDateTime thoiGianBatDau;

    @Column(name = "THOI_GIAN_KET_THUC")
    private LocalDateTime thoiGianKetThuc;

    @Column(name = "DIA_DIEM", length = 500)
    private String diaDiem;

    @Column(name = "NGUOI_THAM_DU_ID")
    private Integer nguoiThamDuId;

    @Column(name = "NGUOI_THAM_DU_TEN", length = 255)
    private String nguoiThamDuTen;

    @Column(name = "NGUOI_TO_CHUC_ID")
    private Integer nguoiToChucId;

    @Column(name = "NGUOI_TO_CHUC_TEN", length = 255)
    private String nguoiToChucTen;

    /**
     * 0 = Lên lịch
     * 1 = Đang diễn ra
     * 2 = Hoàn thành
     * 3 = Huỷ
     */
    @Column(name = "STATUS")
    private Integer status;

    @Column(name = "LOAI_LICH")
    private Integer loaiLich;    // 1=Họp, 2=Công tác, 3=Hội thảo

    @Column(name = "NGAY_TAO")
    private LocalDateTime ngayTao;
}
