package vn.vnpt.kntc.repository;

import vn.vnpt.kntc.model.GqHoSo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GqHoSoRepository extends JpaRepository<GqHoSo, Integer> {

  // ── Hồ sơ được phân công hôm nay ────────────────────────────
  @Query("""
          SELECT h FROM GqHoSo h
          WHERE h.cbDuocPcId = :userId
            AND h.ngayPhanCong >= CURRENT_DATE
            AND h.status NOT IN (3, 4)
          ORDER BY h.ngayPhanCong DESC
      """)
  List<GqHoSo> findAssignedToday(@Param("userId") Integer userId);

  // ── Đếm hồ sơ được phân công hôm nay ────────────────────────
  @Query("""
          SELECT COUNT(h) FROM GqHoSo h
          WHERE h.cbDuocPcId = :userId
            AND h.ngayPhanCong >= CURRENT_DATE
      """)
  long countAssignedToday(@Param("userId") Integer userId);

  // ── Hồ sơ quá hạn GQKNTC ────────────────────────────────────
  @Query("""
          SELECT h FROM GqHoSo h
          WHERE h.cbDuocPcId = :userId
            AND h.ngayPheDuyetGqkntc < CURRENT_TIMESTAMP
            AND h.status NOT IN (3, 4)
          ORDER BY h.ngayPheDuyetGqkntc ASC
      """)
  List<GqHoSo> findOverdue(@Param("userId") Integer userId);

  // ── Hồ sơ sắp đến hạn GQKNTC ────────────────────────────────
  @Query("""
          SELECT h FROM GqHoSo h
          WHERE h.cbDuocPcId = :userId
            AND h.ngayPheDuyetGqkntc BETWEEN CURRENT_TIMESTAMP AND :cutoff
            AND h.status NOT IN (3, 4)
          ORDER BY h.ngayPheDuyetGqkntc ASC
      """)
  List<GqHoSo> findDueSoon(
      @Param("userId") Integer userId,
      @Param("cutoff") LocalDateTime cutoff);

  // ── Hồ sơ quá hạn kiểm tra điều kiện ────────────────────────
  @Query("""
          SELECT h FROM GqHoSo h
          WHERE h.cbDuocPcId = :userId
            AND h.ngayPheDuyetKtrDk < CURRENT_TIMESTAMP
            AND h.status NOT IN (3, 4)
      """)
  List<GqHoSo> findOverdueKtrDk(@Param("userId") Integer userId);

  // ── Hồ sơ chờ trình ký ───────────────────────────────────────
  @Query("""
          SELECT h FROM GqHoSo h
          WHERE h.cbDuocPcId = :userId
            AND h.loaiTrinhKy IS NOT NULL
            AND h.thoiGianTrinhKy > CURRENT_TIMESTAMP
            AND h.status NOT IN (3, 4)
          ORDER BY h.thoiGianTrinhKy ASC
      """)
  List<GqHoSo> findPendingSignature(@Param("userId") Integer userId);

  // ── Đếm theo filter ──────────────────────────────────────────
  @Query("""
          SELECT COUNT(h) FROM GqHoSo h
          WHERE h.cbDuocPcId = :userId
            AND h.status NOT IN (3, 4)
      """)
  long countAll(@Param("userId") Integer userId);

  @Query("""
          SELECT COUNT(h) FROM GqHoSo h
          WHERE h.cbDuocPcId = :userId
            AND h.status IN (0, 1)
      """)
  long countPending(@Param("userId") Integer userId);

  @Query("""
          SELECT COUNT(h) FROM GqHoSo h
          WHERE h.cbDuocPcId = :userId
            AND h.ngayPheDuyetGqkntc < CURRENT_TIMESTAMP
            AND h.status NOT IN (3, 4)
      """)
  long countOverdue(@Param("userId") Integer userId);

  // ── Thống kê theo trạng thái ──────────────────────────────────
  @Query("""
          SELECT h.status, COUNT(h) FROM GqHoSo h
          WHERE h.cbDuocPcId = :userId
          GROUP BY h.status
      """)
  List<Object[]> countGroupByStatus(@Param("userId") Integer userId);

  // ── Thống kê theo tháng ───────────────────────────────────────
  @Query(value = """
          SELECT COUNT(*) FROM GQ_HOSO
          WHERE CB_DUOC_PC_ID = :userId
            AND EXTRACT(MONTH FROM NGAY_PHAN_CONG) = :month
            AND EXTRACT(YEAR FROM NGAY_PHAN_CONG) = :year
      """, nativeQuery = true)
  long countByMonth(
      @Param("userId") Integer userId,
      @Param("month") int month,
      @Param("year") int year);

  // ── Tìm kiếm theo mã hoặc nội dung ───────────────────────────
  @Query("""
          SELECT h FROM GqHoSo h
          WHERE h.cbDuocPcId = :userId
            AND (UPPER(h.tnMaHoSo)            LIKE UPPER(CONCAT('%',:kw,'%'))
              OR UPPER(h.gqMaHoSo)            LIKE UPPER(CONCAT('%',:kw,'%'))
              OR UPPER(h.noiDungPhanCong)      LIKE UPPER(CONCAT('%',:kw,'%'))
              OR UPPER(h.noiDungDuocPhanCong)  LIKE UPPER(CONCAT('%',:kw,'%')))
          ORDER BY h.ngayPhanCong DESC
      """)
  List<GqHoSo> searchByKeyword(
      @Param("userId") Integer userId,
      @Param("kw") String keyword);

  // ── Hồ sơ cần thành lập tổ xác minh ──────────────────────────
  @Query("""
          SELECT h FROM GqHoSo h
          WHERE h.cbDuocPcId = :userId
            AND h.thanhLapToXacMinh = 1
            AND h.status NOT IN (3, 4)
      """)
  List<GqHoSo> findNeedToXacMinh(@Param("userId") Integer userId);
}
