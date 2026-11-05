-- =====================================================================
-- V6: Index cho job hủy đơn quá hạn:
--   WHERE status = 'PENDING' AND created_at < ? ORDER BY created_at
--
-- Đây là PARTIAL INDEX (index một phần): chỉ chứa các dòng thỏa WHERE, tức chỉ
-- các đơn đang PENDING. Phần lớn đơn hàng theo thời gian sẽ là COMPLETED hoặc
-- CANCELLED, nên index này luôn rất nhỏ so với một index thường trên
-- (status, created_at), mà vẫn phục vụ trọn câu truy vấn của job.
-- =====================================================================

CREATE INDEX idx_orders_pending_created_at ON orders (created_at) WHERE status = 'PENDING';
