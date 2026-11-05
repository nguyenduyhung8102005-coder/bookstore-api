package com.nguyenduyhung.bookstore.review;

import com.nguyenduyhung.bookstore.catalog.book.BookRepository;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.common.response.PageResponse;
import com.nguyenduyhung.bookstore.order.OrderItemRepository;
import com.nguyenduyhung.bookstore.order.OrderStatus;
import com.nguyenduyhung.bookstore.review.dto.ReviewRequest;
import com.nguyenduyhung.bookstore.review.dto.ReviewResponse;
import com.nguyenduyhung.bookstore.security.AuthUser;
import com.nguyenduyhung.bookstore.user.Role;
import com.nguyenduyhung.bookstore.user.User;
import com.nguyenduyhung.bookstore.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public PageResponse<ReviewResponse> getReviewsOfBook(Long bookId, Pageable pageable) {
        ensureBookIsActive(bookId);
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
        return PageResponse.from(reviewRepository.findByBookId(bookId, newestFirst).map(ReviewMapper::toResponse));
    }

    /**
     * Thứ tự kiểm tra có chủ đích: sách có tồn tại không (404) → có quyền đánh giá
     * không (403) → đã đánh giá chưa (409). Lỗi "cơ bản" hơn được báo trước.
     *
     * Hai request viết đánh giá trùng chạy song song có thể cùng vượt qua bước
     * existsBy...; ràng buộc UNIQUE(user_id, book_id) trong DB là chốt chặn cuối.
     */
    @Transactional
    public ReviewResponse create(Long userId, Long bookId, ReviewRequest request) {
        ensureBookIsActive(bookId);
        if (!orderItemRepository.existsByBookIdAndOrder_UserIdAndOrder_Status(bookId, userId, OrderStatus.COMPLETED)) {
            throw new AppException(ErrorCode.REVIEW_NOT_ALLOWED);
        }
        if (reviewRepository.existsByUserIdAndBookId(userId, bookId)) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        // getReferenceById KHÔNG truy vấn DB: nó trả về một proxy chỉ chứa id, đủ để
        // Hibernate ghi cột user_id. findById thì tốn thêm một câu SELECT không cần thiết.
        User author = userRepository.getReferenceById(userId);
        Review review = reviewRepository.save(Review.create(author, bookId, request.rating(), request.comment()));
        return ReviewMapper.toResponse(review);
    }

    /** Chỉ người viết được sửa. Admin chỉ được xóa (kiểm duyệt), không được sửa lời của người khác. */
    @Transactional
    public ReviewResponse update(AuthUser currentUser, Long reviewId, ReviewRequest request) {
        Review review = getReview(reviewId);
        if (!review.isWrittenBy(currentUser.id())) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "You can only edit your own reviews");
        }
        review.edit(request.rating(), request.comment());
        // Flush để JPA Auditing cập nhật updatedAt TRƯỚC khi map sang response.
        reviewRepository.flush();
        return ReviewMapper.toResponse(review);
    }

    /** Người viết tự xóa, hoặc ADMIN xóa (ví dụ đánh giá vi phạm quy định). */
    @Transactional
    public void delete(AuthUser currentUser, Long reviewId) {
        Review review = getReview(reviewId);
        if (!review.isWrittenBy(currentUser.id()) && currentUser.role() != Role.ADMIN) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "You can only delete your own reviews");
        }
        reviewRepository.delete(review);
    }

    private Review getReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> AppException.notFound("Review", reviewId));
    }

    /** Sách không tồn tại hoặc đã ngừng bán: 404, giống API chi tiết sách. */
    private void ensureBookIsActive(Long bookId) {
        if (!bookRepository.existsByIdAndActiveTrue(bookId)) {
            throw AppException.notFound("Book", bookId);
        }
    }
}
