package mk.ukim.finki.testing.shop;

/** Calculates the total for an order containing one product type. All amounts are cents. */
public class PriceCalculator {

    public static final int MAX_UNIT_PRICE = 1_000_000;
    public static final int MAX_QUANTITY = 100;
    private static final int FREE_DELIVERY_THRESHOLD = 5_000;
    private static final int SHIPPING_FEE = 500;
    private static final long PREMIUM_CUSTOMER = 90L;
    private static final int PERCENT_TO_DIVIDE = 100;

    public int calculateTotal(int unitPriceCents, int quantity, boolean premiumCustomer) {
        if (unitPriceCents < 1 || unitPriceCents > MAX_UNIT_PRICE) {
            throw new IllegalArgumentException("Unit price must be between 1 and 1000000 cents");
        }
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be between 1 and 100");
        }


        int subtotal = unitPriceCents * quantity;
        int discountedSubtotal;
        if (premiumCustomer == true) {
            discountedSubtotal = (int) (subtotal * PREMIUM_CUSTOMER / PERCENT_TO_DIVIDE);
        } else {
            discountedSubtotal = subtotal;
        }
        if (discountedSubtotal >= FREE_DELIVERY_THRESHOLD) {
            return discountedSubtotal;
        }
        return discountedSubtotal + SHIPPING_FEE;
    }
}
