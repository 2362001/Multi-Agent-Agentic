package vn.vnpt.kntc.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thông báo / Nhắc nhở — bảng THONG_BAO
 */
@Entity
@Table(name = "THONG_BAO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThongBao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "TIEU_DE", length = 500)
    private String tieuDe;

    @Column(name = "NOI_DUNG", length = 4000)
    private String noiDung;

    /**
     * 1 = Thông báo chung
     * 2 = Nhắc nhở
     * 3 = Cảnh báo hạn chót
     */
    @Column(name = "LOAI")
    private Integer loai;

    @Column(name = "NGUOI_NHAN_ID")
    private Integer nguoiNhanId;

    @Column(name = "NGUOI_NHAN_TEN", length = 255)
    private String nguoiNhanTen;

    /**
     * 0 = Chưa đọc
     * 1 = Đã đọc
     */
    @Column(name = "DA_DOC")
    private Integer daDoc;

    /**
     * 1 = Thấp
     * 2 = Trung bình
     * 3 = Cao / Quan trọng
     */
    @Column(name = "DO_UU_TIEN")
    private Integer doUuTien;

    @Column(name = "NGAY_TAO")
    private LocalDateTime ngayTao;

    @Column(name = "NGUOI_TAO_ID")
    private Integer nguoiTaoId;

    // Liên kết tới hồ sơ liên quan (nếu có)
    @Column(name = "REF_HOSO_ID")
    private Integer refHoSoId;

    @Column(name = "REF_HOSO_MA", length = 100)
    private String refHoSoMa;
}
