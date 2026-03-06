package vn.vnpt.kntc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.vnpt.kntc.model.TnHoSo;

import java.util.List;

@Repository
public interface TnHoSoRepository extends JpaRepository<TnHoSo, Integer> {

  // Đơn thư chờ phân công xử lý (status = 0)
  @Query("""
          SELECT t FROM TnHoSo t
          WHERE t.nguoiTiepNhanId = :userId
            AND t.status = 0
          ORDER BY t.ngayTiepNhan ASC
      """)
  List<TnHoSo> findPendingByUser(@Param("userId") Integer userId);

  // Đếm đơn thư tiếp nhận hôm nay
  @Query(value = """
          SELECT COUNT(*) FROM TN_HOSO
          WHERE NGUOI_TIEP_NHAN_ID = :userId
            AND TRUNC(NGAY_TIEP_NHAN) = TRUNC(CURRENT_TIMESTAMP)
      """, nativeQuery = true)
  long countToday(@Param("userId") Integer userId);

  // Thống kê đơn thư theo tháng
  @Query(value = """
          SELECT COUNT(*) FROM TN_HOSO
          WHERE NGUOI_TIEP_NHAN_ID = :userId
            AND EXTRACT(MONTH FROM NGAY_TIEP_NHAN) = :month
            AND EXTRACT(YEAR  FROM NGAY_TIEP_NHAN) = :year
      """, nativeQuery = true)
  long countByMonth(
      @Param("userId") Integer userId,
      @Param("month") int month,
      @Param("year") int year);

  // Đơn thư chưa gắn với hồ sơ giải quyết
  @Query("""
          SELECT t FROM TnHoSo t
          WHERE t.nguoiTiepNhanId = :userId
            AND t.gqHoSoId IS NULL
            AND t.status IN (0, 1)
          ORDER BY t.ngayTiepNhan ASC
      """)
  List<TnHoSo> findNotLinkedToGq(@Param("userId") Integer userId);

  // Tìm kiếm theo nội dung hoặc người ký
  @Query("""
          SELECT t FROM TnHoSo t
          WHERE t.nguoiTiepNhanId = :userId
            AND (UPPER(t.maHoSo)       LIKE UPPER(CONCAT('%',:kw,'%'))
              OR UPPER(t.tenNguoiKy)   LIKE UPPER(CONCAT('%',:kw,'%'))
              OR UPPER(t.noiDung)      LIKE UPPER(CONCAT('%',:kw,'%')))
          ORDER BY t.ngayTiepNhan DESC
      """)
  List<TnHoSo> searchByKeyword(
      @Param("userId") Integer userId,
      @Param("kw") String keyword);
}
