package mk.ukim.finki.testing.shop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderNotifier orderNotifier;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(productRepository, orderRepository, orderNotifier, new PriceCalculator());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n", "\t" })
    void rejectsBlankCustomerId(String customerID) {
        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(customerID, "ProductCode", 73, false));

        verifyNoInteractions(productRepository, orderRepository, orderNotifier);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n", "\t" })
    void rejectsBlankProductCode(String productCode) {
        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder("CustomerID", productCode, 73, false));

        verifyNoInteractions(productRepository, orderRepository, orderNotifier);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void rejectsOutOfRangeQuantity(int quantity) {
        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder("CustomerID", "ProductCode", quantity, false));

        verifyNoInteractions(productRepository, orderRepository, orderNotifier);
    }

    @Test
    void findsProductByTheSameCode() {
        when(productRepository.findByCode(" ProductCode"))
                .thenReturn(Optional.of(new Product(" ProductCode", 1250, 80)));

        orderService.placeOrder("CustomerID", " ProductCode", 73, false);

        verify(productRepository, times(1)).findByCode(" ProductCode");
    }

    @Test
    void checksIfProductIsntFound() {
        when(productRepository.findByCode("ProductCode"))
                .thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> orderService.placeOrder("CustomerID", "ProductCode", 73, false));

        verifyNoInteractions(orderRepository, orderNotifier);
    }

    @Test
    void checksIfRequestedQuantityExceedsAvailableStock() {
        when(productRepository.findByCode("ProductCode"))
                .thenReturn(Optional.of(new Product("ProductCode", 1250, 72)));

        assertThrows(InsufficientStockException.class, () -> orderService.placeOrder("CustomerID", "ProductCode", 73, false));

        verifyNoInteractions(orderRepository, orderNotifier);
    }

    @Test
    void calculatesTotalWithPriceCalculator() {
        when(productRepository.findByCode("ProductCode"))
                .thenReturn(Optional.of(new Product("ProductCode", 1250, 79)));

        Order order = orderService.placeOrder("CustomerID", "ProductCode", 7, false);

        assertEquals(8750, order.totalCents());
    }

    @Test
    void savesOrderOnceAndSendsConfirmation() {
        when(productRepository.findByCode("ProductCode"))
                .thenReturn(Optional.of(new Product("ProductCode", 1250, 73)));

        Order order = orderService.placeOrder("CustomerID", "ProductCode", 9, false);

        verify(orderRepository, times(1)).save(order);
        verify(orderNotifier, times(1)).sendConfirmation(order);

    }

    @Test
    void returnsCorrectOrder() {
        when(productRepository.findByCode("ProductCode"))
                .thenReturn(Optional.of(new Product("ProductCode", 1999, 73)));

        Order order = orderService.placeOrder("CustomerID", "ProductCode", 9, false);

        assertEquals(new Order("CustomerID", "ProductCode", 9, 17991), order);
    }

    @Test
    void doesNotSendConfirmationWhenSaveFails() {
        when(productRepository.findByCode("ProductCode"))
                .thenReturn(Optional.of(new Product("ProductCode", 1250, 73)));

        doThrow(new IllegalStateException())
                .when(orderRepository).save(any());

        assertThrows(IllegalStateException.class, () -> orderService.placeOrder("CustomerID", "ProductCode", 5, false));

        verify(orderNotifier, never()).sendConfirmation(any());
    }

    @Test
    void orderStaysSavedWhenConfirmationFails() {
        when(productRepository.findByCode("ProductCode"))
                .thenReturn(Optional.of(new Product("ProductCode", 1250, 73)));

        doThrow(new IllegalStateException())
                .when(orderNotifier).sendConfirmation(any());

        assertThrows(IllegalStateException.class, () -> orderService.placeOrder("CustomerID", "ProductCode", 5, false));

        verify(orderRepository).save(any());
    }
}
