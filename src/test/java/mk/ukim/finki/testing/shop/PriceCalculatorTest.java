package mk.ukim.finki.testing.shop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PriceCalculatorTest {


    private PriceCalculator priceCalculator;

    @BeforeEach
    void setUp(){
        priceCalculator = new PriceCalculator();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1000001})
    void rejectsOutOfRangePrice(int price){
        assertThrows(IllegalArgumentException.class, () -> priceCalculator.calculateTotal(price, 10, true));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void rejectsOutOfRangeQuantity(int quantity){
        assertThrows(IllegalArgumentException.class, () -> priceCalculator.calculateTotal(5000, quantity, false));
    }

    @ParameterizedTest
    @CsvSource({
            "350, 10, 4000",
            "740, 10, 7400"
    })
    void calculateRegularCustomerSubtotal(int price, int quantity, int total){
        assertEquals(total, priceCalculator.calculateTotal(price, quantity, false));
    }

    @ParameterizedTest
    @CsvSource({
            "350, 10, 3650",
            "740, 10, 6660"
    })
    void calculatePremiumCustomerSubtotal(int price, int quantity, int total){
        assertEquals(total, priceCalculator.calculateTotal(price, quantity, true));
    }

    @Test
    void premiumCustomerSubtotalRoundedDownToWholeCents(){
        assertEquals(2083, priceCalculator.calculateTotal(1759, 1, true));
    }

    @Test
    void shippingIsCalculatedAfterDiscount(){
        assertEquals(5180, priceCalculator.calculateTotal(520, 10, true));
    }
}
