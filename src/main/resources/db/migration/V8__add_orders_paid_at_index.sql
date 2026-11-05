-- =====================================================================
-- V8: Index cho thống kê doanh thu / sách bán chạy (GĐ 7):
--   WHERE paid_at >= :start AND paid_at < :end AND status IN (...)
--
-- Partial index: đơn chưa trả tiền (paid_at NULL) không bao giờ xuất hiện trong
-- báo cáo nên không cần nằm trong index.
--
-- Lưu ý khi kiểm tra bằng EXPLAIN: với bảng nhỏ (vài trăm đơn), PostgreSQL vẫn
-- chọn đọc tuần tự (Seq Scan) vì rẻ hơn. Index phát huy khi bảng orders lớn và
-- khoảng ngày hẹp so với toàn bộ dữ liệu.
-- =====================================================================

CREATE INDEX idx_orders_paid_at ON orders (paid_at) WHERE paid_at IS NOT NULL;
