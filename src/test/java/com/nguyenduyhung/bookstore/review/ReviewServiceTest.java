package com.nguyenduyhung.bookstore.review;

import com.nguyenduyhung.bookstore.catalog.book.BookRepository;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.order.OrderItemRepository;
import com.nguyenduyhung.bookstore.order.OrderStatus;
import com.nguyenduyhung.bookstore.review.dto.ReviewRequest;
import com.nguyenduyhung.bookstore.security.AuthUser;
import com.nguyenduyhung.bookstore.user.Role;
import com.nguyenduyhung.bookstore.user.User;
import com.nguyenduyhung.bookstore.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final Long AUTHOR_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long BOOK_ID = 10L;
    private static final Long REVIEW_ID = 100L;

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    // ===================== Viết đánh giá =====================

    @Test
    void create_withoutCompletedPurchase_isRejected() {
        when(bookRepository.existsByIdAndActiveTrue(BOOK_ID)).thenReturn(true);
        when(orderItemRepository.existsByBookIdAndOrder_UserIdAndOrder_Status(BOOK_ID, AUTHOR_ID, OrderStatus.COMPLETED))
                .thenReturn(false);

        assertThatThrownBy(() -> reviewService.create(AUTHOR_ID, BOOK_ID, new ReviewRequest(5, "Hay")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void create_secondReviewForSameBook_isRejected() {
        when(bookRepository.existsByIdAndActiveTrue(BOOK_ID)).thenReturn(true);
        when(orderItemRepository.existsByBookIdAndOrder_UserIdAndOrder_Status(BOOK_ID, AUTHOR_ID, OrderStatus.COMPLETED))
                .thenReturn(true);
        when(reviewRepository.existsByUserIdAndBookId(AUTHOR_ID, BOOK_ID)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.create(AUTHOR_ID, BOOK_ID, new ReviewRequest(4, null)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    @Test
    void create_forMissingOrInactiveBook_returnsNotFound() {
        when(bookRepository.existsByIdAndActiveTrue(BOOK_ID)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.create(AUTHOR_ID, BOOK_ID, new ReviewRequest(4, null)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void create_success_trimsCommentAndLinksAuthor() {
        User author = user(AUTHOR_ID, "Nguyễn An");
        when(bookRepository.existsByIdAndActiveTrue(BOOK_ID)).thenReturn(true);
        when(orderItemRepository.existsByBookIdAndOrder_UserIdAndOrder_Status(BOOK_ID, AUTHOR_ID, OrderStatus.COMPLETED))
                .thenReturn(true);
        when(reviewRepository.existsByUserIdAndBookId(AUTHOR_ID, BOOK_ID)).thenReturn(false);
        when(userRepository.getReferenceById(AUTHOR_ID)).thenReturn(author);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = reviewService.create(AUTHOR_ID, BOOK_ID, new ReviewRequest(5, "  Rất hay  "));

        ArgumentCaptor<Review> saved = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(saved.capture());
        assertThat(saved.getValue().getBookId()).isEqualTo(BOOK_ID);
        assertThat(saved.getValue().getComment()).isEqualTo("Rất hay");
        assertThat(response.reviewerName()).isEqualTo("Nguyễn An");
    }

    // ===================== Sửa / xóa =====================

    @Test
    void update_byAnotherUser_isDenied_evenIfAdmin() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(existingReview()));

        assertThatThrownBy(() -> reviewService.update(
                new AuthUser(OTHER_USER_ID, "admin@example.com", Role.ADMIN), REVIEW_ID, new ReviewRequest(1, "Sửa")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void delete_byAnotherRegularUser_isDenied() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(existingReview()));

        assertThatThrownBy(() -> reviewService.delete(
                new AuthUser(OTHER_USER_ID, "b@example.com", Role.USER), REVIEW_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    void delete_byAdmin_isAllowed() {
        Review review = existingReview();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        reviewService.delete(new AuthUser(OTHER_USER_ID, "admin@example.com", Role.ADMIN), REVIEW_ID);

        verify(reviewRepository).delete(review);
    }

    @Test
    void delete_byAuthor_isAllowed() {
        Review review = existingReview();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        reviewService.delete(new AuthUser(AUTHOR_ID, "a@example.com", Role.USER), REVIEW_ID);

        verify(reviewRepository).delete(review);
    }

    // ===================== Helper =====================

    private static Review existingReview() {
        Review review = Review.create(user(AUTHOR_ID, "Nguyễn An"), BOOK_ID, 5, "Hay");
        ReflectionTestUtils.setField(review, "id", REVIEW_ID);
        return review;
    }

    private static User user(Long id, String fullName) {
        User user = User.create("u" + id + "@example.com", "hash", fullName, Role.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
