/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.worldline.direct.checkoutaddon.controllers;

/**
 *
 */
public interface WorldlineWebConstants {
    interface URL {

        interface Account {
            String root = "/my-account/worldline";

            interface PaymentDetails {
                String root = Account.root + "/payment-details";
            }

            interface Replenishment {
                String ReadOnlyExpandedOrderForm =  "/fragments/checkout/readOnlyExpandedOrderForm";
            }

        }

        interface Checkout {
            String root = "/checkout/multi/worldline";

            interface OrderConfirmation {
                String root = "/checkout/worldline/orderConfirmation/";
            }

            interface Summary {
                String root = Checkout.root + "/summary";
                String view = "/view";
                String placeOrder = "/placeOrder";
            }

            interface Payment {
                String root = Checkout.root + "/payment";
                String select = "/select-payment-method";
                String billing = "/billingaddressform";

                interface HTP {
                    String root = Checkout.root + "/hosted-tokenization";
                    String handleResponse = "/handle3ds/";
                }

                interface HOP {
                    String root = Checkout.root + "/hosted-checkout";
                    String handleResponse = "/response/";
                }



            }
            interface DeliveryAddress {
                String root = Checkout.root + "/delivery-address";
                String add = "/add";
                String edit = "/edit";
                String remove = "/remove";
                String select = "/select";
            }


        }
    }

}
