package com.nguyenduyhung.bookstore.report;

import com.nguyenduyhung.bookstore.order.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Truy vấn thống kê. Kế thừa {@link Repository} (interface đánh dấu, KHÔNG có sẵn
 * save/delete...) vì đây là repository chỉ đọc: không ai được ghi dữ liệu qua nó.
 *
 * Dùng native SQL thay vì JPQL vì cần hàm riêng của PostgreSQL (date_trunc,
 * AT TIME ZONE, to_char) để gom theo ngày giờ Việt Nam ngay trong DB.
 *
 * Kết quả trả về dạng "interface projection": Spring Data tạo object cài đặt
 * interface, map từng cột theo TÊN ALIAS. Alias đặt trong ngoặc kép ("orderCount")
 * vì PostgreSQL tự đổi tên không có ngoặc kép thành chữ thường (ordercount).
 *
 * Điều kiện thời gian luôn viết dạng "paid_at >= :start AND paid_at < :end" (khoảng
 * nửa mở), KHÔNG bọc cột trong hàm (ví dụ date(paid_at) = ...), để DB dùng được
 * index trên paid_at.
 */
public interface ReportRepository extends Repository<Order, Long> {

    /**
     * Doanh thu và số đơn theo từng kỳ. Chỉ trả về kỳ CÓ đơn; kỳ trống do Service tự điền 0.
     *
     * "GROUP BY 1" = gom theo cột thứ nhất của SELECT. Không lặp lại biểu thức
     * date_trunc(:unit, ...) ở GROUP BY được, vì hai lần xuất hiện tham số là hai
     * placeholder khác nhau ($1, $3...) và PostgreSQL không coi chúng là cùng một biểu thức.
     */
    @Query(nativeQuery = true, value = """
            SELECT to_char(date_trunc(:unit, o.paid_at AT TIME ZONE :zone), 'YYYY-MM-DD') AS "period",
                   SUM(o.total_amount)                                                    AS "revenue",
                   COUNT(*)                                                               AS "orderCount"
              FROM orders o
             WHERE o.paid_at >= :start
               AND o.paid_at < :end
               AND o.status IN (:statuses)
             GROUP BY 1
             ORDER BY 1
            """)
    List<RevenueRow> sumRevenueByPeriod(@Param("unit") String unit,
                                        @Param("zone") String zone,
                                        @Param("start") Instant start,
                                        @Param("end") Instant end,
                                        @Param("statuses") Collection<String> statuses);

    /**
     * Sách bán chạy theo SỐ LƯỢNG, hòa thì xét doanh thu, rồi tới book_id cho thứ tự ổn định.
     *
     * Tên sách lấy từ bảng books (tên HIỆN TẠI), không lấy book_title trong order_items:
     * nếu admin từng đổi tên sách, các dòng snapshot có tên khác nhau và GROUP BY theo
     * tên sẽ tách một cuốn thành nhiều dòng. Gom theo book_id mới đúng "một cuốn sách".
     */
    @Query(nativeQuery = true, value = """
            SELECT oi.book_id                       AS "bookId",
                   b.title                          AS "title",
                   SUM(oi.quantity)                 AS "quantitySold",
                   SUM(oi.unit_price * oi.quantity) AS "revenue"
              FROM order_items oi
              JOIN orders o ON o.id = oi.order_id
              JOIN books b ON b.id = oi.book_id
             WHERE o.paid_at >= :start
               AND o.paid_at < :end
               AND o.status IN (:statuses)
             GROUP BY oi.book_id, b.title
             ORDER BY "quantitySold" DESC, "revenue" DESC, oi.book_id
             LIMIT :limit
            """)
    List<BestSellerRow> findBestSellers(@Param("start") Instant start,
                                        @Param("end") Instant end,
                                        @Param("statuses") Collection<String> statuses,
                                        @Param("limit") int limit);

    interface RevenueRow {
        /** Ngày bắt đầu của kỳ, dạng yyyy-MM-dd (trả chuỗi để tránh lệch kiểu ngày giữa JDBC và Java). */
        String getPeriod();

        BigDecimal getRevenue();

        Long getOrderCount();
    }

    interface BestSellerRow {
        Long getBookId();

        String getTitle();

        Long getQuantitySold();

        BigDecimal getRevenue();
    }
}
