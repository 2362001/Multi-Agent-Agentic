package vn.vnpt.kntc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.vnpt.kntc.model.LichHop;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LichHopRepository extends JpaRepository<LichHop, Integer> {

  // Lịch họp hôm nay
  @Query(value = """
          SELECT * FROM LICH_HOP
          WHERE NGUOI_THAM_DU_ID = :userId
            AND TRUNC(THOI_GIAN_BAT_DAU) = TRUNC(CURRENT_TIMESTAMP)
            AND STATUS NOT IN (2, 3)
          ORDER BY THOI_GIAN_BAT_DAU ASC
      """, nativeQuery = true)
  List<LichHop> findTodayMeetings(@Param("userId") Integer userId);

  // Lịch trong N ngày tới
  @Query("""
          SELECT l FROM LichHop l
          WHERE l.nguoiThamDuId = :userId
            AND l.thoiGianBatDau BETWEEN CURRENT_TIMESTAMP AND :cutoff
            AND l.status IN (0, 1)
          ORDER BY l.thoiGianBatDau ASC
      """)
  List<LichHop> findUpcoming(
      @Param("userId") Integer userId,
      @Param("cutoff") LocalDateTime cutoff);

  // Đếm cuộc họp tuần này (Oracle: IW = start of ISO week)
  @Query(value = """
          SELECT COUNT(*) FROM LICH_HOP
          WHERE NGUOI_THAM_DU_ID = :userId
            AND THOI_GIAN_BAT_DAU >= TRUNC(SYSDATE, 'IW')
            AND THOI_GIAN_BAT_DAU <  TRUNC(SYSDATE, 'IW') + 7
            AND STATUS NOT IN (2, 3)
      """, nativeQuery = true)
  long countThisWeek(@Param("userId") Integer userId);

  // Đếm lịch hôm nay
  @Query(value = """
          SELECT COUNT(*) FROM LICH_HOP
          WHERE NGUOI_THAM_DU_ID = :userId
            AND TRUNC(THOI_GIAN_BAT_DAU) = TRUNC(CURRENT_TIMESTAMP)
            AND STATUS NOT IN (2, 3)
      """, nativeQuery = true)
  long countToday(@Param("userId") Integer userId);
}
