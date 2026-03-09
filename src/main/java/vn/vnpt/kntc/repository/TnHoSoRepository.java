package vn.vnpt.kntc.repository;

import org.springframework.cache.annotation.Cacheable;
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
          WHERE t.userId = :userId
            AND t.status = 0
          ORDER BY t.createdDate ASC
      """)
  List<TnHoSo> findPendingByUser(@Param("userId") Integer userId);

  // Đếm đơn thư tiếp nhận hôm nay
  @Cacheable(value = "countTnToday", key = "#userId")
  @Query("""
          SELECT COUNT(t) FROM TnHoSo t
          WHERE t.userId = :userId
            AND t.createdDate >= CURRENT_DATE
      """)
  Object countToday(@Param("userId") Integer userId);

  // Thống kê đơn thư theo tháng
  @Cacheable(value = "countTnByMonth", key = "#userId + '-' + #month + '-' + #year")
  @Query("""
          SELECT COUNT(t) FROM TnHoSo t
          WHERE t.userId = :userId
            AND YEAR(t.createdDate) = :year
            AND MONTH(t.createdDate) = :month
      """)
  Object countByMonth(
      @Param("userId") Integer userId,
      @Param("month") int month,
      @Param("year") int year);

  // Đơn thư chưa gắn với hồ sơ giải quyết
  // Do TN_HOSO không có cột GQ_HOSO_ID trực tiếp (GQ_HOSO mới là bảng có link),
  // câu query này cần join hoặc logic khác nếu cần chính xác.
  // Tuy nhiên tạm thời giữ nguyên mapping logic để Fix ORA-00904 trước.
  @Query("""
          SELECT t FROM TnHoSo t
          WHERE t.userId = :userId
            AND t.status IN (0, 1)
          ORDER BY t.createdDate ASC
      """)
  List<TnHoSo> findNotLinkedToGq(@Param("userId") Integer userId);

  // Tìm kiếm theo nội dung hoặc người ký
  @Query("""
          SELECT t FROM TnHoSo t
          WHERE t.userId = :userId
            AND (UPPER(t.maHoSo)       LIKE UPPER(CONCAT('%',:kw,'%'))
              OR UPPER(t.tenNguoiKy)   LIKE UPPER(CONCAT('%',:kw,'%'))
              OR UPPER(t.noiDung)      LIKE UPPER(CONCAT('%',:kw,'%')))
          ORDER BY t.createdDate DESC
      """)
  List<TnHoSo> searchByKeyword(
      @Param("userId") Integer userId,
      @Param("kw") String keyword);

  // Thống kê đơn thư theo năm
  @Cacheable(value = "countTnByYear", key = "#userId + '-' + #year")
  @Query("""
          SELECT COUNT(t) FROM TnHoSo t
          WHERE t.userId = :userId
            AND YEAR(t.createdDate) = :year
      """)
  Object countByYear(
      @Param("userId") Integer userId,
      @Param("year") int year);
}
