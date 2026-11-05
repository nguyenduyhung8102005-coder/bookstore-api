package com.nguyenduyhung.bookstore.catalog.category;

import com.nguyenduyhung.bookstore.common.entity.BaseEntity;
import com.nguyenduyhung.bookstore.common.util.SlugUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    public static Category create(String name) {
        Category category = new Category();
        category.rename(name);
        return category;
    }

    /** Slug luôn được tính lại từ tên, nên hai giá trị không bao giờ lệch nhau. */
    public void rename(String name) {
        this.name = name.trim();
        this.slug = SlugUtils.toSlug(name);
    }
}
