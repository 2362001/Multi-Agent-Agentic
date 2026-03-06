package vn.vnpt.kntc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.vnpt.kntc.model.ThongBao;

import java.util.List;

@Repository
public interface ThongBaoRepository extends JpaRepository<ThongBao, Integer> {

    // Tất cả thông báo chưa đọc
    @Query("""
        SELECT t FROM ThongBao t
        WHERE t.nguoiNhanId = :userId
          AND t.daDoc = 0
        ORDER BY t.ngayTao DESC
    """)
    List<ThongBao> findUnread(@Param("userId") Integer userId);

    // Đếm thông báo chưa đọc
    @Query("""
        SELECT COUNT(t) FROM ThongBao t
        WHERE t.nguoiNhanId = :userId
          AND t.daDoc = 0
    """)
    long countUnread(@Param("userId") Integer userId);

    // Thông báo quan trọng (ưu tiên cao) chưa đọc
    @Query("""
        SELECT t FROM ThongBao t
        WHERE t.nguoiNhanId = :userId
          AND t.daDoc = 0
          AND t.doUuTien = 3
        ORDER BY t.ngayTao DESC
    """)
    List<ThongBao> findImportantUnread(@Param("userId") Integer userId);

    // Nhắc nhở hôm nay (loai = 2)
    @Query("""
        SELECT t FROM ThongBao t
        WHERE t.nguoiNhanId = :userId
          AND t.loai = 2
          AND TRUNC(t.ngayTao) = TRUNC(CURRENT_TIMESTAMP)
        ORDER BY t.doUuTien DESC, t.ngayTao DESC
    """)
    List<ThongBao> findTodayReminders(@Param("userId") Integer userId);

    // Cảnh báo hạn chót (loai = 3) chưa đọc
    @Query("""
        SELECT t FROM ThongBao t
        WHERE t.nguoiNhanId = :userId
          AND t.loai = 3
          AND t.daDoc = 0
        ORDER BY t.ngayTao DESC
    """)
    List<ThongBao> findDeadlineWarnings(@Param("userId") Integer userId);
}
