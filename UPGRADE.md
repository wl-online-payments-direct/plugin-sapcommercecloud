# Upgrading to Worldline GoPay

This release renames the plugin from **Worldline Direct** to **Worldline GoPay**.
The change is mechanical but it touches extension names, the Java package, and
some values that live in your database, so it cannot be picked up by a rebuild
alone.

Read this whole page before you start. Steps 1 to 6 are required. Allow for a
full `ant clean all` plus a system update.

## What changed

| Area | Before | After |
|---|---|---|
| Extension names | `worldlinedirectcore`, `worldlinedirectocc`, ... | `worldlinegopaycore`, `worldlinegopayocc`, ... |
| Java package | `com.worldline.direct` | `com.worldline.gopay` |
| Generated class prefix | `Worldlinedirectcore*` | `Worldlinegopaycore*` |
| Config property keys | `worldline.direct.*` | `worldline.gopay.*` (old keys still honoured, see step 3) |
| Extension-scoped keys | `worldlinedirect<ext>.*` | `worldlinegopay<ext>.*` |
| Facade interface | `WorldlineDirectCheckoutFacade` | `WorldlineGoPayCheckoutFacade` |
| Merchant-facing wording | "Worldline" / "Worldline Direct" | "Worldline GoPay" |

## What did NOT change

These are deliberately untouched so that your data, integrations and customisations
keep working:

- **Item types and their database tables.** `WorldlineConfiguration`,
  `WorldlinePaymentInfo`, `WorldlineMandate`, `WorldlineRecurringToken`,
  `WorldlineWebhooksEvent` and every attribute (`BaseStore.worldlineConfiguration`,
  `Order.worldlineSurchargeAmount`, ...) keep their names. No data migration, and
  your existing ImpEx scripts, flexible searches and saved queries still work.
- **Java class and interface names**, other than the one facade listed above, and
  **all Spring bean ids**. Extensions that inject or override
  `WorldlinePaymentService`, `worldlineCheckoutFacade` and friends still compile
  and start.
- **Enum values and DB-stored codes**: `paymentProvider = WORLDLINE`,
  `worldline-order-process`, `worldline-return-process`,
  `worldlineB2CResponsiveCheckoutGroup`, all CMS component uids.
- **The webhook URL**: still `https://<your-domain>/worldline/webhook`. It comes
  from the `webroot` in `extensioninfo.xml`, not the extension name, so **you do
  not need to reconfigure your endpoint in the Worldline Merchant Portal.**
- **The OCC web root**: still `/occ/v2`. Headless and mobile clients are unaffected.
- **Gateway hostnames and the SDK**: `payment.direct.ingenico.com`,
  `*.direct.worldline-solutions.com` and the
  `com.worldline-solutions:onlinepayments-sdk-java` dependency are Worldline's own
  and are unchanged. The word "direct" in these is not plugin branding.
- **Storefront URLs** such as `/my-account/worldline/payment-details`. No SEO or
  bookmark impact.

## Upgrade steps

### 1. Swap the extensions

Remove the old `worldlinedirect*` directories from `hybris/bin/custom/worldline/`
and drop in the new `worldlinegopay*` ones.

Then update `hybris/config/localextensions.xml`:

```xml
<extension dir="${HYBRIS_BIN_DIR}/custom/worldline/worldlinegopaycore"/>
<extension dir="${HYBRIS_BIN_DIR}/custom/worldline/worldlinegopayb2ccheckoutaddon"/>
<extension dir="${HYBRIS_BIN_DIR}/custom/worldline/worldlinegopayb2bcheckoutaddon"/>
<extension dir="${HYBRIS_BIN_DIR}/custom/worldline/worldlinegopaybackoffice"/>
<extension dir="${HYBRIS_BIN_DIR}/custom/worldline/worldlinegopayocc"/>
<extension dir="${HYBRIS_BIN_DIR}/custom/worldline/worldlinegopaywebhook"/>
<extension dir="${HYBRIS_BIN_DIR}/custom/worldline/worldlinegopayfulfilmentprocess"/>
<extension dir="${HYBRIS_BIN_DIR}/custom/worldline/worldlinegopaycustomersupportbackoffice"/>
```

### 2. Update your own code, if you have any

If you have extensions that build on this plugin, apply these three renames:

```
com.worldline.direct            ->  com.worldline.gopay
WorldlineDirectCheckoutFacade   ->  WorldlineGoPayCheckoutFacade
worldlinedirect<ext>            ->  worldlinegopay<ext>   (extension dependencies)
```

Nothing else in the Java API changed, so this is a find-and-replace rather than a
code review.

### 3. Check your property overrides

The `worldline.direct.*` keys are **still read** as a deprecated fallback, so any
override you have in `local.properties` keeps working:

```properties
# still honoured after the upgrade
worldline.direct.api.connectTimeout=20000
```

Move to the new names when convenient; the new key wins if both are set:

```properties
worldline.gopay.api.connectTimeout=20000
```

The aliases cover `worldline.direct.api.*` and
`worldline.direct.featurerequest.mailto`. They are scheduled for removal in a
future major release.

**Not aliased**, because the platform resolves these by extension name and no
fallback is possible. If you override any of them, rename them by hand:

| Old key | New key |
|---|---|
| `worldlinedirectb2ccheckoutaddon.css.paths.responsive` | `worldlinegopayb2ccheckoutaddon.css.paths.responsive` |
| `worldlinedirectb2ccheckoutaddon.javascript.paths.responsive` | `worldlinegopayb2ccheckoutaddon.javascript.paths.responsive` |
| `worldlinedirectb2bcheckoutaddon.css.paths.responsive` | `worldlinegopayb2bcheckoutaddon.css.paths.responsive` |
| `worldlinedirectb2bcheckoutaddon.javascript.paths.responsive` | `worldlinegopayb2bcheckoutaddon.javascript.paths.responsive` |
| `worldlinedirectwebhook.*` | `worldlinegopaywebhook.*` |
| `worldlinedirectocc.*` | `worldlinegopayocc.*` |
| `worldlinedirectbackoffice.key` | `worldlinegopaybackoffice.key` |
| `worldlinedirectcustomersupportbackoffice.*` | `worldlinegopaycustomersupportbackoffice.*` |
| `yacceleratorstorefront.additionalWebSpringConfigs.worldlinedirectb2ccheckoutaddon` | `...worldlinegopayb2ccheckoutaddon` |
| `yacceleratorstorefront.wro4jconfigscan.worldlinedirectb2ccheckoutaddon` | `...worldlinegopayb2ccheckoutaddon` |
| `yb2bacceleratorstorefront.additionalWebSpringConfigs.worldlinedirectb2bcheckoutaddon` | `...worldlinegopayb2bcheckoutaddon` |
| `yb2bacceleratorstorefront.wro4jconfigscan.worldlinedirectb2bcheckoutaddon` | `...worldlinegopayb2bcheckoutaddon` |

### 4. Reinstall the checkout addons

**Back up any customisations first.** If you edited JSPs, tags or CSS inside
`<storefront>/web/webroot/WEB-INF/views/addons/worldlinedirectb2ccheckoutaddon/`
(or the b2b equivalent, or `_ui/addons/`, `tags/addons/`, `messages/addons/`),
those files live in a directory named after the old addon and will be orphaned.
Copy them out now, then reapply them to the new addon folder afterwards.

Remove the old installed addon directories from each storefront. Do **not** bound
the search depth: on a standard layout the b2c addon sits 11 levels down, under
`hybris/bin/modules/base-accelerator/deprecated/yacceleratorstorefront/web/webroot/WEB-INF/views/addons/`.

```bash
find hybris/bin -type d -name "worldlinedirect*" -exec rm -rf {} +
```

Now clear the three places the old addon name is recorded outside those
directories. **Missing the first one makes `ant clean all` fail immediately** with
`Missing required extensions: worldlinedirectb2ccheckoutaddon<-[yacceleratorstorefront]`:

1. Each storefront's `extensioninfo.xml` carries a `requires-extension` entry that
   `addoninstall` wrote. Repoint it:

   ```xml
   <!-- yacceleratorstorefront/extensioninfo.xml -->
   <requires-extension name="worldlinegopayb2ccheckoutaddon"/>
   <!-- yb2bacceleratorstorefront/extensioninfo.xml -->
   <requires-extension name="worldlinegopayb2bcheckoutaddon"/>
   ```

2. `<storefront>/web/webroot/WEB-INF/_ui-src/responsive/lib/ybase-0.1.0/less/addons.less`
   has an `@import` for the old addon's `.less`. Delete that line; the reinstall
   below adds the new one. Leaving it pointed at a deleted file breaks the wro4j
   build.

3. `<storefront>/.classpath`, if you use the Eclipse or IntelliJ project files.
   Regenerated by `ant eclipse` / `ant idea`, or just rename the entry by hand.

Also delete the stale backoffice widget jars so they are rebuilt, otherwise the
backoffice keeps loading widget definitions that reference the old package:

```bash
rm -f hybris/bin/custom/worldline/worldlinegopay*/resources/backoffice/*_bof.jar
```

Then reinstall under the new names:

```bash
cd hybris/bin/platform
. ./setantenv.sh
ant addoninstall -Daddonnames="worldlinegopayb2ccheckoutaddon" -DaddonStorefront.yacceleratorstorefront="yacceleratorstorefront"
ant addoninstall -Daddonnames="worldlinegopayb2bcheckoutaddon" -DaddonStorefront.yb2bacceleratorstorefront="yb2bacceleratorstorefront"
```

### 5. Clean build and system update

```bash
cd hybris/bin/platform
. ./setantenv.sh
ant clean all
```

`ant clean` does not always remove `gensrc/` and `classes/`. If the build reports
missing `Generated*` classes, clear them by hand and rebuild:

```bash
find hybris/bin/custom/worldline -maxdepth 3 -type d \( -name gensrc -o -name classes \) -exec rm -rf {} +
```

Then run a **system update**, either from hAC (Platform > Update > Update running
system, project data unchecked) or:

```bash
ant updatesystem
```

This is required, not optional. The jalo class for each item type is persisted on
its `ComposedType` row, so without it you will get:

```
java.lang.ClassCastException: class de.hybris.platform.jalo.GenericItem cannot be
cast to class com.worldline.gopay.jalo.WorldlineConfiguration
```

### 6. Run the database migration

Two things live in your database as data and survive a system update untouched:
CMS component JSP paths pointing at the old addon folders, and email renderer
context classes pointing at the old package.

Run `worldlinegopaycore/resources/migration/gopay-rename-migration.groovy` in the
hAC Scripting console (Groovy). It defaults to `dryRun = true`: review the output,
then set `dryRun = false` and run it again.

Skipping this leaves blank content slots on the cart, checkout, account and
replenishment pages, and makes replenishment failure emails throw
`ClassNotFoundException` when they render.

Finally, clear the Tomcat compiled-JSP cache so stale addon pages are not served:

```bash
rm -rf hybris/bin/platform/tomcat/work/Catalina
```

## Verification checklist

- [ ] Storefront checkout reaches the payment-method page and lists payment products
- [ ] Cart and checkout content slots render (step 6)
- [ ] Saved payment methods appear under My Account
- [ ] A test payment authorises and the webhook is received on `/worldline/webhook`
- [ ] Backoffice shows the Worldline GoPay sections, and Test Connection succeeds
- [ ] A replenishment failure email renders without error (step 6)
- [ ] `SELECT {code} FROM {RendererTemplate} WHERE {contextClass} LIKE 'com.worldline.direct%'` returns nothing
- [ ] `SELECT {uid} FROM {JspIncludeComponent} WHERE {page} LIKE '%worldlinedirect%'` returns nothing

## Upgrading from 6.0 to 7.0

Release 7.0 keeps all item types and tables. Every data model change is additive
except one, so a clean build plus a system update is enough:

```bash
ant clean all
ant updatesystem
```

After the system update:

1. **Re-select the Wero capture trigger.** The `WeroCaptureTrigger` enum values
   were re-cased to match the Worldline GoPay API (`SHIPPING` became `shipping`,
   `SERVICEFULFILMENT` became `serviceFulfilment`, and so on). A trigger saved
   under 6.0 no longer matches a valid value, so open each Worldline Configuration
   in the Backoffice and choose the trigger again. Merchants that do not offer
   Wero can skip this.
2. **Review the new configuration attributes.** `WorldlineConfiguration` gains
   Google Pay settings (`googlePayMerchantId`, `googlePayMerchantName`,
   `googlePayEnvironment`, `googlePayAcquirerCountry`), Pay by Link settings
   (`paymentLinkExpirationHours`, `paymentLinkReturnUrl`, `paymentLinkLogo`),
   `showMobileWalletsFirst` and `merchantName`. All are optional; Google Pay and
   Pay by Link stay inactive until configured.
3. **Re-import the payment modes** if you use the shipped
   `projectdata-paymentmodes.impex`. It adds Wero (900) and in3 (5410) and renames
   Pledg to Sofinco (product 5300 is unchanged).
4. **Reinstall the checkout addons** (step 4 above) so the storefronts pick up the
   new Google Pay, Apple Pay and Pay by Link templates and scripts.

`WorldlinePaymentInfo` and `WorldlineRecurringToken` gain new optional attributes
(transaction details, Google Pay data, Pay by Link data, `initialPaymentId`). They
are created by the system update and populated for new payments only.
