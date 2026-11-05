package com.nguyenduyhung.bookstore.catalog.category;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsBySlug(String slug);

    /** Dùng khi đổi tên: trùng slug với thể loại KHÁC thì mới là lỗi. */
    boolean existsBySlugAndIdNot(String slug, Long id);
}
