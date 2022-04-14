
# SAP Worldline Direct plugin

SAP Worldline Direct plugin is designed specifically to connect your SAP eCommerce platform to our trusted payment engine.

## Installation

- Clone the project from BitBucket.
- Unzip Hybris folder in the cloned project.
- Execute following commands

```bash
# install addons in yacceleratorstorefront
ant addoninstall -Daddonnames="adaptivesearchsamplesaddon,assistedservicecustomerinterestsaddon,assistedservicepromotionaddon,assistedservicestorefront,assistedserviceyprofileaddon,captchaaddon,configurablebundleaddon,consignmenttrackingaddon,customercouponaddon,customercouponsamplesaddon,customerinterestsaddon,customerticketingaddon,eventtrackingwsaddon,merchandisingaddon,merchandisingstorefrontsampledataaddon,multicountrysampledataaddon,notificationaddon,ordermanagementaddon,orderselfserviceaddon,pcmbackofficesamplesaddon,personalizationaddon,personalizationsampledataaddon,personalizationyprofilesampledataaddon,profiletagaddon,selectivecartsplitlistaddon,smarteditaddon,stocknotificationaddon,textfieldconfiguratortemplateaddon,timedaccesspromotionengineaddon,timedaccesspromotionenginesamplesaddon,xyformssamples,xyformsstorefrontcommons,ysapproductconfigaddon" -DaddonStorefront.yacceleratorstorefront="yacceleratorstorefront"

# install our checkoutaddon in yacceleratorstorefront
ant addoninstall -Daddonnames="worldlinedirectb2ccheckoutaddon" -DaddonStorefront.yacceleratorstorefront="yacceleratorstorefront"

# initialize
ant initialize
```

- Configure your own local.properties in config folder

```
recaptcha.publickey=
recaptcha.privatekey=
googleApiKey=
website.electronics.http=http\://electronics.local\:9001/yacceleratorstorefront
website.electronics.https=https\://electronics.local\:9002/yacceleratorstorefront
website.apparel-de.http=http\://apparel-de.local\:9001/yacceleratorstorefront
website.apparel-de.https=https\://apparel-de.local\:9002/yacceleratorstorefront
website.apparel-uk.http=http\://apparel-uk.local\:9001/yacceleratorstorefront
website.apparel-uk.https=https\://apparel-uk.local\:9002/yacceleratorstorefront
website.powertools.http=http\://powertools.local\:9001/yb2bacceleratorstorefront
website.powertools.https=https\://powertools.local\:9002/yb2bacceleratorstorefront
yb2bacceleratorstorefront.illegalrequirementstest.excluded=true
multicountrysampledataaddon.import.active=false
occ.rewrite.overlapping.paths.enabled=true
backoffice.solr.search.index.autoinit=false
solrserver.instances.default.autostart=false
initialpassword.admin=nimda
standalone.javaoptions=-Xmx4g -Djava.locale.providers=COMPAT,CLDR
installed.tenants=

# HAC webroot
hac.webroot=/hac

# Colors in logs
ansi.colors=true
log4j2.appender.console.layout.pattern=%highlight{[%p] - [%t]- %magenta{Class: [}%X{RemoteAddr}%X{Tenant}%X{CronJob}%c{1}%magenta{]}%n%magenta{==>} %cyan{%msg}%n%throwable }

# Activate Session FailOver
spring.session.enabled=true
spring.session.yacceleratorstorefront.save=sync
spring.session.yacceleratorstorefront.cookie.name=JSESSIONID
spring.session.yacceleratorstorefront.cookie.path=/

# Configure dedicated order Prefix
keygen.order.code.template=MAROU$

# activate mode dev on Backoffice (Performance Impact!!!)
backoffice.cockpitng.reset.triggers=start
backoffice.cockpitng.reset.scope=widgets,cockpitConfig
backoffice.cockpitng.additionalResourceLoader.enabled=true
backoffice.cockpitng.uifactory.cache.enabled=false
backoffice.cockpitng.widgetclassloader.resourcecache.enabled=false
backoffice.cockpitng.resourceloader.resourcecache.enabled=false

#worldlineJS
worldline.hosted.tokenization.js=https://payment.preprod.direct.ingenico.com/hostedtokenization/js/client/tokenizer.min.js


```

## Configuration

- Execute this impex to create the configuration for the accelerator storefronts


```
$htpTemplate=HTPTemplate.html
$htpTemplate=HTPTemplate11_Full.html
INSERT_UPDATE WorldlineConfiguration; merchantID[unique=true]           ; apiKey               ; apiSecret                                        ; webhookKeyId                   ; webhookSecret                        ; variant          ; defaultOperationCode(code); endpointURL;
                                   ; greenlightcommerce1  ; 052BEFE7F9CD932D58D1 ; NGMyZmU0M2EtMjYzMi00YmFkLWI2YjMtYTcyZTJjZTI3ZTNi ; dfe212d95b794eb5971331c8f0ea82 ; ec73cb3b-e771-4fcb-a393-2bd6711351c9 ; $htpTemplate ; SALE                      ; https://payment.preprod.direct.ingenico.com
                                   ; greenlightcommerceAP ; 052BEFE7F9CD932D58D1 ; NGMyZmU0M2EtMjYzMi00YmFkLWI2YjMtYTcyZTJjZTI3ZTNi ; dfe212d95b794eb5971331c8f0ea82 ; ec73cb3b-e771-4fcb-a393-2bd6711351c9 ; $htpTemplate ; SALE                      ; https://payment.preprod.direct.ingenico.com    
                                   

UPDATE BaseStore; uid[unique = true]; paymentProvider; checkoutFlowGroup      ; submitOrderProcessCode ; createReturnProcessCode
                ; apparel-uk        ; WORLDLINE  ; worldlineB2CResponsiveCheckoutGroup ; worldline-order-process ;worldline-return-process
                ; apparel-de        ; WORLDLINE  ; worldlineB2CResponsiveCheckoutGroup ; worldline-order-process ;worldline-return-process
                ; electronics       ; WORLDLINE  ; worldlineB2CResponsiveCheckoutGroup ; worldline-order-process ;worldline-return-process              

UPDATE BaseStore; uid[unique = true]; worldlineConfiguration(merchantID)
                ; apparel-uk        ; greenlightcommerce1
                ; apparel-de        ; greenlightcommerce1
                ; electronics       ; greenlightcommerce1
                
```

###### Custom Currencies
- Run this Impex to create more currencies and delivery modes to work with newly added currencies :

```
INSERT_UPDATE Currency;isocode[unique=true];name[lang=en];active;base;conversion;digits;symbol;baseStores(uid)[mode=append];facetSearchConfigs(name)
;GBP;Pound sterling;true;true;1;2;£;apparel-uk;apparel-ukIndex
;AED;UAE Dirham;true;false;5.02;2;AED;apparel-uk;apparel-ukIndex
;AUD;Australian dollar;true;false;1.88;2;AU$;apparel-uk;apparel-ukIndex
;CHF;Swiss franc;true;false;3.96;2;CHF;apparel-uk;apparel-ukIndex
;DKK;Danish Krone;true;false;8.67;2;Kr.;apparel-uk;apparel-ukIndex
;EUR;Euro;true;false;1.17;2;€;apparel-uk;apparel-ukIndex
;HKD;Hong Kong dollar;true;false;10.64;2;HK$;apparel-uk;apparel-ukIndex
;HRK;Croatian Kuna;true;false;8.75;2;kn;apparel-uk;apparel-ukIndex
;JPY;Japanese Yen;true;false;1.5;0;¥;apparel-uk;apparel-ukIndex
;MYR;Malaysian Ringgit;true;false;5.73;2;RM;apparel-uk;apparel-ukIndex
;NOK;Norwegian Krone;true;false;11.92;2;kr;apparel-uk;apparel-ukIndex
;NZD;New Zealand Dollar;true;false;1.94;2;$;apparel-uk;apparel-ukIndex
;PLN;Polish złoty;true;false;5.37;2;zł;apparel-uk;apparel-ukIndex
;SEK;Swedish Krona;true;false;11.90;2;kr;apparel-uk;apparel-ukIndex
;SGD;Singapore Dollar;true;false;1.85.54;2;S$;apparel-uk;apparel-ukIndex
;USD;United States Dollar;true;false;1.37;2;$;apparel-uk;apparel-ukIndex

INSERT_UPDATE ZoneDeliveryModeValue;currency(isocode)[unique=true];deliveryMode(code)[unique=true];minimum;value;zone(code)[unique=true];
;PLN;standard-gross;12;123;uk
;AED;standard-gross;12;123;uk
;AUD;standard-gross;12;123;uk
;CHF;standard-gross;12;123;uk
;DKK;standard-gross;12;123;uk
;EUR;standard-gross;12;123;uk
;HKD;standard-gross;12;123;uk
;HRK;standard-gross;12;123;uk
;JPY;standard-gross;12;123;uk
;MYR;standard-gross;12;123;uk
;NOK;standard-gross;12;123;uk
;NZD;standard-gross;12;123;uk
;PLN;standard-gross;12;123;uk
;SEK;standard-gross;12;123;uk
;SGD;standard-gross;12;123;uk
;USD;standard-gross;12;123;uk
```

## Edit Account Payment Detail Page

- Execute this impex to edit Account Payment Detail Page for the accelerator storefronts
- Don't forget to synchronize or execute it in Online version.
- This impex includes Apparel-uk as an example.

```
$contentCatalog = apparel-ukContentCatalog
$contentCV = catalogVersion(CatalogVersion.catalog(Catalog.id[default=$contentCatalog]), CatalogVersion.version[default=Staged])[default=$contentCatalog:Staged]
$jspPath = /WEB-INF/views/addons/worldlinedirectb2ccheckoutaddon/responsive/pages/account/payment-details/worldlineAccountPaymentInfoPage.jsp
UPDATE CMSLinkComponent; $contentCV[unique = true]; uid[unique = true] ; name                              ; url
                       ;                          ; PaymentDetailsLink ; WorldlineAccountPaymentDetailsLink ; /my-account/worldline/payment-details
INSERT_UPDATE JspIncludeComponent; $contentCV[unique = true]; uid[unique = true]             ; name                                       ; page
                                 ;                          ; AccountPaymentDetailsComponent ; Worldline Account Payment Details Component ; $jspPath

```