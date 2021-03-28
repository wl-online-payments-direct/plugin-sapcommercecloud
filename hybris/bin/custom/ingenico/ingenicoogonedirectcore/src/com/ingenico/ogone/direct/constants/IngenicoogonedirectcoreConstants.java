/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.ingenico.ogone.direct.constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Global class for all Ingenicoogonedirectcore constants. You can add global constants for your extension into this class.
 */
public final class IngenicoogonedirectcoreConstants extends GeneratedIngenicoogonedirectcoreConstants {
    public static final String EXTENSIONNAME = "ingenicoogonedirectcore";

    // Hybris Keys
    public static final String INGENICO_CONNECT_API_API_KEY_ID = "ingenico.connect.api.apiKeyId";
    public static final String INGENICO_CONNECT_API_SECRET_API_KEY = "ingenico.connect.api.secretApiKey";

    public static final String INGENICO_DIRECT_API_AUTHORIZATION_TYPE = "ingenico.direct.api.authorizationType";
    public static final String INGENICO_DIRECT_API_CONNECT_TIMEOUT = "ingenico.direct.api.connectTimeout";
    public static final String INGENICO_DIRECT_API_SOCKET_TIMEOUT = "ingenico.direct.api.socketTimeout";
    public static final String INGENICO_DIRECT_API_MAX_CONNECTIONS = "ingenico.direct.api.maxConnections";
    public static final String INGENICO_DIRECT_API_PROXY_URI = "ingenico.direct.api.proxy.uri";
    public static final String INGENICO_DIRECT_API_PROXY_USERNAME = "ingenico.direct.api.proxy.username";
    public static final String INGENICO_DIRECT_API_PROXY_PASSWORD = "ingenico.direct.api.proxy.password";
    public static final String INGENICO_DIRECT_API_HTTPS_PROTOCOLS = "ingenico.direct.api.https.protocols";
    public static final String INGENICO_DIRECT_API_INTEGRATOR = "ingenico.direct.api.integrator";
    public static final String INGENICO_DIRECT_API_SHOPPING_CART_EXTENSION_CREATOR = "ingenico.direct.api.shoppingCartExtension.creator";
    public static final String INGENICO_DIRECT_API_SHOPPING_CART_EXTENSION_NAME = "ingenico.direct.api.shoppingCartExtension.name";
    public static final String INGENICO_DIRECT_API_SHOPPING_CART_EXTENSION_VERSION = "ingenico.direct.api.shoppingCartExtension.version";
    public static final String INGENICO_DIRECT_API_SHOPPING_CART_EXTENSION_EXTENSION_ID = "ingenico.direct.api.shoppingCartExtension.extensionId";
    public static final String INGENICO_DIRECT_API_ENDPOINT_HOST = "ingenico.direct.api.endpoint.host";
    public static final String INGENICO_DIRECT_API_ENDPOINT_SCHEME = "ingenico.direct.api.endpoint.scheme";
    public static final String INGENICO_DIRECT_API_ENDPOINT_PORT = "ingenico.direct.api.endpoint.port";

    // Direct API Keys
    public static final String DIRECT_API_AUTHORIZATION_TYPE = "direct.api.authorizationType";
    public static final String DIRECT_API_CONNECT_TIMEOUT = "direct.api.connectTimeout";
    public static final String DIRECT_API_SOCKET_TIMEOUT = "direct.api.socketTimeout";
    public static final String DIRECT_API_MAX_CONNECTIONS = "direct.api.maxConnections";
    public static final String DIRECT_API_PROXY_URI = "direct.api.proxy.uri";
    public static final String DIRECT_API_PROXY_USERNAME = "direct.api.proxy.username";
    public static final String DIRECT_API_PROXY_PASSWORD = "direct.api.proxy.password";
    public static final String DIRECT_API_HTTPS_PROTOCOLS = "direct.api.https.protocols";
    public static final String DIRECT_API_INTEGRATOR = "direct.api.integrator";
    public static final String DIRECT_API_SHOPPING_CART_EXTENSION_CREATOR = "direct.api.shoppingCartExtension.creator";
    public static final String DIRECT_API_SHOPPING_CART_EXTENSION_NAME = "direct.api.shoppingCartExtension.name";
    public static final String DIRECT_API_SHOPPING_CART_EXTENSION_VERSION = "direct.api.shoppingCartExtension.version";
    public static final String DIRECT_API_SHOPPING_CART_EXTENSION_EXTENSION_ID = "direct.api.shoppingCartExtension.extensionId";
    public static final String DIRECT_API_ENDPOINT_HOST = "direct.api.endpoint.host";
    public static final String DIRECT_API_ENDPOINT_SCHEME = "direct.api.endpoint.scheme";
    public static final String DIRECT_API_ENDPOINT_PORT = "direct.api.endpoint.port";


    // Payment methods

    public static final Integer PAYMENT_METHOD_IDEAL = 809;
    public static final Integer PAYMENT_METHOD_BCC = 3012;


    public enum PAYMENT_METHOD_TYPE {
        CARD("card"),
        REDIRECT("redirect"),
        MOBILE("mobile");

        private final String value;

        PAYMENT_METHOD_TYPE(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private IngenicoogonedirectcoreConstants() {
        //empty to avoid instantiating this constant class
    }


}
