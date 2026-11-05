-- =====================================================================
-- V3: Index phục vụ tìm sách theo tên: lower(title) LIKE '%từ khóa%'
--
-- Index B-tree thông thường chỉ dùng được khi chuỗi tìm kiếm có phần ĐẦU cố
-- định (LIKE 'abc%'). Với '%abc%' (chứa ở giữa), PostgreSQL phải quét toàn bộ
-- bảng. Extension pg_trgm cắt chuỗi thành các cụm 3 ký tự (trigram) và đánh
-- index GIN trên đó, nhờ vậy LIKE '%abc%' cũng dùng được index.
--
-- Index được tạo trên BIỂU THỨC lower(title), khớp đúng biểu thức trong câu
-- truy vấn (BookSpecifications.titleContains); index trên title trần sẽ không
-- được dùng cho lower(title).
--
-- Với vài chục cuốn sách, PostgreSQL vẫn sẽ chọn quét bảng vì nhanh hơn; index
-- chỉ phát huy khi bảng lớn. Kiểm tra bằng: EXPLAIN ANALYZE SELECT ...
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_books_title_trgm ON books USING gin (lower(title) gin_trgm_ops);
