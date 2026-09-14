import de.hybris.platform.servicelayer.search.FlexibleSearchQuery

// ----------------------------------------------------------------------------
// Worldline GoPay rename - database migration
// ----------------------------------------------------------------------------
// Run this ONCE, in the hAC Scripting console (Groovy), AFTER deploying the
// renamed extensions and running a system update.
//
// It repairs the database references that the rename invalidates. Neither of
// these is fixed by `ant clean all` or by a system update, because both are
// DATA, not type-system metadata:
//
//   1. JspIncludeComponent.page - CMS components still pointing at
//      /WEB-INF/views/addons/worldlinedirect{b2c,b2b}checkoutaddon/...
//      Symptom: blank or erroring content slots on cart, checkout, account
//      and replenishment pages.
//
//   2. RendererTemplate.contextClass - email templates still pointing at
//      com.worldline.direct.email.context.* (from the earlier package rename).
//      Symptom: replenishment payment-failure and cart-non-valid emails throw
//      ClassNotFoundException at render time.
//
// This is deliberately a script rather than an ImpEx: the same component uid
// maps to a different addon path in the B2B and B2C content catalogs, so an
// ImpEx would need one hand-maintained variant per merchant catalog. A pattern
// rewrite is catalog-agnostic and also catches components the merchant created
// themselves against the old addon path.
// ----------------------------------------------------------------------------

// CONFIGURATION
boolean dryRun = true          // set to false to persist

println ">>> WORLDLINE GOPAY RENAME MIGRATION <<<"
println "Config: [dryRun: ${dryRun}]"

def flexibleSearchService = spring.getBean("flexibleSearchService")
def modelService = spring.getBean("modelService")
def searchRestrictionService = spring.getBean("searchRestrictionService")

// CMS items live in catalog versions; without this, session restrictions can
// hide the Online version and the migration would silently do half the job.
searchRestrictionService.disableSearchRestrictions()

int totalChanged = 0

def rewrite = { String typeCode, String attr, String likePattern, Closure transform ->
    String q = "SELECT {pk} FROM {${typeCode}} WHERE {${attr}} LIKE ?pattern"
    FlexibleSearchQuery query = new FlexibleSearchQuery(q)
    query.addQueryParameter("pattern", likePattern)

    def items = flexibleSearchService.search(query).getResult()
    println ""
    println "--- ${typeCode}.${attr} : ${items.size()} row(s) matched '${likePattern}'"

    int changed = 0
    items.each { item ->
        String oldValue = item.getProperty(attr)
        String newValue = transform(oldValue)
        if (newValue != null && newValue != oldValue) {
            println "    ${item.getProperty('uid') ?: item.getProperty('code') ?: item.pk}"
            println "      ${oldValue}"
            println "   -> ${newValue}"
            if (!dryRun) {
                item.setProperty(attr, newValue)
                modelService.save(item)
            }
            changed++
        }
    }
    println "--- ${typeCode}.${attr} : ${changed} row(s) ${dryRun ? 'would be' : ''} updated"
    return changed
}

// 1. CMS component JSP paths -> new addon names
totalChanged += rewrite("JspIncludeComponent", "page", "%addons/worldlinedirect%") { v ->
    v.replace("addons/worldlinedirect", "addons/worldlinegopay")
}

// 2. Email renderer contexts -> new package
totalChanged += rewrite("RendererTemplate", "contextClass", "com.worldline.direct%") { v ->
    v.replace("com.worldline.direct", "com.worldline.gopay")
}

println ""
if (dryRun) {
    println ">>> DRY RUN COMPLETE. ${totalChanged} row(s) would change."
    println ">>> Review the output above, then set dryRun = false and re-run."
} else {
    println ">>> MIGRATION COMPLETE. ${totalChanged} row(s) updated."
    println ">>> Now clear the CMS/page caches (or restart) so the new paths are picked up."
}
