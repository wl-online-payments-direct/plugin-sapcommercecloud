# Worldline Direct Plugin for SAP Commerce Cloud

## Overview

The Worldline Direct plugin for SAP Commerce Cloud connects your SAP Commerce (Hybris) storefront to the Worldline Online Payments platform, enabling you to accept a wide range of payment methods through a single integration. The plugin supports both B2C and B2B commerce scenarios, including recurring payments, saved payment methods, and order replenishment.

**Plugin Version:** 6.0
**Worldline SDK Version:** 6.3.1

## Supported Payment Methods

The plugin supports 45+ payment methods across the following categories:

### Cards

| Payment Method | Product ID |
|---|---|
| Visa | 1 |
| Mastercard | 3 |
| American Express | 2 |
| Diners Club | 132 |
| JCB | 125 |
| Maestro | 117 |
| Discover | 128 |
| UnionPay International | 56 |
| Cartes Bancaires | 130 |

### Wallets and Mobile

| Payment Method | Product ID |
|---|---|
| Apple Pay | 302 |
| Google Pay | 320 |
| PayPal | 840 |
| Alipay | 861 |
| Alipay+ | 5405 |
| WeChat Pay | 863 |

### Bank Transfers and Direct Debit

| Payment Method | Product ID |
|---|---|
| SEPA Direct Debit | 771 |
| iDEAL / WERO | 809 |
| Bank Transfer | 5408 |
| Bancontact | 3012 |
| Multibanco | 5500 |
| Przelewy24 | 3124 |
| EPS | 5406 |
| BLIK | 3204 |
| Bizum | 5001 |
| PostFinance Pay | 3203 |
| Twint | 5407 |
| Linxo Connect | 5003 |

### Buy Now Pay Later

| Payment Method | Product ID |
|---|---|
| Klarna Pay Now | 3301 |
| Klarna Pay Later | 3302 |
| Oney 3x-4x | 5110 |
| Oney Financement Long | 5125 |
| Floa (1x/3x/4x/10x) | 5138-5144 |
| Pledg | 5300 |

### Vouchers and Gift Cards

| Payment Method | Product ID |
|---|---|
| Meal Vouchers | 5402 |
| Intersolve | 5700 |
| Illicado | 3112 |
| Oney Branded Gift Card | 5600 |
| Cheques Vacances Connect | 5403 |
| Cpay | 5100 |

> **Note:** Available payment methods depend on your Worldline merchant configuration and the countries/currencies you operate in.

## Key Features

### Checkout Flows

- **Hosted Checkout Page (HCP)** - Redirect customers to a Worldline-hosted payment page with full PCI DSS compliance
- **Hosted Tokenization Page (HTP)** - Embed a secure card entry form directly in your checkout page for a seamless experience

### Payment Operations

- **Authorization and Capture** - Support for both sale (immediate capture) and authorization-with-later-capture flows
- **Partial Capture** - Capture a portion of the authorized amount (e.g. for partial shipments)
- **Refunds** - Process full or partial refunds from the backoffice
- **Cancellations** - Cancel authorized payments before capture

### Security

- **3D Secure 2** - Full support for 3DS2 authentication with configurable exemptions
- **Exemption Types** - Low value, transaction risk analysis, or no-challenge-request exemptions
- **PCI DSS Compliance** - No card data touches your servers when using Hosted Checkout or Hosted Tokenization

### Recurring Payments and Tokenization

- **Saved Payment Methods** - Customers can save cards and SEPA mandates for future purchases
- **Token Management** - View, block, and revoke saved tokens from the backoffice
- **SEPA Direct Debit Mandates** - Full mandate lifecycle management (create, block, revoke)
- **Order Replenishment** - Scheduled recurring orders using saved payment methods (daily, weekly, monthly, yearly)

### B2B Commerce

- **Approval Workflows** - Integrates with SAP Commerce B2B approval processes
- **Replenishment Orders** - Scheduled automatic reordering for B2B customers
- **Organization-Level Ordering** - Supports B2B organizational structures

### Headless Commerce (OCC)

- Full REST API support via OCC v2 endpoints for headless and composable storefronts
- Endpoints for payment method selection, tokenization, hosted checkout, recurring payments, and saved payment details

### Webhooks

- Asynchronous payment status notifications processed automatically
- Webhook signature verification for security
- Configurable retry mechanism for failed webhook processing
- Automated order status updates based on payment events

### Backoffice Administration

- Manual capture, refund, and cancellation actions
- Partial capture widget for split shipments
- Recurring token and mandate management
- Customer support backoffice integration

### Surcharging

- Optional payment surcharge calculation and display at checkout

## Compatibility

- **SAP Commerce Cloud** 2011 and later
- **Java** 11+

## Plugin Architecture

The plugin consists of the following extensions, all located in `hybris/bin/custom/worldline/`:

| Extension | Purpose |
|---|---|
| `worldlinedirectcore` | Core business logic, services, data models, and payment processing |
| `worldlinedirectb2ccheckoutaddon` | B2C storefront checkout addon |
| `worldlinedirectb2bcheckoutaddon` | B2B storefront checkout addon with approval workflows |
| `worldlinedirectocc` | REST API (OCC v2) endpoints for headless commerce |
| `worldlinedirectwebhook` | Webhook receiver for asynchronous payment notifications |
| `worldlinedirectbackoffice` | Backoffice administration widgets |
| `worldlinedirectcustomersupportbackoffice` | Customer support backoffice features |
| `worldlinedirectfulfilmentprocess` | Order fulfillment process definitions |

## Installation

### Prerequisites

- SAP Commerce Cloud 2011 or later installed and configured
- A Worldline Direct merchant account with API credentials
- Access to the Worldline Merchant Portal

### Step 1: Deploy the Plugin

Copy the `worldline` directory into your SAP Commerce installation at `hybris/bin/custom/`.

### Step 2: Register Extensions

Add the following extensions to your `hybris/config/localextensions.xml`:

```xml
<extension name="worldlinedirectcore"/>
<extension name="worldlinedirectfulfilmentprocess"/>
<extension name="worldlinedirectwebhook"/>
<extension name="worldlinedirectbackoffice"/>
<extension name="worldlinedirectcustomersupportbackoffice"/>
<extension name="worldlinedirectocc"/>
<!-- Include the addon(s) applicable to your storefront: -->
<extension name="worldlinedirectb2ccheckoutaddon"/>
<extension name="worldlinedirectb2bcheckoutaddon"/>
```

### Step 3: Install Storefront Addons

From `hybris/bin/platform/`, run:

```bash
. ./setantenv.sh

# Install standard accelerator addons in yacceleratorstorefront (if not already installed)
ant addoninstall -Daddonnames="adaptivesearchsamplesaddon,assistedservicecustomerinterestsaddon,assistedservicepromotionaddon,assistedservicestorefront,assistedserviceyprofileaddon,captchaaddon,configurablebundleaddon,consignmenttrackingaddon,customercouponaddon,customercouponsamplesaddon,customerinterestsaddon,customerticketingaddon,eventtrackingwsaddon,merchandisingaddon,merchandisingstorefrontsampledataaddon,multicountrysampledataaddon,notificationaddon,ordermanagementaddon,orderselfserviceaddon,pcmbackofficesamplesaddon,personalizationaddon,personalizationsampledataaddon,personalizationyprofilesampledataaddon,profiletagaddon,selectivecartsplitlistaddon,smarteditaddon,stocknotificationaddon,textfieldconfiguratortemplateaddon,timedaccesspromotionengineaddon,timedaccesspromotionenginesamplesaddon,xyformssamples,xyformsstorefrontcommons,ysapproductconfigaddon" -DaddonStorefront.yacceleratorstorefront="yacceleratorstorefront"

# Install the Worldline checkout addon for B2C storefronts
ant addoninstall -Daddonnames="worldlinedirectb2ccheckoutaddon" -DaddonStorefront.yacceleratorstorefront="yacceleratorstorefront"

# Install the Worldline checkout addon for B2B storefronts
ant addoninstall -Daddonnames="worldlinedirectb2bcheckoutaddon" -DaddonStorefront.yacceleratorstorefront="yb2bacceleratorstorefront"
```

### Step 4: Build and Initialize

```bash
# Build the platform
ant clean all

# Initialize the system (first-time setup only - this is destructive)
ant initialize
```

> **Warning:** `ant initialize` will recreate the database. For subsequent updates, use `ant updatesystem` instead.

### Step 5: Configure local.properties

Configure your own `hybris/config/local.properties` for your storefront URLs and environment settings:

```properties
# Storefront URLs (adjust hostnames and ports to match your environment)
website.electronics.http=http\://electronics.local\:9001/yacceleratorstorefront
website.electronics.https=https\://electronics.local\:9002/yacceleratorstorefront
website.apparel-de.http=http\://apparel-de.local\:9001/yacceleratorstorefront
website.apparel-de.https=https\://apparel-de.local\:9002/yacceleratorstorefront
website.apparel-uk.http=http\://apparel-uk.local\:9001/yacceleratorstorefront
website.apparel-uk.https=https\://apparel-uk.local\:9002/yacceleratorstorefront
website.powertools.http=http\://powertools.local\:9001/yb2bacceleratorstorefront
website.powertools.https=https\://powertools.local\:9002/yb2bacceleratorstorefront

# OCC
occ.rewrite.overlapping.paths.enabled=true

# Worldline Hosted Tokenization JS
worldline.hosted.tokenization.js=https://payment.preprod.direct.worldline.com/hostedtokenization/js/client/tokenizer.min.js
```

## Configuration

### Merchant Configuration

Create your Worldline merchant configuration using the following ImpEx:

```impex
INSERT_UPDATE WorldlineConfiguration ; merchantID[unique=true] ; apiKey       ; apiSecret       ; webhookKeyId   ; webhookSecret   ; variant          ; defaultOperationCode(code) ; endpointURL
                                     ; <your-merchant-id>      ; <your-key>   ; <your-secret>   ; <webhook-key>  ; <webhook-secret> ; HTPTemplate.html ; SALE                       ; https://payment.preprod.direct.worldline-solutions.com
```

| Field | Description |
|---|---|
| `merchantID` | Your Worldline merchant identifier |
| `apiKey` | API key from the Worldline Merchant Portal |
| `apiSecret` | API secret from the Worldline Merchant Portal |
| `webhookKeyId` | Webhook key ID for signature verification |
| `webhookSecret` | Webhook secret for signature verification |
| `variant` | Hosted Tokenization Page template (e.g. `HTPTemplate.html`) |
| `defaultOperationCode` | `SALE` for immediate capture, or `FINAL_AUTHORIZATION` for authorize-then-capture |
| `endpointURL` | Worldline API endpoint URL |

### Assign Configuration to BaseStore

Link your merchant configuration to one or more storefronts:

```impex
UPDATE BaseStore ; uid[unique=true] ; paymentProvider ; checkoutFlowGroup                    ; submitOrderProcessCode  ; createReturnProcessCode   ; worldlineConfiguration(merchantID)
                 ; <your-store>     ; WORLDLINE       ; worldlineB2CResponsiveCheckoutGroup  ; worldline-order-process ; worldline-return-process  ; <your-merchant-id>
```

### Hosted Tokenization JavaScript

Add the Worldline tokenization JavaScript URL to `hybris/config/local.properties`:

```properties
# Pre-production / sandbox
worldline.hosted.tokenization.js=https://payment.preprod.direct.worldline.com/hostedtokenization/js/client/tokenizer.min.js

# Production (update when going live)
# worldline.hosted.tokenization.js=https://payment.direct.worldline.com/hostedtokenization/js/client/tokenizer.min.js
```

### Webhook Endpoint

Configure your webhook endpoint in the Worldline Merchant Portal. The plugin exposes the following URL:

```
https://<your-domain>/worldlinedirectwebhook/webhook
```

Ensure this URL is accessible from the Worldline platform and configure the webhook key ID and secret in your merchant configuration.

### Advanced Configuration Options

The following options can be set on the `WorldlineConfiguration` item via ImpEx or the backoffice:

| Option | Default | Description |
|---|---|---|
| `defaultOperationCode` | `SALE` | `SALE` for immediate capture; `FINAL_AUTHORIZATION` to capture later |
| `captureTimeFrame` | `0` | Hours until automatic capture (0 = immediate) |
| `askConsumerConsent` | `true` | Prompt customers to save their payment method |
| `groupCards` | `false` | Group card payment methods into a single option at checkout |
| `applySurcharge` | `false` | Enable payment surcharging |
| `enable3DS` | `true` | Enable 3D Secure authentication |
| `enableMandatory3DS` | `false` | Force 3DS challenge for all transactions |
| `exemptionType3DS` | - | 3DS exemption type (low value, transaction risk analysis, etc.) |
| `exemptionLimit3DS` | - | Maximum transaction amount for 3DS exemptions |
| `sessionTimeout` | - | Payment session timeout in seconds |
| `submitOrderPromotion` | `false` | Submit promotion details to the payment gateway |
| `hostedCheckoutVariant` | `SimplifiedCustomPaymentPage` | Hosted Checkout Page template |
| `firstRecurringPayment` | `false` | Process the first recurring payment immediately |
| `replenishmentAttempts` | - | Number of retry attempts for failed replenishment orders |

### Account Payment Details Page

To enable the saved payment methods page in the storefront My Account section, run the following ImpEx (adjust `$contentCatalog` for your site):

```impex
$contentCatalog = <your-site>ContentCatalog
$contentCV = catalogVersion(CatalogVersion.catalog(Catalog.id[default=$contentCatalog]), CatalogVersion.version[default=Staged])[default=$contentCatalog:Staged]
$jspPath = /WEB-INF/views/addons/worldlinedirectb2ccheckoutaddon/responsive/pages/account/payment-details/worldlineAccountPaymentInfoPage.jsp

UPDATE CMSLinkComponent ; $contentCV[unique=true] ; uid[unique=true]   ; name                              ; url
                        ;                         ; PaymentDetailsLink ; WorldlineAccountPaymentDetailsLink ; /my-account/worldline/payment-details

INSERT_UPDATE JspIncludeComponent ; $contentCV[unique=true] ; uid[unique=true]               ; name                                       ; page
                                  ;                         ; AccountPaymentDetailsComponent ; Worldline Account Payment Details Component ; $jspPath
```

Remember to synchronize the content catalog after importing into the Staged version.

### Custom Currencies

If you need to support additional currencies beyond the defaults, use the following ImpEx to create currencies and associated delivery modes. Adjust the `baseStores`, `facetSearchConfigs`, conversion rates, and zone codes to match your configuration:

```impex
INSERT_UPDATE Currency ; isocode[unique=true] ; name[lang=en]         ; active ; base  ; conversion ; digits ; symbol ; baseStores(uid)[mode=append] ; facetSearchConfigs(name)
                       ; GBP                  ; Pound sterling        ; true   ; true  ; 1          ; 2      ; £      ; apparel-uk                   ; apparel-ukIndex
                       ; AED                  ; UAE Dirham            ; true   ; false ; 5.02       ; 2      ; AED    ; apparel-uk                   ; apparel-ukIndex
                       ; AUD                  ; Australian dollar     ; true   ; false ; 1.88       ; 2      ; AU$    ; apparel-uk                   ; apparel-ukIndex
                       ; CHF                  ; Swiss franc           ; true   ; false ; 3.96       ; 2      ; CHF    ; apparel-uk                   ; apparel-ukIndex
                       ; DKK                  ; Danish Krone          ; true   ; false ; 8.67       ; 2      ; Kr.    ; apparel-uk                   ; apparel-ukIndex
                       ; EUR                  ; Euro                  ; true   ; false ; 1.17       ; 2      ; €      ; apparel-uk                   ; apparel-ukIndex
                       ; HKD                  ; Hong Kong dollar      ; true   ; false ; 10.64      ; 2      ; HK$    ; apparel-uk                   ; apparel-ukIndex
                       ; JPY                  ; Japanese Yen          ; true   ; false ; 1.5        ; 0      ; ¥      ; apparel-uk                   ; apparel-ukIndex
                       ; NOK                  ; Norwegian Krone       ; true   ; false ; 11.92      ; 2      ; kr     ; apparel-uk                   ; apparel-ukIndex
                       ; NZD                  ; New Zealand Dollar    ; true   ; false ; 1.94       ; 2      ; $      ; apparel-uk                   ; apparel-ukIndex
                       ; PLN                  ; Polish złoty          ; true   ; false ; 5.37       ; 2      ; zł     ; apparel-uk                   ; apparel-ukIndex
                       ; SEK                  ; Swedish Krona         ; true   ; false ; 11.90      ; 2      ; kr     ; apparel-uk                   ; apparel-ukIndex
                       ; SGD                  ; Singapore Dollar      ; true   ; false ; 1.85       ; 2      ; S$     ; apparel-uk                   ; apparel-ukIndex
                       ; USD                  ; United States Dollar  ; true   ; false ; 1.37       ; 2      ; $      ; apparel-uk                   ; apparel-ukIndex

INSERT_UPDATE ZoneDeliveryModeValue ; currency(isocode)[unique=true] ; deliveryMode(code)[unique=true] ; minimum ; value ; zone(code)[unique=true]
                                    ; AED                            ; standard-gross                  ; 12      ; 123   ; uk
                                    ; AUD                            ; standard-gross                  ; 12      ; 123   ; uk
                                    ; CHF                            ; standard-gross                  ; 12      ; 123   ; uk
                                    ; DKK                            ; standard-gross                  ; 12      ; 123   ; uk
                                    ; EUR                            ; standard-gross                  ; 12      ; 123   ; uk
                                    ; HKD                            ; standard-gross                  ; 12      ; 123   ; uk
                                    ; JPY                            ; standard-gross                  ; 12      ; 123   ; uk
                                    ; NOK                            ; standard-gross                  ; 12      ; 123   ; uk
                                    ; NZD                            ; standard-gross                  ; 12      ; 123   ; uk
                                    ; PLN                            ; standard-gross                  ; 12      ; 123   ; uk
                                    ; SEK                            ; standard-gross                  ; 12      ; 123   ; uk
                                    ; SGD                            ; standard-gross                  ; 12      ; 123   ; uk
                                    ; USD                            ; standard-gross                  ; 12      ; 123   ; uk
```

> **Note:** Update the conversion rates to reflect current exchange rates for your base currency. The values above are examples only.

## API Reference (OCC v2)

The plugin provides the following REST API endpoints for headless integrations:

### Cart Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/{baseSiteId}/users/{userId}/carts/{cartId}/paymentProducts` | List available payment methods |
| `POST` | `/{baseSiteId}/users/{userId}/carts/{cartId}/paymentProducts` | Select a payment method |
| `GET` | `/{baseSiteId}/users/{userId}/carts/{cartId}/checkoutType` | Get checkout type (HCP or HTP) |
| `GET` | `/{baseSiteId}/users/{userId}/carts/{cartId}/hostedTokenization` | Get tokenization session |
| `GET` | `/{baseSiteId}/users/{userId}/carts/{cartId}/worldlinePaymentdetails` | List saved payment methods |

### Order Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/{baseSiteId}/users/{userId}/orders/hostedTokenization` | Place order via Hosted Tokenization |
| `POST` | `/{baseSiteId}/users/{userId}/orders/hostedCheckout` | Place order via Hosted Checkout |
| `POST` | `/{baseSiteId}/users/{userId}/orders/{orderCode}/hostedTokenization/return` | Handle tokenization return |
| `POST` | `/{baseSiteId}/users/{userId}/orders/{orderCode}/hostedCheckout/return` | Handle checkout return |

Equivalent B2B endpoints are available under `/{baseSiteId}/orgUsers/{userId}/orders/`.

## Scheduled Jobs

The plugin includes the following cronjobs that are created automatically during initialization:

| Job | Purpose |
|---|---|
| `WorldlineProcessWebhooksEventJob` | Processes queued webhook events from Worldline |
| `WorldlineAutomaticCaptureJob` | Automatically captures authorized payments after the configured `captureTimeFrame` |
| `WorldlineAcceleratorCartToOrderJob` | Converts replenishment carts into orders on their scheduled dates |

## Support

For technical support and documentation, contact your Worldline account representative or visit the [Worldline Developer Portal](https://docs.direct.worldline-solutions.com/).

## License

This plugin is provided under the terms of your Worldline merchant agreement. Copyright Worldline.
