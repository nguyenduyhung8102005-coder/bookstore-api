package com.nguyenduyhung.bookstore.cart;

import com.nguyenduyhung.bookstore.cart.dto.AddCartItemRequest;
import com.nguyenduyhung.bookstore.cart.dto.CartResponse;
import com.nguyenduyhung.bookstore.catalog.book.Book;
import com.nguyenduyhung.bookstore.catalog.book.BookRepository;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Mọi method nhận userId lấy từ access token (Controller truyền vào), và mọi truy
 * vấn đều lọc theo userId đó. Sách được xác định bằng bookId TRONG GIỎ của chính
 * user, nên không có cách nào chạm vào giỏ của người khác (chống lỗi IDOR).
 *
 * Mọi thao tác ghi đều trả về giỏ hàng MỚI NHẤT, để client cập nhật giao diện
 * ngay mà không phải gọi thêm GET /cart.
 *
 * Về tồn kho: kiểm tra ở đây chỉ là kiểm tra "mềm" để báo sớm cho người dùng. Giỏ
 * hàng KHÔNG giữ chỗ hàng; trong lúc sách nằm trong giỏ, người khác vẫn mua được.
 * Quyết định cuối cùng (có trừ được kho hay không) nằm ở bước tạo đơn (GĐ 5).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;

    public CartResponse getCart(Long userId) {
        return CartMapper.toResponse(cartItemRepository.findByUserIdOrderByCreatedAtAscIdAsc(userId));
    }

    /**
     * Thêm sách vào giỏ; nếu sách đã có thì CỘNG DỒN số lượng.
     *
     * Lưu ý về getCart() ở cuối: gọi method trong cùng class không đi qua proxy
     * của Spring, nên @Transactional(readOnly = true) của getCart KHÔNG được áp
     * dụng; nó chạy trong transaction ghi hiện tại. Ở đây điều đó đúng ý ta.
     * Đây là hiện tượng "self-invocation", sẽ gặp lại ở GĐ 5.
     */
    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        Book book = getSellableBook(request.bookId());
        Optional<CartItem> existing = cartItemRepository.findByUserIdAndBookId(userId, book.getId());

        int newQuantity = existing.map(CartItem::getQuantity).orElse(0) + request.quantity();
        validateQuantity(book, newQuantity);

        existing.ifPresentOrElse(
                item -> item.changeQuantity(newQuantity),
                () -> cartItemRepository.save(CartItem.create(userId, book, newQuantity)));
        return getCart(userId);
    }

    /** Đặt lại số lượng cho một sách đã có trong giỏ. */
    @Transactional
    public CartResponse updateQuantity(Long userId, Long bookId, int quantity) {
        CartItem item = getItem(userId, bookId);
        Book book = item.getBook();
        if (!book.isActive()) {
            throw new AppException(ErrorCode.BOOK_NOT_AVAILABLE);
        }
        validateQuantity(book, quantity);
        item.changeQuantity(quantity);
        return getCart(userId);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long bookId) {
        cartItemRepository.delete(getItem(userId, bookId));
        return getCart(userId);
    }

    @Transactional
    public CartResponse clear(Long userId) {
        cartItemRepository.deleteAllByUserId(userId);
        return getCart(userId);
    }

    // ===================== Helper =====================

    /** Sách không tồn tại: 404. Sách đã ngừng bán (soft delete): 409 BOOK_NOT_AVAILABLE. */
    private Book getSellableBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> AppException.notFound("Book", bookId));
        if (!book.isActive()) {
            throw new AppException(ErrorCode.BOOK_NOT_AVAILABLE);
        }
        return book;
    }

    private CartItem getItem(Long userId, Long bookId) {
        return cartItemRepository.findByUserIdAndBookId(userId, bookId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    private static void validateQuantity(Book book, int quantity) {
        if (quantity > CartItem.MAX_QUANTITY) {
            throw new AppException(ErrorCode.QUANTITY_LIMIT_EXCEEDED,
                    "Quantity per book cannot exceed " + CartItem.MAX_QUANTITY);
        }
        if (quantity > book.getStockQuantity()) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                    "Only %d copies of '%s' are left in stock".formatted(book.getStockQuantity(), book.getTitle()));
        }
    }
}
