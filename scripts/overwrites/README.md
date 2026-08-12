# CAWL Overwrites Directory

This directory contains CAWL-branded replacement files that are copied over the
converted output **after** `cawl.sh` completes its find/replace pass (Step 8).

The directory structure mirrors the **post-conversion** output (i.e., all
`worldline` references have already been renamed to `cawl`). Files placed here
will overwrite the corresponding files in `cawl_output/` via `rsync`.

## When to use this

The `cawl.sh` script performs a mechanical find/replace on localisation files,
which works for property **keys** (e.g. renaming `worldlinedirect` prefixes).
However, localisation **values** (user-visible strings) may need hand-crafted
CAWL-specific wording that a simple find/replace cannot produce.

Place fully authored CAWL localisation files here. They will replace the
auto-converted versions in the output.

## Directory structure

Each path below corresponds to where the file will land in `cawl_output/`.

```
overwrites/
├── cawlcore/
│   └── resources/localization/
│       └── cawlcore-locales_en.properties  (+ _de, _es, _fr, _it, _nl)
│
├── cawlb2ccheckoutaddon/
│   ├── resources/localization/
│   │   └── cawlb2ccheckoutaddon-locales_en.properties  (+ all locales)
│   └── acceleratoraddon/web/webroot/WEB-INF/messages/
│       └── base_en.properties  (+ base.properties, all locales)
│
├── cawlb2bcheckoutaddon/
│   ├── resources/localization/
│   │   └── cawlb2bcheckoutaddon-locales_en.properties  (+ all locales)
│   └── acceleratoraddon/web/webroot/WEB-INF/messages/
│       └── base_en.properties  (+ base.properties, all locales)
│
├── cawlocc/
│   └── resources/localization/
│       └── cawlocc-locales_en.properties  (+ all locales)
│
├── cawlbackoffice/
│   ├── resources/localization/
│   │   └── cawlbackoffice-locales_en.properties  (+ all locales)
│   ├── resources/cawlbackoffice-backoffice-labels/
│   │   └── labels_en.properties  (+ labels.properties, all locales)
│   └── backoffice/resources/widgets/
│       ├── actions/order/cawlmanualpaymentcaptureaction/labels/
│       ├── actions/order/cawlmanualpaymentrefundaction/labels/
│       ├── actions/order/cawlmanualpaymentreverseauthaction/labels/
│       └── order/partialcapturewidget/labels/
│
├── cawlcustomersupportbackoffice/
│   ├── resources/localization/
│   │   └── cawlcustomersupportbackoffice-locales_en.properties  (+ all locales)
│   ├── resources/cawlcustomersupportbackoffice-backoffice-labels/
│   │   └── labels_en.properties  (+ labels.properties, all locales)
│   └── backoffice/resources/widgets/
│       └── cawlcustomersupportbackofficeWidget/labels/
│
└── cawlwebhook/
    └── web/webroot/WEB-INF/messages/
        └── messages.properties
```

## Supported locales

The original Worldline extensions support these locales:
`cs`, `de`, `en`, `es`, `es_CO`, `fr`, `hi`, `hu`, `id`, `it`, `ja`, `ko`,
`nl`, `pl`, `pt`, `ru`, `zh`, `zh_TW`

(Note: `cawlcore` only has `de`, `en`, `es`, `fr`, `it`, `nl`)

## SDK replacement

To replace the Worldline Java SDK with a CAWL-branded SDK, place the replacement
`.jar` file in `scripts/sdk/`. The script will auto-detect it and swap it into
`cawlcore/lib/`.

You can also set SDK Maven coordinates via environment variables for the POM update:

```bash
export CAWL_SDK_GROUP_ID='com.cawl-solutions'
export CAWL_SDK_ARTIFACT_ID='cawl-payments-sdk-java'
export CAWL_SDK_VERSION='1.0.0'
```

If no jar is placed in `scripts/sdk/` and Maven is available, the script will
attempt to download the artifact using these coordinates.

## Notes

- Files here must use the **post-conversion** names (e.g. `cawlcore-locales_en.properties`, not `worldlinedirectcore-locales_en.properties`).
- Property keys should also use the post-conversion names (e.g. `type.cawl...` not `type.worldlinedirect...`).
- You do not need to provide every locale — only files present here will overwrite the auto-converted versions. Missing locales will keep the auto-converted output.
- You can also place non-localisation files here if needed — any file in this tree will overwrite its counterpart in `cawl_output/`.
