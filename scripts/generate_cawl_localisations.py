#!/usr/bin/env python3
"""
generate_cawl_localisations.py

Regenerates the CAWL-branded localisation overwrite files under
scripts/overwrites/{cawlcore,cawlbackoffice}/... from:

  1. The CURRENT Worldline GoPay source localisation files (which define the
     key set / structure, and are the "no translation available" fallback).
  2. The six Worldline-supplied translation .txt files (which use the OLD
     "Worldline Direct" key naming and provide better/newer translations).

It performs the *same* text conversion that scripts/cawl.sh performs on
properties files (the ordered PATTERNS/REPLACEMENTS substitutions, followed
by the cawl[A-Z] -> CAWL[A-Z] acronym-casing fix), so the generated overwrite
files are byte-for-byte what a human would have produced by hand-translating
the post-conversion files.

Usage:
    python3 generate_cawl_localisations.py [--check]

--check: only print the report, do not write any files.

stdlib only. Paths are resolved relative to this script's location, so it
can be run from any working directory.
"""
from __future__ import annotations

import argparse
import re
from collections import OrderedDict
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
ROOT = SCRIPT_DIR.parent

CORE_LOC_DIR = ROOT / "hybris/bin/custom/worldline/worldlinegopaycore/resources/localization"
BACKOFFICE_LABELS_DIR = ROOT / "hybris/bin/custom/worldline/worldlinegopaybackoffice/resources/worldlinegopaybackoffice-backoffice-labels"

OVERWRITES_CORE_DIR = SCRIPT_DIR / "overwrites/cawlcore/resources/localization"
OVERWRITES_BACKOFFICE_DIR = SCRIPT_DIR / "overwrites/cawlbackoffice/resources/cawlbackoffice-backoffice-labels"

LOCALES = ["en", "de", "es", "fr", "it", "nl"]

TXT_FILES = {
    "en": SCRIPT_DIR / "Worldline-Localisations for CAWL.txt",
    "de": SCRIPT_DIR / "Worldline-Localisations for CAWL_de_DE.txt",
    "es": SCRIPT_DIR / "Worldline-Localisations for CAWL_es_ES.txt",
    "fr": SCRIPT_DIR / "Worldline-Localisations for CAWL_fr_FR.txt",
    "it": SCRIPT_DIR / "Worldline-Localisations for CAWL_it_IT.txt",
    "nl": SCRIPT_DIR / "Worldline-Localisations for CAWL_nl_NL.txt",
}

BACKOFFICE_SECTION_MARKER = "#worldlinedirectbackoffice-backoffice-labels/labels.properties"
CORE_SECTION_MARKER_PREFIX = "#worldlinedirectcore-locales_"

# ---------------------------------------------------------------------------
# CAWL text conversion - mirrors scripts/cawl.sh PATTERNS/REPLACEMENTS
# (Step 2) plus the Step 5b acronym-casing fix for non-Java files.
# ---------------------------------------------------------------------------
ORDERED_PAIRS = [
    ("Worldline GoPay", "CAWL"),
    ("worldlinegopay", "cawl"),
    ("Worldlinegopay", "Cawl"),
    ("WORLDLINE_DIRECT", "CAWL"),
    ("WORLDLINE", "CAWL"),
    ("WorldLine", "CAWL"),
    ("worldLine", "cawl"),
    ("worldline", "cawl"),
    ("Worldline", "CAWL"),
    ("GoPay", "Direct"),
    ("GOPAY", "DIRECT"),
    ("gopay", "direct"),
    ("isvpartners@cawl.com", "isvpartners@cawl.com"),
    ("support.ecom@cawl.com", "support.ecom@cawl.com"),
]
ACRONYM_FIX_RE = re.compile(r"cawl([A-Z])")


def cawl_convert(text: str) -> str:
    for pat, rep in ORDERED_PAIRS:
        text = text.replace(pat, rep)
    text = ACRONYM_FIX_RE.sub(r"CAWL\1", text)
    return text


# Old (pre "Worldline GoPay" rename) -> current key naming, used only to
# translate .txt file KEYS before matching them against current source keys.
TXT_KEY_MAPPING = [
    ("com.worldline.direct.", "com.worldline.gopay."),
]


def map_txt_key(key: str) -> str:
    for old, new in TXT_KEY_MAPPING:
        if key.startswith(old):
            return new + key[len(old):]
    return key


def repair_txt_keys(txt_kv: dict, valid_keys) -> dict:
    """Recover translation keys that the supplier's files mistyped.

    The Italian file translates the ``type.`` prefix to ``tipo.``, the Dutch
    file lower-cases type names (``type.cart.`` for ``type.Cart.``) and the
    German/Spanish backoffice sections misspell ``customersupportbackoffice``.
    A key that does not match any current source key is retried with those
    fixes and a case-insensitive lookup; genuinely unknown keys are kept as-is
    so the unmatched-key report still lists them.
    """
    lower_index = {k.lower(): k for k in valid_keys}
    repaired = {}
    for key, value in txt_kv.items():
        if key in valid_keys:
            repaired[key] = value
            continue
        candidate = key
        if candidate.startswith("tipo."):
            candidate = "type." + candidate[len("tipo."):]
        candidate = candidate.replace("customerupportbackoffice", "customersupportbackoffice")
        match = lower_index.get(candidate.lower())
        repaired[match if match else key] = value
    return repaired


# ---------------------------------------------------------------------------
# .properties parsing that preserves comments / blank lines / order.
# ---------------------------------------------------------------------------
class Item:
    __slots__ = ("kind", "line", "key", "value")

    def __init__(self, kind, line, key=None, value=None):
        self.kind = kind  # 'raw' (comment/blank) or 'kv'
        self.line = line
        self.key = key
        self.value = value


def parse_properties(text: str):
    """Return (items, dict_of_key_to_value, duplicate_keys, blank_values)."""
    items = []
    kv = OrderedDict()
    duplicates = []
    blanks = []
    for raw_line in text.split("\n"):
        stripped = raw_line.strip()
        if stripped == "" or stripped.startswith("#") or stripped.startswith("!"):
            items.append(Item("raw", raw_line))
            continue
        if "=" not in raw_line:
            # Not a recognised key=value line (e.g. stray text) - preserve as raw.
            items.append(Item("raw", raw_line))
            continue
        idx = raw_line.index("=")
        key = raw_line[:idx]
        value = raw_line[idx + 1:]
        key_stripped = key.strip()
        if key_stripped in kv:
            duplicates.append(key_stripped)
        kv[key_stripped] = value
        if value.strip() == "":
            blanks.append(key_stripped)
        items.append(Item("kv", raw_line, key_stripped, value))
    return items, kv, duplicates, blanks


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


# ---------------------------------------------------------------------------
# .txt translation file parsing
# ---------------------------------------------------------------------------
def parse_txt_file(path: Path):
    """
    Returns:
        backoffice_kv: OrderedDict[current-naming key -> value]
        core_kv: OrderedDict[current-naming key -> value]
        impex_lines: list[str] (the UPDATE WorldlineMealvouchersProductType block, raw)
        anomalies: list[str] (human-readable notes: dup keys, blank values, stray lines)
        key_map_log: list[(old_key, new_key)] for keys that were remapped
    """
    text = read_text(path)
    lines = text.split("\n")

    backoffice_kv = OrderedDict()
    core_kv = OrderedDict()
    impex_lines = []
    anomalies = []
    key_map_log = []

    section = None  # None, 'backoffice', 'core', 'impex'
    seen_backoffice = OrderedDict()
    seen_core = OrderedDict()

    for lineno, raw_line in enumerate(lines, start=1):
        stripped = raw_line.strip()

        if stripped == BACKOFFICE_SECTION_MARKER:
            section = "backoffice"
            continue
        if stripped.startswith(CORE_SECTION_MARKER_PREFIX):
            section = "core"
            continue
        # The mealvoucher ImpEx table header always contains "[lang" (e.g.
        # "name[lang = en]" / "nome[lang = it]"). One supplied file (IT) has
        # a mistranslated "UPDATE WorldlineMealvouchersProductType" prefix
        # ("AGGIORNAMENTO Buoni pasto Worldline Tipo prodotto"), so detect the
        # block by the "[lang" marker rather than relying on that prefix.
        if stripped.startswith("UPDATE WorldlineMealvouchersProductType") or "[lang" in stripped.lower():
            section = "impex"
            impex_lines.append(raw_line)
            continue

        if section == "impex":
            if stripped == "":
                # Trailing blank lines after the impex block: stop collecting,
                # but don't change section (nothing meaningful follows).
                continue
            impex_lines.append(raw_line)
            continue

        if stripped == "" or stripped.startswith("#") or stripped.startswith("!"):
            continue  # blank / comment line within backoffice or core section

        if "=" not in raw_line:
            anomalies.append(f"{path.name}:{lineno}: stray non-key=value line ignored: {raw_line!r}")
            continue

        idx = raw_line.index("=")
        raw_key = raw_line[:idx].strip()
        value = raw_line[idx + 1:]
        mapped_key = map_txt_key(raw_key)
        if mapped_key != raw_key:
            key_map_log.append((raw_key, mapped_key))

        if value.strip() == "":
            anomalies.append(f"{path.name}:{lineno}: blank translation value for key {raw_key!r}")

        if section == "backoffice":
            target, seen = backoffice_kv, seen_backoffice
        elif section == "core":
            target, seen = core_kv, seen_core
        else:
            anomalies.append(f"{path.name}:{lineno}: key=value line found before any section marker: {raw_key!r}")
            continue

        if mapped_key in seen:
            anomalies.append(
                f"{path.name}:{lineno}: duplicate key {mapped_key!r} in {section} section "
                f"(previous value {seen[mapped_key]!r} at earlier line, now {value!r})"
            )
        seen[mapped_key] = value
        target[mapped_key] = value

    return backoffice_kv, core_kv, impex_lines, anomalies, key_map_log


# ---------------------------------------------------------------------------
# Core (cawlcore-locales_*.properties) generation
# ---------------------------------------------------------------------------
def generate_core():
    report_lines = ["", "=" * 78, "CORE: cawlcore-locales_*.properties", "=" * 78]

    src_en_text = read_text(CORE_LOC_DIR / "worldlinegopaycore-locales_en.properties")
    en_items, en_kv, en_dups, en_blanks = parse_properties(src_en_text)
    if en_dups:
        report_lines.append(f"  ANOMALY: duplicate keys in source en file: {en_dups}")

    src_locale_kv = {}
    for loc in LOCALES:
        if loc == "en":
            continue
        p = CORE_LOC_DIR / f"worldlinegopaycore-locales_{loc}.properties"
        _, kv, dups, blanks = parse_properties(read_text(p))
        src_locale_kv[loc] = kv
        if dups:
            report_lines.append(f"  ANOMALY: duplicate keys in source {loc} file: {dups}")

    txt_core_kv = {}
    txt_anomalies_all = []
    txt_key_maps_all = []
    for loc in LOCALES:
        boff, core, impex, anomalies, key_maps = parse_txt_file(TXT_FILES[loc])
        txt_core_kv[loc] = repair_txt_keys(core, en_kv.keys())
        txt_anomalies_all.extend(anomalies)
        txt_key_maps_all.extend((loc, o, n) for o, n in key_maps)

    all_source_keys = [it.key for it in en_items if it.kind == "kv"]

    outputs = {loc: [] for loc in LOCALES}
    counts = {loc: {"a_txt": 0, "b_locale": 0, "c_english": 0} for loc in LOCALES}
    matched_txt_keys = {loc: set() for loc in LOCALES}

    for it in en_items:
        if it.kind == "raw":
            converted = cawl_convert(it.line)
            for loc in LOCALES:
                outputs[loc].append(converted)
            continue

        key = it.key
        en_value = en_kv[key]

        for loc in LOCALES:
            if loc == "en":
                if key in txt_core_kv["en"]:
                    value = txt_core_kv["en"][key]
                    counts[loc]["a_txt"] += 1
                    matched_txt_keys[loc].add(key)
                else:
                    value = en_value
                    counts[loc]["c_english"] += 1
            else:
                if key in txt_core_kv[loc]:
                    value = txt_core_kv[loc][key]
                    counts[loc]["a_txt"] += 1
                    matched_txt_keys[loc].add(key)
                elif key in src_locale_kv[loc]:
                    value = src_locale_kv[loc][key]
                    counts[loc]["b_locale"] += 1
                else:
                    value = en_value
                    counts[loc]["c_english"] += 1

            raw_line = f"{key}={value}"
            outputs[loc].append(cawl_convert(raw_line))

    # Verify the en output's key set matches cawl.sh's own conversion of the
    # source en file exactly (sanity check requested by the task).
    sed_converted_en_keys = set()
    for it in en_items:
        if it.kind == "kv":
            sed_converted_en_keys.add(cawl_convert(it.key))
    generated_en_keys = set()
    for line in outputs["en"]:
        if line.strip() == "" or line.lstrip().startswith("#"):
            continue
        if "=" in line:
            generated_en_keys.add(line.split("=", 1)[0])
    if sed_converted_en_keys != generated_en_keys:
        missing = sed_converted_en_keys - generated_en_keys
        extra = generated_en_keys - sed_converted_en_keys
        report_lines.append("  VERIFY FAILED: generated en key set != cawl.sh-converted source key set")
        if missing:
            report_lines.append(f"    missing from generated: {sorted(missing)}")
        if extra:
            report_lines.append(f"    extra in generated: {sorted(extra)}")
    else:
        report_lines.append(f"  VERIFY OK: generated en key set matches cawl.sh conversion of source en ({len(generated_en_keys)} keys).")

    report_lines.append("")
    report_lines.append("  Per-locale value-source counts (a=txt translation, b=source-locale fallback, c=source-English fallback):")
    for loc in LOCALES:
        c = counts[loc]
        report_lines.append(f"    {loc}: a={c['a_txt']:>3}  b={c['b_locale']:>3}  c={c['c_english']:>3}  (total keys={len(all_source_keys)})")

    report_lines.append("")
    report_lines.append("  .txt core-section keys that matched NO current source key (per locale):")
    any_unmatched = False
    for loc in LOCALES:
        unmatched = [k for k in txt_core_kv[loc].keys() if k not in en_kv]
        if unmatched:
            any_unmatched = True
            report_lines.append(f"    {loc}: {unmatched}")
    if not any_unmatched:
        report_lines.append("    (none)")

    if txt_anomalies_all:
        report_lines.append("")
        report_lines.append("  .txt file anomalies encountered while parsing (core + backoffice, see backoffice section for its own list too):")
        for a in txt_anomalies_all:
            report_lines.append(f"    {a}")

    return outputs, report_lines


# ---------------------------------------------------------------------------
# Backoffice (labels*.properties) generation
# ---------------------------------------------------------------------------
def generate_backoffice():
    report_lines = ["", "=" * 78, "BACKOFFICE: labels*.properties", "=" * 78]

    default_text = read_text(BACKOFFICE_LABELS_DIR / "labels.properties")
    default_items, default_kv, default_dups, _ = parse_properties(default_text)
    if default_dups:
        report_lines.append(f"  ANOMALY: duplicate keys in source default labels.properties: {default_dups}")

    src_locale_items = {}
    src_locale_kv = {}
    for loc in LOCALES:
        p = BACKOFFICE_LABELS_DIR / f"labels_{loc}.properties"
        items, kv, dups, _ = parse_properties(read_text(p))
        src_locale_items[loc] = items
        src_locale_kv[loc] = kv
        if dups:
            report_lines.append(f"  ANOMALY: duplicate keys in source labels_{loc}.properties: {dups}")

    txt_backoffice_kv = {}
    for loc in LOCALES:
        boff, core, impex, anomalies, key_maps = parse_txt_file(TXT_FILES[loc])
        txt_backoffice_kv[loc] = repair_txt_keys(boff, set(default_kv.keys()) | {k for kv in src_locale_kv.values() for k in kv})

    default_keys_order = [it.key for it in default_items if it.kind == "kv"]
    default_keys_set = set(default_keys_order)

    outputs = {loc: [] for loc in LOCALES}
    counts = {loc: {"a_txt": 0, "b_locale": 0, "c_english": 0} for loc in LOCALES}

    for loc in LOCALES:
        extra_keys = [k for k in src_locale_kv[loc].keys() if k not in default_keys_set]
        key_order = default_keys_order + extra_keys

        # Emit the default file's structure (comments/blanks + resolved
        # default keys), then append any locale-only extra keys at the end.
        out_lines = []
        for it in default_items:
            if it.kind == "raw":
                out_lines.append(cawl_convert(it.line))
                continue
            key = it.key
            value, source = _resolve_backoffice_value(key, loc, txt_backoffice_kv, src_locale_kv, default_kv)
            counts[loc][source] += 1
            out_lines.append(cawl_convert(f"{key}={value}"))

        if extra_keys:
            out_lines.append("")
            out_lines.append(cawl_convert(f"# Additional keys present only in source labels_{loc}.properties"))
            for key in extra_keys:
                value, source = _resolve_backoffice_value(key, loc, txt_backoffice_kv, src_locale_kv, default_kv)
                counts[loc][source] += 1
                out_lines.append(cawl_convert(f"{key}={value}"))

        outputs[loc] = out_lines

    # "labels.properties" (default, no-suffix) output: English content over
    # the union key set (same as labels_en.properties, per task rule 1).
    outputs["default"] = outputs["en"]

    report_lines.append("  Per-locale value-source counts (a=txt translation, b=source-locale fallback, c=source-English/default fallback):")
    for loc in LOCALES:
        c = counts[loc]
        total = c["a_txt"] + c["b_locale"] + c["c_english"]
        report_lines.append(f"    {loc}: a={c['a_txt']:>3}  b={c['b_locale']:>3}  c={c['c_english']:>3}  (total keys={total})")

    report_lines.append("")
    report_lines.append("  .txt backoffice-section keys that matched NO current source key (default ∪ labels_L) (per locale):")
    any_unmatched = False
    for loc in LOCALES:
        valid_keys = set(default_keys_order) | set(src_locale_kv[loc].keys())
        unmatched = [k for k in txt_backoffice_kv[loc].keys() if k not in valid_keys]
        if unmatched:
            any_unmatched = True
            report_lines.append(f"    {loc}: {unmatched}")
    if not any_unmatched:
        report_lines.append("    (none)")

    return outputs, report_lines


def _resolve_backoffice_value(key, loc, txt_backoffice_kv, src_locale_kv, default_kv):
    if key in txt_backoffice_kv[loc]:
        return txt_backoffice_kv[loc][key], "a_txt"
    if key in src_locale_kv[loc]:
        return src_locale_kv[loc][key], "b_locale"
    if key in src_locale_kv.get("en", {}):
        return src_locale_kv["en"][key], "c_english"
    return default_kv.get(key, ""), "c_english"


# ---------------------------------------------------------------------------
# Mealvoucher ImpEx block reporting (no changes made, per task instructions)
# ---------------------------------------------------------------------------
def report_impex(report_lines):
    report_lines.append("")
    report_lines.append("=" * 78)
    report_lines.append("MEALVOUCHER IMPEX BLOCK")
    report_lines.append("=" * 78)
    existing = ROOT / "hybris/bin/custom/worldline/worldlinegopaycore/resources/impex/essentialdata-enumeration-localisation.impex"
    if existing.exists():
        text = read_text(existing)
        if "WorldlineMealvouchersProductType" in text:
            report_lines.append(
                f"  {existing.relative_to(ROOT)} ALREADY defines "
                "'UPDATE WorldlineMealvouchersProductType' with name[lang=en/fr/de/it/es] "
                "columns and English translated values matching the .txt files' block "
                "(codes FoodAndDrink/HomeAndGarden/GiftAndFlowers vs the .txt's "
                "FOOD_AND_DRINK/HOME_AND_GARDEN/GIFT_AND_FLOWERS - different code casing, "
                "same translated names)."
            )
            report_lines.append(
                "  No impex change made (out of scope / instructed to report only). "
                "The mealvoucher block in each .txt file should NOT be written into the "
                "*.properties overwrite outputs - it has been excluded."
            )
        else:
            report_lines.append(f"  {existing.relative_to(ROOT)} exists but does NOT contain WorldlineMealvouchersProductType (unexpected).")
    else:
        report_lines.append(f"  {existing.relative_to(ROOT)} not found (unexpected).")

    for loc in LOCALES:
        _, _, impex_lines, _, _ = parse_txt_file(TXT_FILES[loc])
        if impex_lines:
            report_lines.append(f"  {TXT_FILES[loc].name}: extracted {len(impex_lines)} impex line(s), excluded from properties output.")


def write_outputs(core_outputs, backoffice_outputs, dry_run: bool):
    OVERWRITES_CORE_DIR.mkdir(parents=True, exist_ok=True)
    OVERWRITES_BACKOFFICE_DIR.mkdir(parents=True, exist_ok=True)

    written = []
    for loc in LOCALES:
        path = OVERWRITES_CORE_DIR / f"cawlcore-locales_{loc}.properties"
        content = "\n".join(core_outputs[loc]) + "\n"
        written.append((path, content))

    for loc in LOCALES:
        path = OVERWRITES_BACKOFFICE_DIR / f"labels_{loc}.properties"
        content = "\n".join(backoffice_outputs[loc]) + "\n"
        written.append((path, content))

    path = OVERWRITES_BACKOFFICE_DIR / "labels.properties"
    content = "\n".join(backoffice_outputs["default"]) + "\n"
    written.append((path, content))

    for path, content in written:
        if dry_run:
            print(f"  [dry-run] would write {path.relative_to(ROOT)} ({len(content)} bytes)")
        else:
            path.write_text(content, encoding="utf-8")
            print(f"  wrote {path.relative_to(ROOT)} ({len(content)} bytes)")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="Only print the report; do not write files.")
    args = parser.parse_args()

    core_outputs, core_report = generate_core()
    backoffice_outputs, backoffice_report = generate_backoffice()

    print("Writing overwrite files..." if not args.check else "Dry run (--check): not writing files.")
    write_outputs(core_outputs, backoffice_outputs, dry_run=args.check)

    for line in core_report:
        print(line)
    for line in backoffice_report:
        print(line)
    report_impex_lines = []
    report_impex(report_impex_lines)
    for line in report_impex_lines:
        print(line)


if __name__ == "__main__":
    main()
