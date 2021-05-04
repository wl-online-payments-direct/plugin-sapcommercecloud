/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.ingenico.ogone.direct.checkoutaddon.controllers;

/**
 *
 */
public interface IngenicoWebConstants {
    interface URL {

        interface Checkout {
            String root = "/checkout/multi/ingenico";

            interface OrderConfirmation{
                String root = "/checkout/ingenico/orderConfirmation/";
            }

            interface Summary {
                String root = Checkout.root + "/summary";
                String view = "/view";
                String placeOrder = "/placeOrder";
            }

            interface Payment {
                String root = Checkout.root;
                String select = "/select-payment-method";
                String billing = "/billingaddressform";

                interface HTP {
                    String root = Checkout.root + "/hosted-tokenization";
                    String view = "/view";
                    String create = "/do";
                    String handleResponse = "/handle3ds";
                }

                interface HOP {
                    String root = Checkout.root + "/hosted-checkout";
                    String handleResponse = "/response/";
                }

            }


        }
    }
}
