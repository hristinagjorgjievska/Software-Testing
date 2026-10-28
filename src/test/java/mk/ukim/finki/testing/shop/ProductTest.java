package mk.ukim.finki.testing.shop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProductTest {

    @Test
    void createsProductWithValidValues(){
        Product product = new Product("ProductCode", 7000, 23);

        assertEquals("ProductCode", product.code());
        assertEquals(7000, product.unitPriceCents());
        assertEquals(23, product.availableQuantity());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t", "\n"})
    void rejectsBlankCode(String code){
        assertThrows(IllegalArgumentException.class, () -> new Product(code, 1000, 10));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1000001})
    void rejectsOutOfRangePrice(int price){
        assertThrows(IllegalArgumentException.class, () -> new Product("ProductCode", price, 5));
    }

    @Test
    void rejectsNegativeValueForQuantity(){
        assertThrows(IllegalArgumentException.class, () -> new Product("ProductCode", 8500, -3));
    }
}
