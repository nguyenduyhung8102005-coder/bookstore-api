package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.cart.CartItem;
import com.nguyenduyhung.bookstore.cart.CartItemRepository;
import com.nguyenduyhung.bookstore.catalog.book.Book;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.order.dto.OrderResponse;
import com.nguyenduyhung.bookstore.order.dto.PlaceOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * MỘT lần thử đặt hàng, gói trọn trong MỘT transaction.
 *
 * Tất cả hoặc không gì cả (atomicity): nếu bất kỳ sách nào trong giỏ không đủ
 * hàng, exception làm rollback toàn bộ: kho của những sách đã trừ trước đó được
 * trả lại, đơn không được tạo, giỏ hàng giữ nguyên.
 *
 * Class này KHÔNG tự thử lại. Việc thử lại khi gặp xung đột nằm ở OrderService,
 * bên ngoài transaction (xem giải thích ở đó).
 */
@Service
@RequiredArgsConstructor
public class OrderPlacementService {

    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtAscIdAsc(userId);
        if (cartItems.isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        Order order = Order.create(userId, request.recipientName(), request.phone(), request.shippingAddress());

        // Xử lý các sách theo thứ tự id tăng dần (xem thêm hibernate.order_updates trong
        // application.yml). Nếu đơn A trừ sách 1 rồi sách 2, còn đơn B trừ sách 2 rồi
        // sách 1, hai transaction có thể chờ khóa của nhau mãi mãi (deadlock). Cùng
        // một thứ tự cho mọi đơn thì không thể xảy ra vòng chờ như vậy.
        List<CartItem> sortedItems = cartItems.stream()
                .sorted(Comparator.comparing(item -> item.getBook().getId()))
                .toList();

        for (CartItem cartItem : sortedItems) {
            Book book = cartItem.getBook();
            int quantity = cartItem.getQuantity();
            ensureCanBuy(book, quantity);

            // Chỉ đổi giá trị trên object Java. Câu UPDATE thật (kèm điều kiện
            // "WHERE version = ?") chạy lúc flush/commit; nếu có request khác đã
            // sửa cuốn sách này trước, nó ném ObjectOptimisticLockingFailureException.
            book.decreaseStock(quantity);
            order.addItem(book.getId(), book.getTitle(), book.getPrice(), quantity);
        }

        orderRepository.save(order);
        cartItemRepository.deleteAllByUserId(userId);
        return OrderMapper.toResponse(order);
    }

    private static void ensureCanBuy(Book book, int quantity) {
        if (!book.isActive()) {
            throw new AppException(ErrorCode.BOOK_NOT_AVAILABLE,
                    "Book '%s' is no longer available for sale".formatted(book.getTitle()));
        }
        if (book.getStockQuantity() < quantity) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                    "Only %d copies of '%s' are left in stock".formatted(book.getStockQuantity(), book.getTitle()));
        }
    }
}
