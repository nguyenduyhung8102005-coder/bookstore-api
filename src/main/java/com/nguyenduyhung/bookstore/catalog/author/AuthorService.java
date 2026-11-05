package com.nguyenduyhung.bookstore.catalog.author;

import com.nguyenduyhung.bookstore.catalog.author.dto.AuthorRequest;
import com.nguyenduyhung.bookstore.catalog.author.dto.AuthorResponse;
import com.nguyenduyhung.bookstore.catalog.book.BookRepository;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    public PageResponse<AuthorResponse> findAll(Pageable pageable) {
        return PageResponse.from(authorRepository.findAll(pageable).map(AuthorMapper::toResponse));
    }

    public AuthorResponse getById(Long id) {
        return AuthorMapper.toResponse(getEntity(id));
    }

    @Transactional
    public AuthorResponse create(AuthorRequest request) {
        return AuthorMapper.toResponse(authorRepository.save(Author.create(request.name(), request.bio())));
    }

    @Transactional
    public AuthorResponse update(Long id, AuthorRequest request) {
        Author author = getEntity(id);
        author.update(request.name(), request.bio());
        return AuthorMapper.toResponse(author);
    }

    /**
     * Không cho xóa tác giả còn gắn với sách. Nếu không kiểm tra, khóa ngoại
     * trong bảng book_authors cũng chặn lại, nhưng client chỉ nhận lỗi chung
     * DATA_CONFLICT thay vì một mã lỗi nói rõ lý do.
     */
    @Transactional
    public void delete(Long id) {
        Author author = getEntity(id);
        if (bookRepository.existsByAuthorsId(id)) {
            throw new AppException(ErrorCode.AUTHOR_IN_USE);
        }
        authorRepository.delete(author);
    }

    public Author getEntity(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Author", id));
    }

    /**
     * Lấy nhiều tác giả bằng MỘT câu "WHERE id IN (...)" thay vì gọi findById
     * từng id (N câu truy vấn). Báo đúng những id không tồn tại.
     */
    public Set<Author> getEntities(Collection<Long> ids) {
        List<Author> found = authorRepository.findAllById(ids);
        if (found.size() != new HashSet<>(ids).size()) {
            Set<Long> missing = new TreeSet<>(ids);
            found.forEach(author -> missing.remove(author.getId()));
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Author not found with id: " + missing);
        }
        return new HashSet<>(found);
    }
}
