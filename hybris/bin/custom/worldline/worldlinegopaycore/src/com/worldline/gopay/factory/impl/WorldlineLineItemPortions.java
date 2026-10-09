package com.worldline.gopay.factory.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Breaks an order line into portions whose per unit amounts add up exactly to the line totals.
 * <p>
 * Worldline treats {@code productPrice}, {@code discountAmount} and {@code taxAmount} as per unit values and rejects
 * a line item unless {@code quantity * (productPrice - discountAmount + taxAmount)} equals its amount. When the line
 * totals do not divide evenly by the quantity, the line is returned as two portions: {@code quantity - 1} units at
 * the rounded-down unit amounts, and one unit carrying the remainder.
 * <p>
 * All amounts are in the currency's minor units.
 */
final class WorldlineLineItemPortions {

    private WorldlineLineItemPortions() {
    }

    /**
     * @param quantity number of units on the line; null or less than 1 is treated as 1
     * @param net      line amount excluding tax, after discount
     * @param discount line discount
     * @param tax      line tax
     * @return one portion when the totals divide evenly by the quantity, otherwise two
     */
    static List<Portion> split(Long quantity, long net, long discount, long tax) {
        final long units = quantity == null || quantity < 1L ? 1L : quantity;
        if (net % units == 0 && discount % units == 0 && tax % units == 0) {
            return Collections.singletonList(new Portion(units, net / units, discount / units, tax / units));
        }
        final long unitNet = Math.floorDiv(net, units);
        final long unitDiscount = Math.floorDiv(discount, units);
        final long unitTax = Math.floorDiv(tax, units);
        final long others = units - 1;
        return Arrays.asList(
                new Portion(others, unitNet, unitDiscount, unitTax),
                new Portion(1L, net - others * unitNet, discount - others * unitDiscount, tax - others * unitTax));
    }

    /**
     * Units of a line that share the same per unit amounts.
     *
     * @param quantity number of units
     * @param net      amount per unit excluding tax, after discount
     * @param discount discount per unit
     * @param tax      tax per unit
     */
    record Portion(long quantity, long net, long discount, long tax) {

        /**
         * @return price per unit excluding tax, before discount
         */
        long productPrice() {
            return net + discount;
        }

        /**
         * @return amount to pay for all units in this portion, including tax
         */
        long amount() {
            return quantity * (net + tax);
        }
    }
}
