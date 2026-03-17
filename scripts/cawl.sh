#!/bin/bash
set -euo pipefail

export LC_CTYPE=C LANG=C

# ==============================================================================
# CAWL Conversion Script for SAP Commerce Cloud (Hybris)
# ==============================================================================
# This script converts the Worldline Direct payment plugin codebase into the
# CAWL-branded variant. It renames packages, directories, files, and performs
# text replacements so there is ZERO mention of "Worldline" in the output.
#
# Usage:
#   ./cawl.sh [source_dir] [dest_dir]
#
# Defaults:
#   source_dir = ./hybris/bin/custom/worldline
#   dest_dir   = ./cawl_output
#
# SDK Replacement (optional - configure in cawl.conf alongside this script):
#   CAWL_SDK_GROUP_ID='com.cawl-solutions'
#   CAWL_SDK_ARTIFACT_ID='cawl-payments-sdk-java'
#   CAWL_SDK_VERSION='1.0.0'
#   CAWL_SDK_JAR='/path/to/jar'   # optional: uses Maven download if omitted
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

SRC_DIR="${1:-./hybris/bin/custom/worldline}"
DEST_DIR="${2:-./cawl_output}"

# ------------------------------------------------------------------------------
# Load configuration from cawl.conf
# ------------------------------------------------------------------------------
CAWL_CONF="${SCRIPT_DIR}/cawl.conf"
CAWL_SDK_GROUP_ID=
CAWL_SDK_ARTIFACT_ID=
CAWL_SDK_VERSION=
CAWL_SDK_JAR=

if [[ -f "$CAWL_CONF" ]]; then
    echo "Loading config from $CAWL_CONF"
    # shellcheck source=cawl.conf
    source "$CAWL_CONF"
else
    echo "WARNING: Config file not found at $CAWL_CONF - using defaults."
fi

# Auto-detect SDK jar from scripts/sdk/ directory if CAWL_SDK_JAR is not set
if [[ -z "$CAWL_SDK_JAR" ]]; then
    SDK_DIR="$SCRIPT_DIR/sdk"
    if [[ -d "$SDK_DIR" ]]; then
        AUTO_JAR=$(find "$SDK_DIR" -maxdepth 1 -name "*.jar" -type f | head -1)
        if [[ -n "$AUTO_JAR" ]]; then
            CAWL_SDK_JAR="$AUTO_JAR"
            echo "Auto-detected SDK jar: $CAWL_SDK_JAR"
        fi
    fi
fi

if [[ ! -d "$SRC_DIR" ]]; then
    echo "ERROR: Source directory '$SRC_DIR' does not exist."
    exit 1
fi

# ------------------------------------------------------------------------------
# 1. Prepare target directory
# ------------------------------------------------------------------------------
echo "=== Step 1: Preparing output directory ==="
rm -rf "$DEST_DIR"
rsync -a --exclude='node_modules' --exclude='.git' --exclude='documentation' "$SRC_DIR"/ "$DEST_DIR"/

cd "$DEST_DIR" || { echo "Failed to enter $DEST_DIR"; exit 1; }

# ------------------------------------------------------------------------------
# 2. Define text replacements (ORDER MATTERS - most specific first)
# ------------------------------------------------------------------------------
# IMPORTANT: Longer/more-specific patterns MUST come before shorter ones so that
# sed processes them first and avoids partial matches.
#
# Pattern ordering rationale:
#   1. "worldlinedirect" before "worldline" (extension names)
#   2. "Worldlinedirect" before "Worldline" (Constants class prefixes)
#   3. "WORLDLINE_DIRECT" / "WORLDLINE" (uppercase constants & enum values)
#   4. "worldline" / "Worldline" (everything else)
#   5. Brand-specific strings (emails, labels)
# ------------------------------------------------------------------------------

declare -a PATTERNS=(
    # --- Extension / compound names (most specific first) ---
    "worldlinedirect"
    "Worldlinedirect"
    "WORLDLINE_DIRECT"

    # --- All-caps (enum values, constants like WORLDLINE_WAITING_AUTH) ---
    "WORLDLINE"

    # --- CamelCase variant "WorldLine" (capital L) ---
    "WorldLine"
    "worldLine"

    # --- Generic lowercase & PascalCase ---
    "worldline"
    "Worldline"

    # --- Email addresses (must come after generic patterns to override) ---
    # These are applied in the sed script AFTER the generic patterns, so the
    # generic pass will have already changed them. We fix them here.
    "isvpartners@cawl\\.com"
    "support\\.ecom@cawl\\.com"
)

declare -a REPLACEMENTS=(
    # --- Extension / compound names ---
    "cawl"
    "Cawl"
    "CAWL"

    # --- All-caps ---
    "CAWL"

    # --- CamelCase variant "WorldLine" (capital L) ---
    "CAWL"
    "cawl"

    # --- Generic lowercase & PascalCase ---
    "cawl"
    "CAWL"

    # --- Email addresses (restore to CAWL-branded emails) ---
    "isvpartners@cawl.com"
    "support.ecom@cawl.com"
)

# ------------------------------------------------------------------------------
# 3. Portable `sed -i` detection
# ------------------------------------------------------------------------------
if sed --version >/dev/null 2>&1; then
    SED_INPLACE=(-i)          # GNU sed
else
    SED_INPLACE=(-i '')       # BSD/macOS sed
fi

# ------------------------------------------------------------------------------
# 4. Rename directories and files, build sed script
# ------------------------------------------------------------------------------
sed_script=$(mktemp)
trap 'rm -f "$sed_script"' EXIT

# Build the combined sed script
for i in "${!PATTERNS[@]}"; do
    printf 's/%s/%s/g\n' "${PATTERNS[$i]}" "${REPLACEMENTS[$i]}" >> "$sed_script"
done

echo ""
echo "=== Step 2: Renaming directories ==="

# Rename directories - process deepest paths first to avoid breaking parent paths
# We iterate the patterns in order (most specific first)
for i in "${!PATTERNS[@]}"; do
    pattern="${PATTERNS[$i]}"
    replacement="${REPLACEMENTS[$i]}"

    # Skip regex-only patterns (those with backslashes) - they don't match dir names
    [[ "$pattern" == *\\* ]] && continue

    # Rename directories (deepest first via -depth)
    # Only rename the LAST path component to avoid breaking parent paths
    find . -depth -type d -name "*${pattern}*" 2>/dev/null | while IFS= read -r dir; do
        parent="$(dirname "$dir")"
        base="$(basename "$dir")"
        newbase="$(echo "$base" | sed -e "s/${pattern}/${replacement}/g")"
        if [[ "$base" != "$newbase" ]]; then
            echo "  DIR: $dir -> $parent/$newbase"
            mv "$dir" "$parent/$newbase"
        fi
    done
done

echo ""
echo "=== Step 3: Renaming files ==="

for i in "${!PATTERNS[@]}"; do
    pattern="${PATTERNS[$i]}"
    replacement="${REPLACEMENTS[$i]}"

    # Skip regex-only patterns
    [[ "$pattern" == *\\* ]] && continue

    find . -type f -name "*${pattern}*" 2>/dev/null | while IFS= read -r file; do
        parent="$(dirname "$file")"
        base="$(basename "$file")"
        newbase="$(echo "$base" | sed -e "s/${pattern}/${replacement}/g")"
        if [[ "$base" != "$newbase" ]]; then
            echo "  FILE: $file -> $parent/$newbase"
            mv "$file" "$parent/$newbase"
        fi
    done
done

# ------------------------------------------------------------------------------
# 5. Rename Java package directories
# ------------------------------------------------------------------------------
# The Java package com.worldline.direct lives under:
#   src/com/worldline/direct/
#   testsrc/com/worldline/direct/
#   web/src/com/worldline/direct/
#   backoffice/src/com/worldline/direct/
#
# After step 4, "worldline" dirs are already renamed to "cawl", so the
# directory structure should already be com/cawl/direct/. Verify and report.

echo ""
echo "=== Step 4: Verifying package directory structure ==="

remaining_worldline_dirs=$(find . -type d -name "*worldline*" 2>/dev/null || true)
if [[ -n "$remaining_worldline_dirs" ]]; then
    echo "  WARNING: Found remaining directories with 'worldline':"
    echo "$remaining_worldline_dirs" | while IFS= read -r d; do echo "    $d"; done
else
    echo "  OK: No directories containing 'worldline' remain."
fi

# ------------------------------------------------------------------------------
# 6. Apply text replacements to all files
# ------------------------------------------------------------------------------
echo ""
echo "=== Step 5: Applying text replacements ==="

# Apply to text files only (skip binary files like .jar, .class, images)
find . -type f \
    ! -name "*.jar" \
    ! -name "*.class" \
    ! -name "*.png" \
    ! -name "*.gif" \
    ! -name "*.jpg" \
    ! -name "*.jpeg" \
    ! -name "*.ico" \
    ! -name "*.woff" \
    ! -name "*.woff2" \
    ! -name "*.ttf" \
    ! -name "*.eot" \
    ! -name "*.svg" \
    ! -name "*.zip" \
    ! -name "*.gz" \
    ! -name "*.war" \
    -exec sed "${SED_INPLACE[@]}" -f "$sed_script" {} +

echo "  Text replacements applied."

# ------------------------------------------------------------------------------
# 7. Swap SDK dependency
# ------------------------------------------------------------------------------
echo ""
echo "=== Step 6: SDK dependency swap ==="

# Find the core extension's external-dependencies.xml and lib/ directory
# (after renaming, the extension is now "cawlcore")
CORE_EXT_DIR=$(find . -maxdepth 1 -type d -name "cawlcore" | head -1)

if [[ -z "$CORE_EXT_DIR" ]]; then
    echo "  WARNING: Could not find cawlcore extension directory. SDK swap skipped."
elif [[ -n "$CAWL_SDK_GROUP_ID" && -n "$CAWL_SDK_ARTIFACT_ID" && -n "$CAWL_SDK_VERSION" ]]; then
    echo "  Updating SDK coordinates:"
    echo "    groupId:    $CAWL_SDK_GROUP_ID"
    echo "    artifactId: $CAWL_SDK_ARTIFACT_ID"
    echo "    version:    $CAWL_SDK_VERSION"

    DEPS_XML="$CORE_EXT_DIR/external-dependencies.xml"

    if [[ -f "$DEPS_XML" ]]; then
        # The generic text replacement already changed "worldline" references in
        # the POM. Now we do a precise replacement of the SDK dependency
        # coordinates to the exact CAWL values, regardless of what the generic
        # pass produced.

        # Replace the SDK groupId (whatever it is now after generic replacement)
        sed "${SED_INPLACE[@]}" \
            -e "s|<groupId>com\.[^<]*-solutions</groupId>|<groupId>${CAWL_SDK_GROUP_ID}</groupId>|" \
            "$DEPS_XML"

        # Replace the SDK artifactId
        sed "${SED_INPLACE[@]}" \
            -e "s|<artifactId>onlinepayments-sdk-java</artifactId>|<artifactId>${CAWL_SDK_ARTIFACT_ID}</artifactId>|" \
            "$DEPS_XML"

        # Replace the version property value
        sed "${SED_INPLACE[@]}" \
            -e "s|<cawl\.direct\.version>[^<]*</cawl\.direct\.version>|<cawl.direct.version>${CAWL_SDK_VERSION}</cawl.direct.version>|" \
            "$DEPS_XML"

        echo "  Updated: $DEPS_XML"
    else
        echo "  WARNING: $DEPS_XML not found."
    fi

    # Swap the .jar file in lib/
    LIB_DIR="$CORE_EXT_DIR/lib"
    if [[ -d "$LIB_DIR" ]]; then
        # Remove the original Worldline SDK jar (name may vary after renaming)
        OLD_JAR=$(find "$LIB_DIR" -name "onlinepayments-sdk-java-*.jar" -o -name "*payments-sdk*.jar" | head -1)
        if [[ -n "$OLD_JAR" ]]; then
            echo "  Removing old SDK jar: $OLD_JAR"
            rm -f "$OLD_JAR"
        fi

        if [[ -n "$CAWL_SDK_JAR" && -f "$CAWL_SDK_JAR" ]]; then
            # Option B: Copy the provided local jar
            cp "$CAWL_SDK_JAR" "$LIB_DIR/"
            echo "  Copied new SDK jar: $CAWL_SDK_JAR -> $LIB_DIR/"
        elif command -v mvn >/dev/null 2>&1; then
            # Option A: Download via Maven
            echo "  Downloading SDK jar via Maven..."
            mvn dependency:copy \
                -Dartifact="${CAWL_SDK_GROUP_ID}:${CAWL_SDK_ARTIFACT_ID}:${CAWL_SDK_VERSION}" \
                -DoutputDirectory="$LIB_DIR" \
                -q 2>&1 && echo "  SDK jar downloaded to $LIB_DIR/" \
                || echo "  WARNING: Maven download failed. Place the jar manually in $LIB_DIR/"
        else
            echo "  WARNING: No local jar provided and Maven not available."
            echo "  Please manually place ${CAWL_SDK_ARTIFACT_ID}-${CAWL_SDK_VERSION}.jar in $LIB_DIR/"
        fi
    else
        echo "  WARNING: $LIB_DIR not found."
    fi
else
    echo "  SDK swap skipped (CAWL_SDK_GROUP_ID/ARTIFACT_ID/VERSION not set)."
    echo "  The external-dependencies.xml still references the original Worldline SDK."
    echo ""
    echo "  To configure, edit $CAWL_CONF:"
    echo "    CAWL_SDK_GROUP_ID='com.cawl-solutions'"
    echo "    CAWL_SDK_ARTIFACT_ID='cawl-payments-sdk-java'"
    echo "    CAWL_SDK_VERSION='1.0.0'"
    echo "    CAWL_SDK_JAR='/path/to/jar'   # optional, will use Maven if omitted"
fi

# ------------------------------------------------------------------------------
# 8. Post-processing verification
# ------------------------------------------------------------------------------
echo ""
echo "=== Step 7: Verification ==="

remaining_files=$(grep -rl "worldline\|Worldline\|WORLDLINE" . \
    --include="*.java" \
    --include="*.xml" \
    --include="*.properties" \
    --include="*.impex" \
    --include="*.jsp" \
    --include="*.tag" \
    --include="*.js" \
    --include="*.css" \
    --include="*.less" \
    --include="*.json" \
    2>/dev/null || true)

if [[ -n "$remaining_files" ]]; then
    echo "  WARNING: The following files still contain 'worldline' references:"
    echo "$remaining_files" | while IFS= read -r f; do
        echo "    $f"
        grep -n "worldline\|Worldline\|WORLDLINE" "$f" | head -5 | while IFS= read -r line; do
            echo "      $line"
        done
    done
    echo ""
    echo "  These may be false positives (e.g. external SDK references) or may"
    echo "  need manual attention or handling via the overwrites mechanism."
else
    echo "  SUCCESS: No remaining 'worldline' references found in source files."
fi

remaining_worldline_files=$(find . -name "*worldline*" -o -name "*Worldline*" 2>/dev/null || true)
if [[ -n "$remaining_worldline_files" ]]; then
    echo ""
    echo "  WARNING: Files/dirs still containing 'worldline' in their name:"
    echo "$remaining_worldline_files" | while IFS= read -r f; do echo "    $f"; done
else
    echo "  SUCCESS: No files or directories with 'worldline' in their name."
fi

# ------------------------------------------------------------------------------
# 9. Apply file overwrites (if overwrites directory exists)
# ------------------------------------------------------------------------------
cd ..

OVERWRITES_DIR="$(dirname "$0")/overwrites"
if [[ -d "$OVERWRITES_DIR" ]]; then
    echo ""
    echo "=== Step 8: Applying file overwrites ==="
    rsync -aI "$OVERWRITES_DIR"/ "$DEST_DIR"/
    echo "  Overwrites applied from $OVERWRITES_DIR"
else
    echo ""
    echo "=== Step 8: No overwrites directory found at $OVERWRITES_DIR - skipping ==="
fi

# ------------------------------------------------------------------------------
# 10. Package artifacts (optional)
# ------------------------------------------------------------------------------
echo ""
echo "=== Step 9: Packaging ==="
ARCHIVE_NAME="cawl_hybris_plugin.zip"
zip -qr "$ARCHIVE_NAME" "$DEST_DIR"
ls -lh "$ARCHIVE_NAME"

echo ""
echo "Done! Output in: $DEST_DIR"
echo "Archive: $ARCHIVE_NAME"
