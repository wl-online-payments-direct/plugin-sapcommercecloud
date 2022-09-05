package com.worldline.direct.b2bcheckoutaddon.controllers;

public interface WorldlineWebConstants {
    interface URL {

        interface Account {
            String root = "/my-account/worldline";

            interface PaymentDetails {
                String root = Account.root + "/payment-details";
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

            interface Replenishment {
                interface OrderConfirmation {
                    String root = "/checkout/worldline/replenishment/";
                }
            }

            interface Payment {
                String root = Checkout.root;
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


        }
    }
}
