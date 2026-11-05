package com.nguyenduyhung.bookstore.catalog.author;

import com.nguyenduyhung.bookstore.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Lưu ý về equals/hashCode: entity này KHÔNG override, tức dùng so sánh theo
 * địa chỉ đối tượng (mặc định của Object). Điều đó đủ đúng trong dự án này vì
 * trong cùng một transaction, Hibernate đảm bảo mỗi dòng DB chỉ tương ứng MỘT
 * đối tượng Java. Tuyệt đối không dùng @Data/@EqualsAndHashCode của Lombok cho
 * entity: chúng so sánh mọi field, kể cả quan hệ lazy, gây truy vấn ngầm, vòng
 * lặp vô hạn, và hashCode đổi sau khi lưu (id từ null thành có giá trị).
 */
@Getter
@Entity
@Table(name = "authors")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Author extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String bio;

    public static Author create(String name, String bio) {
        Author author = new Author();
        author.update(name, bio);
        return author;
    }

    public void update(String name, String bio) {
        this.name = name.trim();
        this.bio = bio;
    }
}
